package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import org.apache.http.entity.ContentType;
import org.yop.orm.sql.adapter.IConnection;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

public class Delete implements HttpMethod {

	static final HttpMethod INSTANCE = new Delete();

	private Delete(){}

	@Override
	public Object executeDefault(RestRequest restRequest, IConnection connection) {
		throw new UnsupportedOperationException("Not implemented yet !");
	}

	@Override
	public Operation openAPIDefaultModel(String resource) {
		Operation delete = new Operation();
		delete.setSummary("Do delete operation on [" + resource + "]");
		delete.setResponses(new ApiResponses());
		delete.setParameters(new ArrayList<>());
		delete.requestBody(new RequestBody().content(new Content().addMediaType(
			ContentType.APPLICATION_JSON.getMimeType(),
			new MediaType()))
		);

		ApiResponse responseItem = new ApiResponse();
		responseItem.setDescription("A set of [" + resource + "] deleted objects");
		responseItem.setContent(new Content());
		responseItem.getContent().addMediaType(ContentType.APPLICATION_JSON.getMimeType(), new MediaType());
		delete.getResponses().addApiResponse(String.valueOf(HttpServletResponse.SC_OK), responseItem);
		return delete;
	}
}
