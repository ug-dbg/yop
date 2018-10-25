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

public class Post implements HttpMethod {

	static final HttpMethod INSTANCE = new Post();
	private static final String PARAM_TYPE = "queryType";

	private Post(){}

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

	private static <T extends Yopable> Select<T> example(Class<T> yopable) {
		Select<T> select = Select.from(yopable);
		select.where(Where.id(1L, 2L, 3L));
		select.joinAll();
		return select;
	}
}
