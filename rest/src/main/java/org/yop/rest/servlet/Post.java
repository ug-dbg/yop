package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Delete;
import org.yop.orm.query.Select;
import org.yop.orm.query.Upsert;
import org.yop.orm.sql.adapter.IConnection;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class Post implements HttpMethod {

	static final HttpMethod INSTANCE = new Post();
	private static final String PARAM_TYPE = "queryType";

	private Post(){}

	@Override
	public Object executeDefault(RestRequest restRequest, IConnection connection) {
		String queryType = restRequest.getParameterFirstValue(PARAM_TYPE);
		switch (StringUtils.lowerCase(queryType)) {
			case "select" : return doSelect(restRequest, connection);
			case "upsert" : return doUpsert(restRequest, connection);
			case "delete" : return doDelete(restRequest, connection);
			default: throw new IllegalArgumentException("Unknown query type [" + queryType + "]");
		}
	}

	@Override
	public Operation openAPIDefaultModel(Class yopable) {
		String resource = yopable.getSimpleName();
		Operation post = new Operation();
		post.setSummary("Execute custom YOP operation on [" + resource + "]");
		post.setResponses(new ApiResponses());
		post.setParameters(new ArrayList<>());

		Schema<String> typeParameterSchema = new Schema<>();
		typeParameterSchema.setType("string");
		typeParameterSchema.setEnum(Arrays.asList("select", "upsert", "delete"));
		Parameter typeParameter = new Parameter()
			.name(PARAM_TYPE).in("query")
			.required(true)
			.schema(typeParameterSchema)
			.description("A custom Yop query for [" + resource + "] in JSON format");
		post.getParameters().add(typeParameter);
		post.requestBody(new RequestBody().description("YOP custom query").content(new Content().addMediaType(
			ContentType.APPLICATION_JSON.getMimeType(),
			new MediaType()))
		);

		ApiResponse responseItem = new ApiResponse();
		responseItem.setDescription("A set of [" + resource + "]");
		responseItem.setContent(new Content());
		responseItem.getContent().addMediaType(ContentType.APPLICATION_JSON.getMimeType(), new MediaType());
		post.getResponses().addApiResponse(String.valueOf(HttpServletResponse.SC_OK), responseItem);
		return post;
	}

	private static Object doSelect(RestRequest restRequest, IConnection connection) {
		Select<Yopable> select = Select.fromJSON(restRequest.getContent());
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
		Delete<Yopable> delete = Delete.fromJSON(restRequest.getContent());
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
		Upsert<Yopable> upsert = Upsert.fromJSON(restRequest.getContent());
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
}
