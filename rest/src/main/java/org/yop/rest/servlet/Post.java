package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.ComposedSchema;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponses;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Delete;
import org.yop.orm.query.Select;
import org.yop.orm.query.Upsert;
import org.yop.orm.query.Where;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.openapi.OpenAPIUtil;
import org.yop.rest.openapi.YopSchemas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import static javax.servlet.http.HttpServletResponse.*;

/**
 * HTTP POST method implementation.
 * <br>
 * This is probably the trickiest part of Yop unrestful REST.
 * <br><br>
 * POST method is dedicated to custom queries :
 * {@link Select}, {@link Delete} and {@link Upsert} can be serialized to and from JSON.
 * <br>
 * So given the query parameter {@link #PARAM_TYPE}, we execute the query requested from the user.
 * <br>
 * <br>
 * <b>
 *     ⚠⚠⚠
 *     The target {@link Yopable} is both set in :
 *     <ul>
 *         <li>the request path context ({@link RestRequest#getRestResource()}</li>
 *         <li>the serialized query (e.g. {@link Upsert#getTarget()})</li>
 *     </ul>
 *     For security coherence, if they do not match when executing a custom query,
 *     an {@link IllegalArgumentException} is thrown.
 *     <br>
 *     Basically, custom queries MUST be executed on the appropriate REST resource.
 *     ⚠⚠⚠
 * </b>
 */
public class Post implements HttpMethod {

	static final HttpMethod INSTANCE = new Post();
	private static final String PARAM_TYPE = "queryType";

	private Post(){}

	/**
	 * Read the {@link #PARAM_TYPE} query parameter and execute the appropriate Yop query.
	 * @param restRequest the incoming request
	 * @param connection the JDBC (or other) underlying connection
	 * @return the result of the custom Yop query execution
	 * @throws IllegalArgumentException for an unknown query type.
	 */
	@Override
	public Object executeDefault(RestRequest restRequest, IConnection connection) {
		String queryType = restRequest.getParameterFirstValue(PARAM_TYPE);
		switch (StringUtils.lowerCase(queryType)) {
			case YopSchemas.SELECT : return doSelect(restRequest, connection);
			case YopSchemas.UPSERT : return doUpsert(restRequest, connection);
			case YopSchemas.DELETE : return doDelete(restRequest, connection);
			default: throw new IllegalArgumentException("Unknown query type [" + queryType + "]");
		}
	}

	@Override
	public Operation openAPIDefaultModel(Class<? extends Yopable> yopable) {
		String resource = OpenAPIUtil.getResourceName(yopable);
		Operation post = new Operation();
		post.setSummary("Execute custom YOP operation on [" + resource + "]");
		post.setResponses(new ApiResponses());
		post.setParameters(new ArrayList<>());

		Schema<String> typeParameterSchema = new Schema<>();
		typeParameterSchema.setType("string");
		typeParameterSchema.setEnum(Arrays.asList(YopSchemas.SELECT, YopSchemas.UPSERT, YopSchemas.DELETE));
		Parameter typeParameter = new Parameter()
			.name(PARAM_TYPE).in("query")
			.required(true)
			.schema(typeParameterSchema)
			.description("A custom Yop query for [" + resource + "] in JSON format");
		post.getParameters().add(typeParameter);

		Schema queries = new ComposedSchema().anyOf(Arrays.asList(
			new Schema().$ref(YopSchemas.SELECT),
			new Schema().$ref(YopSchemas.DELETE),
			new Schema().$ref(YopSchemas.UPSERT)
		));

		post.requestBody(new RequestBody().description("YOP custom query").content(new Content().addMediaType(
			ContentType.APPLICATION_JSON.getMimeType(),
			new MediaType().schema(queries).example(example(yopable).toJSON().toString())))
		);

		post.getResponses().addApiResponse(String.valueOf(SC_OK),                    HttpMethod.http200(yopable));
		post.getResponses().addApiResponse(String.valueOf(SC_BAD_REQUEST),           HttpMethod.http400());
		post.getResponses().addApiResponse(String.valueOf(SC_UNAUTHORIZED),          HttpMethod.http401());
		post.getResponses().addApiResponse(String.valueOf(SC_FORBIDDEN),             HttpMethod.http403());
		post.getResponses().addApiResponse(String.valueOf(SC_NOT_FOUND),             HttpMethod.http404());
		post.getResponses().addApiResponse(String.valueOf(SC_INTERNAL_SERVER_ERROR), HttpMethod.http500());
		return post;
	}

