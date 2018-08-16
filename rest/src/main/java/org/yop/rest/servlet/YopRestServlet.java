package org.yop.rest.servlet;


import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.sql.adapter.jdbc.JDBCConnection;
import org.yop.orm.util.Reflection;
import org.yop.rest.annotations.Rest;
import org.yop.rest.exception.YopResourceInvocationException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

public class YopRestServlet extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(YopRestServlet.class);

	static final String PACKAGE_INIT_PARAM = "packages";
	static final String DATASOURCE_JNDI_INIT_PARAM = "datasource_jndi";

	private static final String CONTENT = "content";

	private final Map<String, Class<Yopable>> yopablePaths = new HashMap<>();
	private String dataSourceJNDIName;
	private DataSource dataSource;

	protected IConnection getConnection() {
		try {
			return new JDBCConnection(this.dataSource.getConnection());
		} catch (SQLException e) {
			throw new RuntimeException(
				"Could not get connection for dataSource. JNDI name was [" + this.dataSourceJNDIName + "]",
				e
			);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void init() throws ServletException {
		super.init();
		Set<Class<? extends Yopable>> subtypes = new Reflections("").getSubTypesOf(Yopable.class);
		String[] packages = this.getInitParameter(PACKAGE_INIT_PARAM).split(",");
		for (Class<? extends Yopable> subtype : subtypes) {
			if (StringUtils.startsWithAny(subtype.getPackage().getName(), packages)
			&& subtype.isAnnotationPresent(Rest.class)) {
				this.yopablePaths.put(subtype.getAnnotation(Rest.class).path(), (Class<Yopable>) subtype);
			}
		}

		this.dataSourceJNDIName = this.getInitParameter(DATASOURCE_JNDI_INIT_PARAM);
		if (StringUtils.isNotBlank(this.dataSourceJNDIName)) {
			try {
				this.dataSource = (DataSource) new InitialContext().lookup(this.dataSourceJNDIName);
			} catch (NamingException e) {
				throw new YopRuntimeException("No datasource for JNDI name [" + this.dataSourceJNDIName + "]", e);
			}
		} else {
			logger.warn("No JNDI datasource set.");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		logger.info("Finding REST resource for GET [{}] ", req.getRequestURI());
		RestRequest restRequest = new RestRequest(req);

		if (this.isInvalidResource(restRequest, resp)) {
			return;
		}

		Class<Yopable> target = this.yopablePaths.get(restRequest.restResource);

		if (StringUtils.isNotBlank(restRequest.subResource)) {
			this.invokeSubResource(restRequest, target, resp);
			return;
		}

		Get.doGet(restRequest, target, resp, this.getConnection());
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		logger.info("Finding REST resource for HEAD [{}] ", req.getRequestURI());
		RestRequest restRequest = new RestRequest(req);

		if (this.isInvalidResource(restRequest, resp)) {
			return;
		}

		Class<Yopable> target = this.yopablePaths.get(restRequest.restResource);

		if (StringUtils.isNotBlank(restRequest.subResource)) {
			this.invokeSubResource(restRequest, target, resp);
			return;
		}
		Head.doHead(restRequest, target, resp, this.getConnection());
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.info("Finding REST resource for POST [{}] ", req.getRequestURI());
		RestRequest restRequest = new RestRequest(req);

		if (this.isInvalidResource(restRequest, resp)) {
			return;
		}

		Class<Yopable> target = this.yopablePaths.get(restRequest.restResource);

		if (StringUtils.isNotBlank(restRequest.subResource)) {
			this.invokeSubResource(restRequest, target, resp);
			return;
		}
		//Post.doPost(restRequest, target, resp, this.getConnection());
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doPut(req, resp);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doDelete(req, resp);
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doOptions(req, resp);
	}

	@Override
	protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doTrace(req, resp);
	}

	private boolean isInvalidResource(RestRequest restRequest, HttpServletResponse resp) throws IOException {
		if (StringUtils.isBlank(restRequest.restResource)) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No Yop REST resource set in path !");
			return true;
		}
		if (! this.yopablePaths.containsKey(restRequest.restResource)) {
			resp.sendError(
				HttpServletResponse.SC_NOT_FOUND,
				"The yop REST resource [" + restRequest.restResource + "] was not found."
			);
			return true;
		}
		return false;
	}

	private void invokeSubResource(
		RestRequest restRequest,
		Class<Yopable> target,
		HttpServletResponse resp)
		throws IOException {

		Optional<Method> candidate = Reflection.getMethods(target).stream().filter(restRequest::matches).findFirst();
		if (! candidate.isPresent()) {
			logger.warn("No method for subresource [{}]", restRequest.subResource);
			resp.sendError(
				HttpServletResponse.SC_NOT_FOUND,
				"The yop REST sub resource [" + restRequest.subResource + "] was not found."
			);
			return;
		}

		try {
			Method method = candidate.get();
			method.setAccessible(true);
			Object[] parameters = new Object[method.getParameterCount()];
			for (int i =0; i < method.getParameterCount(); i++) {
				Parameter parameter = method.getParameters()[i];
				if (IConnection.class.isAssignableFrom(parameter.getType())) {
					parameters[i] = this.getConnection();
				} else if(Header[].class.isAssignableFrom(parameter.getType())) {
					parameters[i] = restRequest.headers;
				} else if(NameValuePair[].class.isAssignableFrom(parameter.getType())) {
					parameters[i] = restRequest.parameters;
				} else if (String.class.isAssignableFrom(parameter.getType())) {
					if (CONTENT.equals(parameter.getName())) {
						parameters[i] = restRequest.content;
					}
				}
			}

			String content = Objects.toString(method.invoke(target, parameters));
			resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
			resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
			resp.setContentLength(content.getBytes(StandardCharsets.UTF_8).length);

			if (StringUtils.isNotBlank(content) && ! "HEAD".equals(restRequest.method)) {
				resp.getWriter().write(content);
			}

			resp.setStatus(HttpServletResponse.SC_OK);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new YopResourceInvocationException(
				"Error invoking YOP resource [" + Objects.toString(candidate.get()) + "]",
				e
			);
		}
	}
}
