package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.ioc.Singleton;
import org.yop.orm.query.serialize.Serialize;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.TransformUtil;
import org.yop.reflection.Reflection;
import org.yop.rest.annotations.JoinProfiles;
import org.yop.rest.exception.YopNoResourceException;
import org.yop.rest.exception.YopResourceInvocationException;
import org.yop.rest.openapi.OpenAPIUtil;
import org.yop.rest.serialize.Deserializers;
import org.yop.rest.serialize.Serializers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * A convenience interface for creating HTTP method implementations.
 * <br><br>
 * This is to be used in the YOP REST servlet : {@link YopRestServlet}.
 * <br>
 * Once {@link #executeDefault(RestRequest, IConnection)} is implemented, the behavior should be always the same :
 * see {@link YopRestServlet#doExecute(HttpServletRequest, HttpServletResponse, HttpMethod)}.
 * <br>
 * Non conventional behavior :
 * <ul>
 *  <li>Override default method when required (e.g. the 'HEAD' method → do not write content).</li>
 *  <li>
 *      Throw {@link org.yop.rest.exception} exceptions
 *      and handle them in {@link YopRestServlet#service(HttpServletRequest, HttpServletResponse)}
 *  </li>
 * </ul>
 */
public interface HttpMethod {

	Logger logger = LoggerFactory.getLogger(HttpMethod.class);

	/** HTTP 'joinAll' parameter : join all non transient relation on the resources. */
	String PARAM_JOIN_ALL = "joinAll";

	/** HTTP 'checkNaturalID' parameter : when inserting, check if the Natural Key already exists. */
	String PARAM_CHECK_NK = "checkNaturalID";

	/** HTTP 'joinProfile' parameter : a profile to join when executing a CRUD operation. */
	String PARAM_JOIN_PROFILE = "joinProfile";

	/** HTTP 'count' parameter : return the total number of results in the output headers. */
	String PARAM_COUNT = "count";

	/** HTTP 'offset' parameter : return results from the given offset. */
	String PARAM_OFFSET = "offset";

	/** HTTP 'limit' parameter : only return X results. */
	String PARAM_LIMIT = "limit";

	/** JSON 'partial' parameter (only update provided fields) */
	String PARAM_PARTIAL = "partial";

	/** JSON error message key */
	String ERROR = "error";

	/** JSON error Schema for OpenAPI */
	Schema ERROR_SCHEMA = new Schema().addProperties(ERROR, new Schema().type("string"));

	/**
	 * Return the method implementation for the given method name
	 * @param method the method name (GET, POST...)
	 * @return the HttpMethod implementation
	 * @throws UnsupportedOperationException if the method name did not match any existing instance
	 */
	static HttpMethod instance(String method) {
		switch (StringUtils.upperCase(method)) {
			case "GET"     : return Get.INSTANCE;
			case "POST"    : return Post.INSTANCE;
			case "PUT"     : return Put.INSTANCE;
			case "UPSERT"  : return Upsert.INSTANCE;
			case "DELETE"  : return Delete.INSTANCE;
			case "HEAD"    : return Head.INSTANCE;
		}
		throw new UnsupportedOperationException("HTTP method [" + method + "] is not supported ! Sorry about that.");
	}

	/**
	 * Create an OpenAPI ID path parameter for a given resource.
	 * @param forResource the resource name (for {@link io.swagger.oas.models.parameters.Parameter#description}.
	 * @return the OpenAPI 'id' path parameter
	 */
	static io.swagger.oas.models.parameters.Parameter idParameter(String forResource) {
		return new io.swagger.oas.models.parameters.Parameter()
			.name("id")
			.in("path")
			.required(true)
			.schema(new Schema().type("integer").minimum(new BigDecimal(1)))
			.description("[" + forResource + "] ID");
	}

	/**
	 * Create a {@link #PARAM_JOIN_ALL} OpenAPI parameter for a given resource.
	 * @param forResource the resource name (for {@link io.swagger.oas.models.parameters.Parameter#description}.
	 * @return the OpenAPI 'joinAll' parameter
	 */
	static io.swagger.oas.models.parameters.Parameter joinAllParameter(String forResource) {
		return new io.swagger.oas.models.parameters.Parameter()
			.name(PARAM_JOIN_ALL)
			.in("query")
			.required(false)
			.schema(new Schema().type("boolean"))
			.description("join all non transient relations to [" + forResource + "]");
	}

	/**
	 * Create a {@link #PARAM_CHECK_NK}' OpenAPI parameter for a given resource.
	 * @param forResource the resource name (for {@link io.swagger.oas.models.parameters.Parameter#description}.
	 * @return the OpenAPI 'checkNaturalId' parameter
	 */
	static io.swagger.oas.models.parameters.Parameter checkNaturalIDParameter(String forResource) {
		return new io.swagger.oas.models.parameters.Parameter()
			.name(PARAM_CHECK_NK)
			.in("query")
			.required(false)
			.schema(new Schema().type("boolean"))
			.description("Check natural ID when inserting [" + forResource + "] elements. Do update if possible.");
	}

	/**
	 * Create a {@link #PARAM_JOIN_PROFILE}' OpenAPI parameter for a given resource.
	 * <br>
	 * Available profiles for the resource will be read from any {@link JoinProfiles} annotation.
	 * @param yopable the target resource.
	 * @return the OpenAPI 'joinProfile' parameter
	 */
	static io.swagger.oas.models.parameters.Parameter joinProfilesParameter(Class<?> yopable) {
		JoinProfiles annotation = Reflection.getAnnotation(yopable, JoinProfiles.class);
		String[] profiles = new String[0];
		if (annotation != null) {
			profiles = annotation.names();
		}
		return new io.swagger.oas.models.parameters.Parameter()
			.name(PARAM_JOIN_PROFILE)
			.in("query")
			.required(false)
			.schema(OpenAPIUtil.forValues("A collection of join profiles", profiles))
			.description("A profile to join for the operation on [" + OpenAPIUtil.getResourceName(yopable) + "].");
	}

	/**
	 * Create a {@link #PARAM_COUNT}' OpenAPI parameter for a given resource.
	 * @param forResource the resource name (for {@link io.swagger.oas.models.parameters.Parameter#description}.
	 * @return the OpenAPI 'count' parameter
	 */
	static io.swagger.oas.models.parameters.Parameter countParameter(String forResource) {
		return new io.swagger.oas.models.parameters.Parameter()
			.name(PARAM_COUNT)
			.in("header")
			.required(false)
			.schema(new Schema().type("boolean"))
			.description("Set the total number of results in your query on [" + forResource + "].");
	}

	/**
	 * Create a {@link #PARAM_OFFSET}' OpenAPI parameter for a given resource.
	 * @param forResource the resource name (for {@link io.swagger.oas.models.parameters.Parameter#description}.
	 * @return the OpenAPI 'offset' parameter
	 */
	static io.swagger.oas.models.parameters.Parameter pagingOffsetParameter(String forResource) {
		return new io.swagger.oas.models.parameters.Parameter()
			.name(PARAM_OFFSET)
			.in("header")
			.required(false)
			.schema(new Schema().type("integer"))
			.description("Set the offset from which to read in your query on [" + forResource + "].");
	}

	/**
	 * Create a {@link #PARAM_LIMIT}' OpenAPI parameter for a given resource.
	 * @param forResource the resource name (for {@link io.swagger.oas.models.parameters.Parameter#description}.
	 * @return the OpenAPI 'limit' parameter
	 */
	static io.swagger.oas.models.parameters.Parameter pagingLimitParameter(String forResource) {
		return new io.swagger.oas.models.parameters.Parameter()
			.name(PARAM_LIMIT)
			.in("header")
			.required(false)
			.schema(new Schema().type("integer"))
			.description("Set the number of results to return in your query on [" + forResource + "].");
	}

	/**
	 * Create a {@link #PARAM_PARTIAL}' OpenAPI parameter for a given resource.
	 * @param forResource the resource name (for {@link io.swagger.oas.models.parameters.Parameter#description}.
	 * @return the OpenAPI 'partial' parameter
	 */
	static io.swagger.oas.models.parameters.Parameter partialParameter(String forResource) {
		return new io.swagger.oas.models.parameters.Parameter()
			.name(PARAM_PARTIAL)
			.in("header")
			.required(false)
			.schema(new Schema().type("boolean"))
			.description("Only update the provided fields of [" + forResource + "]. Only suitable for update operation.");
	}

	/**
	 * Create a JSON with the error message as cause.
	 * @param cause the error cause
	 * @return a new JSON object with the {@link #ERROR} property set to the {@link Throwable#getMessage()}.
	 */
	static JSONObject errorJSON(Throwable cause) {
		return new JSONObject().put(ERROR, cause.getMessage());
	}

	/**
	 * The error message OpenAPI schema. One string property : {@link #ERROR}.
	 * @return the JSON error schema.
	 */
	static Schema errorJSONSchema() {
		return ERROR_SCHEMA;
	}

	/**
	 * Create an OpenAPI error response description. Content is set to JSON and schema is {@link #errorJSONSchema()}.
	 * @param description the error description
	 * @return a new {@link ApiResponse} with the given description.
	 */
	static ApiResponse httpError(String description) {
		ApiResponse responseItem = new ApiResponse();
		responseItem.setDescription(description);
		responseItem.setContent(OpenAPIUtil.contentFor(errorJSONSchema(), Serializers.SUPPORTED));
		return responseItem;
	}

	/**
	 * Create an OpenAPI success response for the given Yopable resource.
	 * Content is set to JSON and schema is set to the Yopable resource schema reference.
	 * @param yopable the Yopable resource
	 * @return a new {@link ApiResponse} with a resource description and referenced Schema.
	 */
	static ApiResponse http200(Class<?> yopable) {
		ApiResponse responseItem = new ApiResponse();
		responseItem.setDescription("A set of [" + OpenAPIUtil.getResourceName(yopable) + "]");
		responseItem.setContent(OpenAPIUtil.contentFor(OpenAPIUtil.refArraySchema(yopable), Serializers.SUPPORTED));
		return responseItem;
	}

	/**
	 * HTTP 400 Bad Request OpenAPI response description
	 * @return a new {@link ApiResponse} description for HTTP 400
	 */
	static ApiResponse http400() {
		return httpError("Bad request");
	}

	/**
	 * HTTP 401 Not authenticated OpenAPI response description
	 * @return a new {@link ApiResponse} description for HTTP 401
	 */
	static ApiResponse http401() {
		return httpError("Unauthorized");
	}

	/**
	 * HTTP 403 Forbidden OpenAPI response description
	 * @return a new {@link ApiResponse} description for HTTP 403
	 */
	static ApiResponse http403() {
		return httpError("Forbidden");
	}

	/**
	 * HTTP 404 Not found OpenAPI response description
	 * @return a new {@link ApiResponse} description for HTTP 404
	 */
	static ApiResponse http404() {
		return httpError("Resource not found");
	}

	/**
	 * HTTP 500 Internal server error OpenAPI response description
	 * @return a new {@link ApiResponse} description for HTTP 500
	 */
	static ApiResponse http500() {
		return httpError("Internal server error");
	}

	/**
	 * Create an OpenAPI documentation {@link RequestBody} as an array of the target (Yopable).
	 * @param target the target class
	 * @return the OpenAPI request body
	 */
	static RequestBody requestBody(Class<?> target) {
		return new RequestBody().content(OpenAPIUtil.contentFor(
			OpenAPIUtil.refArraySchema(target),
			Deserializers.SUPPORTED
		));
	}

	/**
	 * Is the rest request associated to a valid resource ?
	 * <br>
	 * If so, please throw a Runtime exception with context.
	 * <br>
	 * Default implementation : check if {@link RestRequest#getRestResource()} is null.
	 * @param restRequest the incoming rest request
	 * @throws YopNoResourceException no REST resource associated to the request
	 */
	default void checkResource(RestRequest restRequest) {
		if (restRequest.getRestResource() == null) {
			throw new YopNoResourceException("No resource for path [" + restRequest.getRequestPath() + "]");
		}
	}

	/**
	 * Do execute the request.
	 * <ul>
	 *     <li>custom resource → {@link #executeCustom(RestRequest, IConnection)}</li>
	 *     <li>default → {@link #executeDefault(RestRequest, IConnection)}</li>
	 * </ul>
	 * @param restRequest the incoming rest request
	 * @param connection the JDBC (or other) underlying connection
	 * @return the execution result, be it from default or custom behavior
	 */
	default <T> RestResponse execute(RestRequest<T> restRequest, IConnection connection) {
		if (restRequest.isCustomResource()) {
			return this.executeCustom(restRequest, connection);
		}
		return this.executeDefault(restRequest, connection);
	}

	/**
	 * Execute the custom resource (i.e.custom method on the Yopable.
	 * <br>
	 * Uses {@link RestRequest#matches(Method)} to find the custom method to execute.
	 * @param restRequest the incoming rest request.
	 * @param connection the JDBC (or other) underlying connection
	 * @return the execution result
	 * @throws YopNoResourceException no custom resource found for the request
	 * @throws YopResourceInvocationException an error occurred executing the custom method
	 */
	default <T> RestResponse executeCustom(RestRequest<T> restRequest, IConnection connection) {
		Optional<Method> candidate = restRequest.getCandidate();

		if (! candidate.isPresent()) {
			logger.warn("No sub-resource method for [{}]", restRequest);
			throw new YopNoResourceException("No sub-resource found for [" + restRequest.getRequestPath() + "]");
		}

		try {
			Method method = candidate.get();
			method.setAccessible(true);
			Object[] parameters = new Object[method.getParameterCount()];
			for (int i = 0; i < method.getParameterCount(); i++) {
				Parameter parameter = method.getParameters()[i];
				if (IConnection.class.isAssignableFrom(parameter.getType())) {
					parameters[i] = connection;
				} else if(Header[].class.isAssignableFrom(parameter.getType())) {
					parameters[i] = restRequest.getHeaders();
				} else if(NameValuePair[].class.isAssignableFrom(parameter.getType())) {
					parameters[i] = restRequest.getParameters();
				} else {
					parameters[i] = TransformUtil.transform(
						AnnotationToParameter.get(restRequest, parameter),
						parameter.getType()
					);
				}
			}

			Object out;
			if (Modifier.isStatic(method.getModifiers())) {
				out = method.invoke(null, parameters);
			} else {
				out = method.invoke(Singleton.of(restRequest.getRestResource()).get(), parameters);
			}
			return RestResponse.forOutput(out);
		} catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
			throw new YopResourceInvocationException(
				"Error invoking YOP resource [" + candidate.get() + "]",
				e
			);
		}
	}

	/**
	 * Serialize the execution result, be it a Yopable or a collection of Yopable.
	 * <br>
	 * For now, it can only serialize to {@link ContentType#APPLICATION_JSON}.
	 * <br>
	 * If the input object is neither a Yopable or a collection, naively use {@link Objects#toString(Object)}.
	 * @param what        the object(s) to serialize
	 * @param restRequest the incoming rest request.
	 * @return the execution result, serialized into a String
	 */
	@SuppressWarnings("unchecked")
	default <T> String serialize(Object what, RestRequest<T> restRequest) {
		if (ORMUtil.isYopable(what) || what instanceof Collection) {
			String outputContentType = restRequest.accept(Serializers.SUPPORTED);
			Serialize serializer = Serializers.getFor(restRequest.getRestResource(), outputContentType);

			if (what instanceof Collection) {
				serializer.onto((Collection) what);
				if (restRequest.joinAll()) {
					serializer.joinAll();
				}
			} else {
				serializer.onto(what);
			}

			if (restRequest.joinAll()) {
				serializer.joinAll();
			}
			serializer.joinProfiles(restRequest.profiles().toArray(new String[0]));
			return serializer.execute();
		}

		return Objects.toString(what);
	}

	/**
	 * Write the serialized execution result into the {@link RestRequest#getResponse()} writer.
	 * <br>
	 * Content length is set using the length of the String to write.
	 * <br>
	 * Charset is forced to {@link StandardCharsets#UTF_8} for now.
	 * @param what    the serialized execution result
	 * @param request the incoming request
	 * @throws YopResourceInvocationException an I/O exception occurred writing into the response
	 */
	default void write(String what, RestRequest request) {
		String content = Objects.toString(what);
		HttpServletResponse resp = request.getResponse();
		resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resp.setContentLength(content.getBytes(StandardCharsets.UTF_8).length);

		if (StringUtils.isNotBlank(content)) {
			try {
				resp.getWriter().write(content);
			} catch (IOException e) {
				throw new YopResourceInvocationException(
					"Error writing content [" + StringUtils.abbreviate(content, 20) + "] for request [" + request + "]",
					e
				);
			}
		}
	}

	/**
	 * Execute the default behavior for the given request.
	 * @param restRequest the incoming request
	 * @param connection the JDBC (or other) underlying connection
	 * @return the execution result : a wrapper containing both the output and some extra output headers to set.
	 */
	<T> RestResponse executeDefault(RestRequest<T> restRequest, IConnection connection);

	/**
	 * Generate an OpenAPI operation model for the given resource.
	 * <br>
	 * This operation should contain the default Yop REST behavior.
	 * @param resource the resource (mostly : Yopable) class
	 * @return the default Operation model for the resource name
	 */
	Operation openAPIDefaultModel(Class<?> resource);
}