	/**
	 * Execute a {@link Select} query from the REST request.
	 * @param restRequest the incoming custom query REST request
	 * @param connection  the underlying connection to use to execute the request
	 * @return a Set of Yopable, serialized to JSON using {@link Select#toJSONQuery()}
	 * @throws IllegalArgumentException {@link RestRequest#getRestResource()} does not match {@link Select#getTarget()}
	 */
	private static Object doSelect(RestRequest restRequest, IConnection connection) {
		Select<Yopable> select = Select.fromJSON(
			restRequest.getContent(),
			restRequest.getRestResource().getClassLoader()
		);
		if (select.getTarget() != restRequest.getRestResource()) {
			throw new IllegalArgumentException(
				"The Select request for [" + select.getTarget().getName() + "] "
				+ "should be invoked on the appropriate REST resource "
				+ "instead of [" + restRequest.getRestResource().getName() + "]"
			);
		}
		Set<Yopable> results = select.execute(connection);
		return select.toJSONQuery().onto(results).toJSON();
	}

	/**
	 * Execute a {@link Delete} query from the REST request.
	 * @param restRequest the incoming custom query REST request
	 * @param connection  the underlying connection to use to execute the request
	 * @return an empty array list
	 * @throws IllegalArgumentException {@link RestRequest#getRestResource()} does not match {@link Select#getTarget()}
	 */
	private static Object doDelete(RestRequest restRequest, IConnection connection) {
		Delete<Yopable> delete = Delete.fromJSON(
			restRequest.getContent(),
			restRequest.getRestResource().getClassLoader()
		);
		if (delete.getTarget() != restRequest.getRestResource()) {
			throw new IllegalArgumentException(
				"The Delete request for [" + delete.getTarget().getName() + "] "
				+ "should be invoked on the appropriate REST resource "
				+ "instead of [" + restRequest.getRestResource().getName() + "]"
			);
		}
		delete.executeQueries(connection);
		return new ArrayList<>(0);
	}

	/**
	 * Execute a {@link Upsert} query from the REST request.
	 * @param restRequest the incoming custom query REST request
	 * @param connection  the underlying connection to use to execute the request
	 * @return an empty array list
	 * @throws IllegalArgumentException {@link RestRequest#getRestResource()} does not match {@link Select#getTarget()}
	 */
	private static Object doUpsert(RestRequest restRequest, IConnection connection) {
		Upsert<Yopable> upsert = Upsert.fromJSON(
			restRequest.getContent(),
			restRequest.getRestResource().getClassLoader()
		);
		if (upsert.getTarget() != restRequest.getRestResource()) {
			throw new IllegalArgumentException(
				"The Upsert request for [" + upsert.getTarget().getName() + "] "
				+ "should be invoked on the appropriate REST resource "
				+ "instead of [" + restRequest.getRestResource().getName() + "]"
			);
		}
		upsert.execute(connection);
		return new ArrayList<>(0);
	}

	/**
	 * Generate a dummy custom query example : a Select, with joinAll and an IdIn restriction.
	 * <br>
	 * This example is intended for OpenAPI documentation.
	 * @param yopable the target yopable
	 * @param <T> the target type
	 * @return a Select query
	 */
	private static <T extends Yopable> Select<T> example(Class<T> yopable) {
		Select<T> select = Select.from(yopable);
		select.where(Where.id(1L, 2L, 3L));
		select.joinAll();
		return select;
	}
}
