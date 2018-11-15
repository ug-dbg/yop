package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.headers.Header;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import org.apache.http.entity.ContentType;
import org.yop.rest.openapi.OpenAPIUtil;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static javax.servlet.http.HttpServletResponse.*;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

/**
 * HTTP HEAD method implementation.
 * <br>
 * It does a {@link Get} execution but does not serialize content.
 * <br>
 * See {@link #write(String, RestRequest)}, which simply sets {@link HttpServletResponse#setContentLength(int)}.
 */
public class Head extends Get {

	static final HttpMethod INSTANCE = new Head();

	private Head() {
		super();
	}

	/**
	 * Set the content length as the number of bytes of 'what'.
	 * <br>
	 * Set the Charset to UTF-8.
	 * <br>
	 * Set the content type to JSON.
	 * @param what    the serialized execution result
	 * @param request the incoming request
	 */
	@Override
	public void write(String what, RestRequest request) {
		String content = Objects.toString(what);
		HttpServletResponse resp = request.getResponse();
		resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resp.setContentLength(content.getBytes(StandardCharsets.UTF_8).length);
	}

	@Override
	public Operation openAPIDefaultModel(Class yopable) {
		String resource = OpenAPIUtil.getResourceName(yopable);
		Operation head = new Operation();
		head.setSummary("Do head operation on [" + resource + "]. Execute request. Set content-length. No response.");
		head.setResponses(new ApiResponses());
		head.setParameters(new ArrayList<>());
		head.getParameters().add(HttpMethod.countParameter(resource));
		head.getParameters().add(HttpMethod.pagingOffsetParameter(resource));
		head.getParameters().add(HttpMethod.pagingLimitParameter(resource));

		ApiResponse responseItem = new ApiResponse();
		responseItem.setDescription("Empty response. See content-length for equivalent GET.");
		responseItem.headers(new HashMap<>()).getHeaders().put(
			"Content-Length",
			new Header().description("Equivalent GET request content-length")
		);
		head.getResponses().addApiResponse(String.valueOf(SC_OK),                    responseItem);
		head.getResponses().addApiResponse(String.valueOf(SC_BAD_REQUEST),           HttpMethod.http400());
		head.getResponses().addApiResponse(String.valueOf(SC_UNAUTHORIZED),          HttpMethod.http401());
		head.getResponses().addApiResponse(String.valueOf(SC_FORBIDDEN),             HttpMethod.http403());
		head.getResponses().addApiResponse(String.valueOf(SC_NOT_FOUND),             HttpMethod.http404());
		head.getResponses().addApiResponse(String.valueOf(SC_INTERNAL_SERVER_ERROR), HttpMethod.http500());
		return head;
	}
}
