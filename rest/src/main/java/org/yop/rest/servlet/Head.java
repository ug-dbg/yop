package org.yop.rest.servlet;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
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
		resp.setContentType(request.getAccept().getMimeType());
		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resp.setContentLength(content.getBytes(StandardCharsets.UTF_8).length);
	}
}
