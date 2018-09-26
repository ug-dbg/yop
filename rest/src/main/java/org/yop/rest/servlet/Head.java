package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.headers.Header;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import org.apache.http.entity.ContentType;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Head extends Get {

	static final HttpMethod INSTANCE = new Head();

	private Head() {
		super();
	}

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
		String resource = yopable.getSimpleName();
		Operation head = new Operation();
		head.setSummary("Do head operation on [" + resource + "]. Execute request. Set content-length. No response.");
		head.setResponses(new ApiResponses());
		head.setParameters(new ArrayList<>());

		ApiResponse responseItem = new ApiResponse();
		responseItem.setDescription("Empty response. See content-length for equivalent GET.");
		responseItem.headers(new HashMap<>()).getHeaders().put(
			"Content-Length",
			new Header().description("Equivalent GET request content-length")
		);
		head.getResponses().addApiResponse(String.valueOf(HttpServletResponse.SC_OK), responseItem);
		return head;
	}
}
