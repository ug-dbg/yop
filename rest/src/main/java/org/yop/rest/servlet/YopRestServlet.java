package org.yop.rest.servlet;


import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.sql.adapter.jdbc.JDBCConnection;
import org.yop.rest.annotations.Rest;
import org.yop.rest.exception.YopBadContentException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A servlet that will answer HTTP requests for the {@link Rest} annotated {@link Yopable} objects.
 * <br>
 * Configure the servlet in your web.xml with the given parameters :
 * <ul>
 *     <li>{@link #PACKAGE_INIT_PARAM} : the package prefix to scan for {@link Rest} {@link Yopable} objects.</li>
 *     <li>
 *         {@link #DATASOURCE_JNDI_INIT_PARAM} the JNDI name of the datasource to use.
 *         Feel free to override {@link #getConnection()} if you directly have a JDBC connection with no JNDI.
 *     </li>
 * </ul>
 * Supported HTTP methods :
 * <ul>
 *     <li>{@link Head}</li>
 *     <li>{@link Get}</li>
 *     <li>{@link Post} (not implemented yet)</li>
 *     <li>{@link Upsert} (custom HTTP method)</li>
 *     <li>{@link Put} (does {@link Upsert}, not idempotent)</li>
 *     <li>{@link Delete} (not implemented yet)</li>
 * </ul>
 */
public class YopRestServlet extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(YopRestServlet.class);

	static final String PACKAGE_INIT_PARAM = "packages";

	private static final String DATASOURCE_JNDI_INIT_PARAM = "datasource_jndi";

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
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("Finding REST resource for GET [{}] ", req.getRequestURI());
		this.doExecute(req, resp, Get.INSTANCE);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("Finding REST resource for HEAD [{}] ", req.getRequestURI());
		this.doExecute(req, resp, Head.INSTANCE);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("Finding REST resource for POST [{}] ", req.getRequestURI());
		this.doExecute(req, resp, Post.INSTANCE);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("Finding REST resource for PUT [{}] ", req.getRequestURI());
		this.doExecute(req, resp, Put.INSTANCE);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
		logger.info("Finding REST resource for DELETE [{}] ", req.getRequestURI());
		this.doExecute(req, resp, Delete.INSTANCE);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doOptions(req, resp);
	}

	@Override
	protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doTrace(req, resp);
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			if (Upsert.UPSERT.equals(req.getMethod())) {
				this.doUpsert(req, resp);
				return;
			}
			super.service(req, resp);
		} catch (YopBadContentException e) {
			logger.error("YOP Rest resource invocation error, Bad request !", e);
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.getWriter().write(errorJSON(e).toString());
		} catch (RuntimeException e) {
			logger.error("YOP Rest resource invocation error!", e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().write(errorJSON(e).toString());
		}
	}

	private void doUpsert(HttpServletRequest req, HttpServletResponse resp) {
		this.doExecute(req, resp, Upsert.INSTANCE);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	private void doExecute(HttpServletRequest req, HttpServletResponse resp, HttpMethod method) {
		RestRequest restRequest = new RestRequest(req, resp, this.yopablePaths);
		method.checkResource(restRequest);
		Object out = method.execute(restRequest, this.getConnection());
		String serialized = method.serialize(out, restRequest);

		if (StringUtils.isNotBlank(serialized)) {
			method.write(serialized, restRequest);
		}
	}

	private static JSONObject errorJSON(Throwable cause) {
		return new JSONObject().put("error", cause.getMessage());
	}
}
