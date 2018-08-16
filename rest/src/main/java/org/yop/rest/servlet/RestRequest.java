package org.yop.rest.servlet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.rest.annotations.Rest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class RestRequest {

	private static final Logger logger = LoggerFactory.getLogger(RestRequest.class);



	Long id = 0L;

	boolean joinAll;
	boolean joinIDs;

	String restResource;
	String subResource;
	String method;

	String content;
	NameValuePair[] parameters;
	Header[] headers;

	RestRequest(HttpServletRequest req) {
		this.method = req.getMethod();

		String requestPath  = req.getRequestURI();
		String servletPath  = req.getServletPath();
		String resourcePath = StringUtils.removeStart(requestPath, servletPath);
		this.joinAll = req.getParameterMap().containsKey("joinAll");
		this.joinIDs = req.getParameterMap().containsKey("joinIDs");

		try {
			this.content = IOUtils.toString(req.getInputStream());
		} catch (IOException e) {
			logger.debug("No content for request", e);
		}

		List<NameValuePair> parameters = new ArrayList<>();
		for (Map.Entry<String, String[]> parameter : req.getParameterMap().entrySet()) {
			Arrays
				.stream(parameter.getValue())
				.forEach(v -> parameters.add(new BasicNameValuePair(parameter.getKey(), v)));
		}
		this.parameters = parameters.toArray(new NameValuePair[0]);

		List<Header> headers = new ArrayList<>();
		for(Enumeration<String> headerNames = req.getHeaderNames(); headerNames.hasMoreElements();) {
			String header = headerNames.nextElement();
			headers.add(new BasicHeader(header, req.getHeader(header)));
		}
		this.headers = headers.toArray(new Header[0]);

		Path path = Paths.get(resourcePath);
		if (path.getNameCount() > 0) {
			this.restResource = path.getName(0).toString().trim();
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

	boolean matches(Method method) {
		return method.isAnnotationPresent(Rest.class)
			&& Arrays.stream(method.getAnnotation(Rest.class).methods()).anyMatch(s -> this.method.equals(s))
			&& StringUtils.equals(method.getAnnotation(Rest.class).path(), this.subResource);
	}
}
