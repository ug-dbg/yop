package org.yop.rest.servlet;


import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.sql.adapter.jdbc.JDBCConnection;
import org.yop.rest.annotations.Rest;

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
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		logger.info("Finding REST resource for GET [{}] ", req.getRequestURI());
		this.doExecute(req, resp, Get.INSTANCE);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		logger.info("Finding REST resource for HEAD [{}] ", req.getRequestURI());
		this.doExecute(req, resp, Head.INSTANCE);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		logger.info("Finding REST resource for POST [{}] ", req.getRequestURI());
		this.doExecute(req, resp, Post.INSTANCE);
		resp.setStatus(HttpServletResponse.SC_OK);
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

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if ("UPSERT".equals(req.getMethod())) {
			this.doUpsert(req, resp);
			return;
		}
		super.service(req, resp);
	}

	private void doUpsert(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.getWriter().write("It works !");
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	private void doExecute(HttpServletRequest req, HttpServletResponse resp, HttpMethod method) throws IOException {
		RestRequest restRequest = new RestRequest(req, resp, this.yopablePaths);
		if (method.isInvalidResource(restRequest)) {
			return;
		}
		Object out = method.execute(restRequest, this.getConnection());
		String serialized = method.serialize(out, restRequest);

		if (StringUtils.isNotBlank(serialized)) {
			method.write(serialized, restRequest);
		}
	}
}
