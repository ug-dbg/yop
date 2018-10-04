package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import org.apache.http.entity.ContentType;
import org.yop.orm.evaluation.IdIn;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.openapi.OpenAPIUtil;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;

public class Delete implements HttpMethod {

	static final HttpMethod INSTANCE = new Delete();

	private Delete(){}

	@Override
	public Object executeDefault(RestRequest restRequest, IConnection connection) {
		org.yop.orm.query.Delete<Yopable> delete = org.yop.orm.query.Delete.from(restRequest.getRestResource());
		if (restRequest.joinAll() || restRequest.joinIDs()) {
			delete.joinAll();
		}

		if (restRequest.getId() > 0) {
			delete.where(new IdIn(Collections.singletonList(restRequest.getId())));
		}
		delete.executeQueries(connection);
		return "[]";
	}

	@Override
	public Operation openAPIDefaultModel(Class yopable) {
		String resource = OpenAPIUtil.getResourceName(yopable);
		Operation delete = new Operation();
		delete.setSummary("Do delete operation on [" + resource + "]. If not ID provided, delete all entries !");
		delete.setResponses(new ApiResponses());
		delete.setParameters(new ArrayList<>());
		delete.getParameters().add(HttpMethod.joinAllParameter(resource));
		delete.getParameters().add(HttpMethod.joinIDsParameter(resource));

		ApiResponse responseItem = new ApiResponse();
		responseItem.setDescription("Empty json array. To return the deleted objects, please override me.");
		responseItem.setContent(new Content());
		responseItem.getContent().addMediaType(ContentType.APPLICATION_JSON.getMimeType(), new MediaType());
		delete.getResponses().addApiResponse(String.valueOf(HttpServletResponse.SC_OK), responseItem);
		return delete;
	}
}
