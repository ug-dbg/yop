package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import org.apache.http.entity.ContentType;
import org.yop.orm.evaluation.IdIn;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Select;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.openapi.OpenAPIUtil;

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
	public Operation openAPIDefaultModel(Class<? extends Yopable> yopable) {
		String resource = yopable.getSimpleName();
		Operation get = new Operation();
		get.setSummary("Get all [" + resource + "] or one single object by ID");
		get.setResponses(new ApiResponses());
		get.setParameters(new ArrayList<>());
		get.getParameters().add(HttpMethod.idParameter(resource));
		get.getParameters().add(HttpMethod.joinAllParameter(resource));
		get.getParameters().add(HttpMethod.joinIDsParameter(resource));

		ApiResponse responseItem = new ApiResponse();
		responseItem.setDescription("A set of [" + resource + "]");
		responseItem.setContent(new Content());
		responseItem.getContent().addMediaType(
			ContentType.APPLICATION_JSON.getMimeType(),
			new MediaType().schema(new ArraySchema().items(OpenAPIUtil.forResource(yopable)))
		);
		get.getResponses().addApiResponse(String.valueOf(HttpServletResponse.SC_OK), responseItem);
		return get;
	}
}
