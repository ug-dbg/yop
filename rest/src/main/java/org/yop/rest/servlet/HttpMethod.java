package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.responses.ApiResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.json.JSON;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.TransformUtil;
import org.yop.rest.exception.YopNoResourceException;
import org.yop.rest.exception.YopResourceInvocationException;
import org.yop.rest.openapi.OpenAPIUtil;

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

	/** HTTP 'joinIDs' parameter : add extra properties (#ids suffix) for the non transient related objects. */
	String PARAM_JOIN_IDS = "joinIDs";

	/** HTTP 'checkNaturalID' parameter : when inserting, check if the Natural Key already exists. */
	String PARAM_CHECK_NK = "checkNaturalID";

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
	 * Create a {@link #PARAM_JOIN_IDS}' OpenAPI parameter for a given resource.
	 * @param forResource the resource name (for {@link io.swagger.oas.models.parameters.Parameter#description}.
	 * @return the OpenAPI 'joinIDs' parameter
	 */
	static io.swagger.oas.models.parameters.Parameter joinIDsParameter(String forResource) {
		return new io.swagger.oas.models.parameters.Parameter()
			.name(PARAM_JOIN_IDS)
			.in("query")
			.required(false)
			.schema(new Schema().type("boolean"))
			.description("join all IDs from non transient relations to [" + forResource + "]");
	}

	/**
	 * Create a {@link #PARAM_JOIN_IDS}' OpenAPI parameter for a given resource.
	 * @param forResource the resource name (for {@link io.swagger.oas.models.parameters.Parameter#description}.
	 * @return the OpenAPI 'joinIDs' parameter
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
		responseItem.setContent(new io.swagger.oas.models.media.Content());
		responseItem.getContent().addMediaType(
			ContentType.APPLICATION_JSON.getMimeType(),
			new MediaType().schema(errorJSONSchema())
		);
		return responseItem;
	}

	/**
	 * Create an OpenAPI success response for the given Yopable resource.
	 * Content is set to JSON and schema is set to the Yopable resource schema reference.
	 * @param yopable the Yopable resource
	 * @return a new {@link ApiResponse} with a resource description and referenced Schema.
	 */
	static ApiResponse http200(Class yopable) {
		ApiResponse responseItem = new ApiResponse();
		responseItem.setDescription("A set of [" + OpenAPIUtil.getResourceName(yopable) + "]");
		responseItem.setContent(new io.swagger.oas.models.media.Content());
		responseItem.getContent().addMediaType(
			ContentType.APPLICATION_JSON.getMimeType(),
			new MediaType().schema(new ArraySchema().items(OpenAPIUtil.refSchema(yopable)))
		);
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
			throw new YopNoResourceException("No resource for path [" + restRequest.getPath() + "]");
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
	default Object execute(RestRequest restRequest, IConnection connection) {
		if (restRequest.isCustomResource()) {
			return this.executeCustom(restRequest, connection);
		}
		return this.executeDefault(restRequest, connection);
	}

	/**
	 * Execute the custom resource (i.e.custom method on the {@link Yopable}.
	 * <br>
	 * Uses {@link RestRequest#matches(Method)} to find the custom method to execute.
	 * @param restRequest the incoming rest request.
	 * @param connection the JDBC (or other) underlying connection
	 * @return the execution result
	 * @throws YopNoResourceException no custom resource found for the request
	 * @throws YopResourceInvocationException an error occurred executing the custom method
	 */
	default Object executeCustom(RestRequest restRequest, IConnection connection) {
		Optional<Method> candidate = restRequest.getCandidate();

		if (! candidate.isPresent()) {
			logger.warn("No sub-resource method for [{}]", restRequest);
			throw new YopNoResourceException("No sub-resource found for [" + restRequest.getPath() + "]");
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

			if (Modifier.isStatic(method.getModifiers())) {
				return method.invoke(null, parameters);
			}
			return method.invoke(restRequest.getRestResource(), parameters);
		} catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
			throw new YopResourceInvocationException(
				"Error invoking YOP resource [" + Objects.toString(candidate.get()) + "]",
				e
			);
		}
	}

	/**
	 * Serialize the execution result, be it a {@link Yopable} or a collection of {@link Yopable}.
	 * <br>
	 * For now, it can only serialize to {@link ContentType#APPLICATION_JSON}.
	 * <br>
	 * If the input object is neither a {@link Yopable} or a collection, naively use {@link Objects#toString(Object)}.
	 * @param what        the object(s) to serialize
	 * @param restRequest the incoming rest request.
	 * @return the execution result, serialized into a String
	 */
	@SuppressWarnings("unchecked")
	default String serialize(Object what, RestRequest restRequest) {
		if (! restRequest.accept(ContentType.APPLICATION_JSON)) {
			logger.warn("For now, we just serialize to JSON. Sorry about that!");
		}

		if (what instanceof Yopable || what instanceof Collection) {
			JSON<Yopable> json = JSON.from(restRequest.getRestResource());
			if (what instanceof Yopable) {
				json.onto((Yopable) what);
			} else {
				json.onto((Collection<Yopable>) what);
				if (restRequest.joinIDs()) {
					json.joinIDsAll();
				}
				if (restRequest.joinAll()) {
					json.joinAll();
				}
			}

			if (restRequest.joinIDs()) {
				json.joinIDsAll();
			}
			if (restRequest.joinAll()) {
				json.joinAll();
			}
			return json.toJSON();
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
	 * @return the execution result
	 */
	Object executeDefault(RestRequest restRequest, IConnection connection);

	/**
	 * Generate an OpenAPI operation model for the given resource.
	 * <br>
	 * This operation should contain the default Yop REST behavior.
	 * @param resource the resource (Yopable) class
	 * @return the default Operation model for the resource name
	 */
	Operation openAPIDefaultModel(Class<? extends Yopable> resource);
}
