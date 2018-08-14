package org.yop.rest.servlet;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.evaluation.IdIn;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Select;
import org.yop.orm.query.json.JSON;
import org.yop.rest.annotations.Rest;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.sql.adapter.jdbc.JDBCConnection;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

public class YopRestServlet extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(YopRestServlet.class);

	static final String PACKAGE_INIT_PARAM = "packages";
	static final String DATASOURCE_JNDI_INIT_PARAM = "datasource_jndi";

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

		if (StringUtils.isBlank(restRequest.restResource)) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No Yop REST resource set in path !");
			return;
		}
		if (! this.yopablePaths.containsKey(restRequest.restResource)) {
			resp.sendError(
				HttpServletResponse.SC_NOT_FOUND,
				"The yop REST resource [" + restRequest.restResource + "] was not found."
			);
			return;
		}

		Class<Yopable> target = this.yopablePaths.get(restRequest.restResource);
		Select<Yopable> select = Select.from(target);
		if (restRequest.joinAll || restRequest.joinIDs) {
			select.joinAll();
		}

		if (StringUtils.isNotBlank(restRequest.subResource)) {
			try {
				String out = this.invokeSubResource(restRequest, target);
				resp.setContentType("application/json");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write(out);
				resp.setStatus(HttpServletResponse.SC_OK);
				return;
			} catch (UnsupportedOperationException e) {
				resp.sendError(
					HttpServletResponse.SC_NOT_FOUND,
					"The yop REST sub resource [" + restRequest.subResource + "] was not found."
				);
				return;
			}
		}

		JSON<Yopable> json = JSON.from(target);
		if (restRequest.id > 0) {
			select.where(new IdIn(Collections.singletonList(restRequest.id)));
			Yopable foundByID = select.uniqueResult(this.getConnection());
			json.onto(foundByID);
		} else {
			Set<Yopable> all = select.execute(this.getConnection());
			json.onto(all);
			if (restRequest.joinIDs) {
				json.joinIDsAll();
			}
			if (restRequest.joinAll) {
				json.joinAll();
			}
		}

		if (restRequest.joinIDs) {
			json.joinIDsAll();
		}
		if (restRequest.joinAll) {
			json.joinAll();
		}

		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		resp.getWriter().write(json.toJSON());
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doHead(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doPost(req, resp);
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

	private String invokeSubResource(RestRequest restRequest, Class<Yopable> target) {
		Optional<Method> candidate = Arrays.stream(target.getDeclaredMethods()).filter(restRequest::matches).findFirst();
		if (! candidate.isPresent()) {
			throw new UnsupportedOperationException("No method for subresource [" + restRequest.subResource + "]");
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
				}
			}
			return Objects.toString(method.invoke(target, parameters));
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static class RestRequest {
		private String method;
		private String restResource;
		private String subResource;
		private Long id = 0L;
		private boolean joinAll;
		private boolean joinIDs;
		private NameValuePair[] parameters;
		private Header[] headers;

		private RestRequest(HttpServletRequest req) {
			this.method = req.getMethod();

			String requestPath  = req.getRequestURI();
			String servletPath  = req.getServletPath();
			String resourcePath = StringUtils.removeStart(requestPath, servletPath);
			this.joinAll = req.getParameterMap().containsKey("joinAll");
			this.joinIDs = req.getParameterMap().containsKey("joinIDs");

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

		private boolean matches(Method method) {
			return method.isAnnotationPresent(Rest.class)
				&& Arrays.stream(method.getAnnotation(Rest.class).methods()).anyMatch(s -> this.method.equals(s))
				&& StringUtils.equals(method.getAnnotation(Rest.class).path(), this.subResource);
		}
	}
}
