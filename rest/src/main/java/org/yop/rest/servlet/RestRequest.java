package org.yop.rest.servlet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.model.Yopable;
import org.yop.rest.annotations.Rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A rest request to a {@link Yopable} with a reference to the response.
 */
class RestRequest {

	private static final Logger logger = LoggerFactory.getLogger(RestRequest.class);

	private HttpServletResponse response;

	private Long id = 0L;

	private String requestPath;

	private Class<Yopable> restResource;
	private String subResource;
	private String method;
	private String accept;

	private String content;

	private Map<String, String[]> parameters = new HashMap<>();
	private List<Header> headers = new ArrayList<>();

	RestRequest(HttpServletRequest req, HttpServletResponse resp, Map<String, Class<Yopable>> yopablePaths) {
		this.method = req.getMethod();
		this.accept = req.getHeader("Accept");
		this.response = resp;

		this.requestPath  = req.getRequestURI();
		String servletPath  = req.getServletPath();
		String resourcePath = StringUtils.removeStart(requestPath, servletPath);

		try {
			this.content = IOUtils.toString(req.getInputStream());
		} catch (IOException e) {
			logger.debug("No content for request", e);
		}

		this.parameters.putAll(req.getParameterMap());


		for(Enumeration<String> headerNames = req.getHeaderNames(); headerNames.hasMoreElements();) {
			String header = headerNames.nextElement();
			this.headers.add(new BasicHeader(header, req.getHeader(header)));
		}

		Path path = Paths.get(resourcePath);
		if (path.getNameCount() > 0) {
			String restResourceName = path.getName(0).toString().trim();
			if (yopablePaths.containsKey(restResourceName)) {
				this.restResource = yopablePaths.get(restResourceName);
			}
		}
		if (path.getNameCount() >= 2) {
			String subPath = path.getName(1).toString();
			if (NumberUtils.isCreatable(subPath)) {
				this.id = Long.valueOf(subPath);
			} else {
				this.subResource = subPath;
			}
		}
	}

	String getMethod() {
		return this.method;
	}

	String getPath() {
		return this.requestPath;
	}

	ContentType getAccept() {
		return this.accept != null ? ContentType.create(this.accept) : ContentType.APPLICATION_JSON;
	}

	NameValuePair[] getParameters() {
		List<NameValuePair> out = new ArrayList<>();
		for (Map.Entry<String, String[]> parameter : this.parameters.entrySet()) {
			Arrays
				.stream(parameter.getValue())
				.forEach(v -> out.add(new BasicNameValuePair(parameter.getKey(), v)));
		}
		return out.toArray(new NameValuePair[0]);
	}

	Header[] getHeaders() {
		return this.headers.toArray(new Header[0]);
	}

	public Long getId() {
		return this.id;
	}

	Class<Yopable> getRestResource() {
		return this.restResource;
	}

	boolean isCustomResource() {
		return StringUtils.isNotBlank(this.subResource);
	}

	boolean joinAll() {
		return this.parameters.containsKey("joinAll");
	}

	boolean joinIDs() {
		return this.parameters.containsKey("joinIDs");
	}

	boolean checkNaturalID() {
		return this.parameters.containsKey("checkNaturalID");
	}

	String getContent() {
		return content;
	}

	HttpServletResponse getResponse() {
		return this.response;
	}

	boolean matches(Method method) {
		return method.isAnnotationPresent(Rest.class)
			&& Arrays.stream(method.getAnnotation(Rest.class).methods()).anyMatch(s -> this.method.equals(s))
			&& StringUtils.equals(method.getAnnotation(Rest.class).path(), this.subResource);
	}

	@Override
	public String toString() {
		return "RestRequest{" +
			"requestPath='" + this.requestPath + '\'' +
			", method='" + this.method + '\'' +
		'}';
	}
}
