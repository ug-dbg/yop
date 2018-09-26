package org.yop.rest.servlet;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
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
import org.yop.orm.util.Reflection;
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
	private String subResource = "";
	private String method;
	private String accept;

	private String content;

	private MultiValuedMap<String, String> parameters = new ArrayListValuedHashMap<>();
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

		req.getParameterMap().forEach((key, value) -> this.parameters.putAll(key, Arrays.asList(value)));

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

	boolean accept(ContentType contentType) {
		// laziest.implementation.ever
		return StringUtils.containsIgnoreCase(this.accept, contentType.getMimeType());
	}

	NameValuePair[] getParameters() {
		List<NameValuePair> out = new ArrayList<>();
		this.parameters.entries().forEach(entry -> out.add(new BasicNameValuePair(entry.getKey(), entry.getValue())));
		return out.toArray(new NameValuePair[0]);
	}

	String getParameterFirstValue(String name) {
		Collection<String> values = this.parameters.get(name);
		return values.isEmpty() ? null : values.iterator().next();
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
		return StringUtils.isNotBlank(this.subResource)
			|| Reflection.getMethods(this.restResource).stream().anyMatch(this::matches);
	}

	boolean joinAll() {
		return this.parameters.containsMapping(HttpMethod.PARAM_JOIN_ALL, "true")
			|| this.parameters.containsMapping(HttpMethod.PARAM_JOIN_ALL, null);
	}

	boolean joinIDs() {
		return this.parameters.containsMapping(HttpMethod.PARAM_JOIN_IDS, "true")
			|| this.parameters.containsMapping(HttpMethod.PARAM_JOIN_IDS, null);
	}

	boolean checkNaturalID() {
		return this.parameters.containsMapping(HttpMethod.PARAM_CHECK_NK, "true")
			|| this.parameters.containsMapping(HttpMethod.PARAM_CHECK_NK, null);
	}

	String getContent() {
		return StringUtils.isEmpty(this.content) ? "" : this.content;
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
