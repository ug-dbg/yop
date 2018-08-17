package org.yop.rest.servlet;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.json.JSON;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.Reflection;
import org.yop.rest.annotations.ContentParam;
import org.yop.rest.annotations.PathParam;
import org.yop.rest.exception.YopResourceInvocationException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public interface HttpMethod {

	Logger logger = LoggerFactory.getLogger(HttpMethod.class);

	String CONTENT = "content";
	String PATH = "path";

	default boolean isInvalidResource(RestRequest restRequest) throws IOException {
		if (restRequest.getRestResource() == null) {
			restRequest.getResponse().sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"No Yop REST resource for [" + restRequest + "]"
			);
			return true;
		}
		return false;
	}

	default Object execute(RestRequest restRequest, IConnection connection) {
		if (restRequest.isCustomResource()) {
			return this.executeCustom(restRequest, connection);
		}
		return this.executeDefault(restRequest, connection);
	}

	default Object executeCustom(RestRequest restRequest, IConnection connection) {
		Optional<Method> candidate = Reflection
			.getMethods(restRequest.getRestResource())
			.stream()
			.filter(restRequest::matches)
			.findFirst();

		if (! candidate.isPresent()) {
			logger.warn("No sub-resource method for [{}]", restRequest);
			throw new YopResourceInvocationException("No sub-resource found !");
		}

		try {
			Method method = candidate.get();
			method.setAccessible(true);
			Object[] parameters = new Object[method.getParameterCount()];
			for (int i = 0; i < method.getParameterCount(); i++) {
				Parameter parameter = method.getParameters()[i];
				if (IConnection.class.isAssignableFrom(parameter.getType())) {
					parameters[i] = connection;
				} else if(Header[].class.isAssignableFrom(parameter.getType())) {
					parameters[i] = restRequest.getHeaders();
				} else if(NameValuePair[].class.isAssignableFrom(parameter.getType())) {
					parameters[i] = restRequest.getParameters();
				} else if (String.class.isAssignableFrom(parameter.getType())) {
					if (CONTENT.equals(parameter.getName()) || parameter.isAnnotationPresent(ContentParam.class)) {
						parameters[i] = restRequest.getContent();
					}
					if (PATH.equals(parameter.getName()) || parameter.isAnnotationPresent(PathParam.class)) {
						parameters[i] = restRequest.getPath();
					}
				}
			}

			return method.invoke(restRequest.getRestResource(), parameters);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new YopResourceInvocationException(
				"Error invoking YOP resource [" + Objects.toString(candidate.get()) + "]",
				e
			);
		}
	}

	@SuppressWarnings("unchecked")
	default String serialize(Object what, RestRequest restRequest) {
		if (! ContentType.APPLICATION_JSON.getMimeType().equals(restRequest.getAccept().getMimeType())) {
			throw new UnsupportedOperationException("For now, we just serialize to JSON. Sorry about that!");
		}

		if (what instanceof Yopable || what instanceof Collection) {
			JSON<Yopable> json = JSON.from(restRequest.getRestResource());
			if (what instanceof Yopable) {
				json.onto((Yopable) what);
			} else {
				json.onto((Collection<Yopable>) what);
				if (restRequest.joinIDs()) {
					json.joinIDsAll();
				}
				if (restRequest.joinAll()) {
					json.joinAll();
				}
			}

			if (restRequest.joinIDs()) {
				json.joinIDsAll();
			}
			if (restRequest.joinAll()) {
				json.joinAll();
			}
			return json.toJSON();
		}

		return Objects.toString(what);
	}

	default void write(String what, RestRequest request) {
		String content = Objects.toString(what);
		HttpServletResponse resp = request.getResponse();
		resp.setContentType(request.getAccept().getMimeType());
		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resp.setContentLength(content.getBytes(StandardCharsets.UTF_8).length);

		if (StringUtils.isNotBlank(content)) {
			try {
				resp.getWriter().write(content);
			} catch (IOException e) {
				throw new YopResourceInvocationException(
					"Error writing content [" + StringUtils.abbreviate(content, 20) + "] for request [" + request + "]",
					e
				);
			}
		}
	}

	Object executeDefault(RestRequest restRequest, IConnection connection);
}
