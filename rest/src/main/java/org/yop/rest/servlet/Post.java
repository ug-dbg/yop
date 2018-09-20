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

public class Post implements HttpMethod {

	static final HttpMethod INSTANCE = new Post();

	private Post(){}

	@Override
	public Object executeDefault(RestRequest restRequest, IConnection connection) {
		throw new UnsupportedOperationException("Not implemented yet !");
	}

	@Override
	public Operation openAPIDefaultModel(String resource) {
		Operation post = new Operation();
		post.setSummary("Execute custom YOP operation on [" + resource + "]");
		post.setResponses(new ApiResponses());
		post.setParameters(new ArrayList<>());
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
}
