package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.Parameter;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import org.apache.http.entity.ContentType;
import org.yop.orm.evaluation.IdIn;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Select;
import org.yop.orm.sql.adapter.IConnection;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;

class Get implements HttpMethod {

	static final HttpMethod INSTANCE = new Get();

	Get(){}

	@Override
	public Object executeDefault(RestRequest restRequest, IConnection connection) {
		Select<Yopable> select = Select.from(restRequest.getRestResource());
		if (restRequest.joinAll() || restRequest.joinIDs()) {
			select.joinAll();
		}

		if (restRequest.getId() > 0) {
			select.where(new IdIn(Collections.singletonList(restRequest.getId())));
			return select.uniqueResult(connection);
		} else {
			return select.execute(connection);
		}
	}

	@Override
	public Operation openAPIDefaultModel(String resource) {
		Operation get = new Operation();
		get.setSummary("Get all [" + resource + "] or by ID");
		get.setResponses(new ApiResponses());
		get.setParameters(new ArrayList<>());
		get.getParameters().add(idParameter(resource));
		get.getParameters().add(joinAllParameter(resource));

		ApiResponse responseItem = new ApiResponse();
		responseItem.setDescription("A set of [" + resource + "]");
		responseItem.setContent(new Content());
		responseItem.getContent().addMediaType(ContentType.APPLICATION_JSON.getMimeType(), new MediaType());
		get.getResponses().addApiResponse(String.valueOf(HttpServletResponse.SC_OK), responseItem);
		return get;
	}

	private static Parameter idParameter(String forResource) {
		return new Parameter()
			.name("id")
			.in("path")
			.required(false)
			.schema(new Schema().type("long"))
			.description("[" + forResource + "] ID");
	}

	private static Parameter joinAllParameter(String forResource) {
		return new Parameter()
			.name("joinAll")
			.in("query")
			.required(false)
			.schema(new Schema().type("boolean"))
			.description("join all non transient relations to [" + forResource + "]");
	}
}
