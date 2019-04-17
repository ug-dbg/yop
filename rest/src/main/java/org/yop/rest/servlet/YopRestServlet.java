package org.yop.rest.servlet;


import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.sql.adapter.jdbc.JDBCConnection;
import org.yop.reflection.Reflection;
import org.yop.rest.annotations.Rest;
import org.yop.rest.exception.*;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

/**
 * A servlet that will answer HTTP requests for the {@link Rest} annotated Yopable objects.
 * <br>
 * Configure the servlet in your web.xml with the given parameters :
 * <ul>
 *     <li>{@link #PACKAGE_INIT_PARAM} : the package prefix to scan for {@link Rest} Yopable objects.</li>
 *     <li>
 *         {@link #DATASOURCE_JNDI_INIT_PARAM} the JNDI name of the datasource to use.
 *         Feel free to override {@link #getConnection()} if you directly have a JDBC connection with no JNDI.
 *     </li>
 *     <li>{@link #REQUEST_CHECKER_INIT_PARAM} if you want to add some security logic</li>
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

	/** Servlet init param : the packages to scan for {@link Rest} Yopable to expose */
	public static final String PACKAGE_INIT_PARAM = "packages";

	/** Servlet init param : a custom implementation for {@link RequestChecker}. Optional. */
	public static final String REQUEST_CHECKER_INIT_PARAM = "request_checker_class";

	/** Servlet init param : the datasource JNDI name. Optional if you override {@link #getConnection()} */
	public static final String DATASOURCE_JNDI_INIT_PARAM = "datasource_jndi";

	private final Yopables yopablePaths = new Yopables();
	private String dataSourceJNDIName;
	private DataSource dataSource;
	private Connector connector = this::getConnection;
	private RequestChecker requestChecker = new RequestChecker() {};

	/**
	 * Get the connection to the database.
	 * <br>
	 * This asks the {@link #dataSource} for a new connection.
	 * <br>
	 * Override this method if you need an other way to get a JDBC connection
	 * @return the underlying connection
	 */
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

	/**
	 * Set the connection to use for the servlet. The Connector is simply a lambda that returns a connection.
	 * @param connector the connector that can be a method reference to a JDBC connection
	 * @return the current REST servlet, for chaining purposes
	 */
	public YopRestServlet withConnector(Connector connector) {
		this.connector = connector;
		return this;
	}

	/**
	 * Register any {@link Rest} class whose package name starts with any of the given ones.
	 * @param packages package name filter the packages the @Rest class must start with. If empty : no package filter.
	 * @return the current servlet instance.
	 */
	public YopRestServlet register(String... packages) {
		this.yopablePaths.register(packages);
		return this;
	}

	/**
	 * Register a {@link Rest} class as REST webservice.
	 * @param target the target class(s) to register. If not @Rest annotated, do nothing.
	 * @return the current servlet instance.
	 */
	public YopRestServlet register(Class<?>... target) {
		this.yopablePaths.register(target);
		return this;
	}

	@Override
	public void init() throws ServletException {
		super.init();
		this.yopablePaths.register(this.getInitParameter(PACKAGE_INIT_PARAM));

		// The JNDI data source init param → the underlying connection
		// It can be null if the getConnection is overridden
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

		// A custom request checker class can be set using an init param
		String requestCheckerClass = this.getInitParameter(REQUEST_CHECKER_INIT_PARAM);
		if (StringUtils.isNotBlank(requestCheckerClass)) {
			this.requestChecker = Reflection.newInstanceNoArgs(Reflection.forName(requestCheckerClass));
			this.requestChecker.init(this.getServletConfig());
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
			resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
			resp.getWriter().write(HttpMethod.errorJSON(e).toString());
		} catch (YopNoAuthException e) {
			logger.error("YOP Rest resource invocation error, Not authenticated !", e);
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
			resp.getWriter().write(HttpMethod.errorJSON(e).toString());
		} catch (YopForbiddenException e) {
			logger.error("YOP Rest resource invocation error, Forbidden !", e);
			resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
			resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
			resp.getWriter().write(HttpMethod.errorJSON(e).toString());
		} catch (YopNoResultException e) {
			logger.error("YOP No resource for given ID !", e);
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
			resp.getWriter().write(HttpMethod.errorJSON(e).toString());
		} catch (YopNoResourceException e) {
			logger.error("YOP No resource for given path !", e);
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
			resp.getWriter().write(HttpMethod.errorJSON(e).toString());
		}catch (RuntimeException e) {
			logger.error("YOP Rest resource invocation error!", e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
			resp.getWriter().write(HttpMethod.errorJSON(e).toString());
		}
	}

	private void doUpsert(HttpServletRequest req, HttpServletResponse resp) {
		this.doExecute(req, resp, Upsert.INSTANCE);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	/**
	 * Execute the request using the {@link HttpMethod} implementation.
	 * <br>
	 * <ol>
	 *     <li>{@link HttpMethod#checkResource(RestRequest)}</li>
	 *     <li>{@link RequestChecker#checkResource(RestRequest, IConnection)} </li>
	 *     <li>Create transaction</li>
	 *     <li>Execute {@link HttpMethod#execute(RestRequest, IConnection)}</li>
	 *     <li>Commit or Rollback on exception</li>
	 *     <li>Serialize output to the response using {@link HttpMethod#serialize(Object, RestRequest)}</li>
	 * </ol>
	 * @param req    the servlet request
	 * @param resp   the servlet response
	 * @param method the method implementation (e.g. {@link Get}, {@link Post}...)
	 */
	private <T> void doExecute(HttpServletRequest req, HttpServletResponse resp, HttpMethod method) {
		RestRequest<T> restRequest = new RestRequest<>(req, resp, this.yopablePaths);
		method.checkResource(restRequest);

		RestResponse out;
		try (IConnection connection = this.connector.getConnection()) {
			this.requestChecker.checkResource(restRequest, connection);

			boolean autocommit = connection.getAutoCommit();
			connection.setAutoCommit(false);
			try {
				out = method.execute(restRequest, connection);
			} catch (RuntimeException e) {
				connection.rollback();
				connection.setAutoCommit(autocommit);
				throw e;
			}
			connection.commit();
		} catch (SQLException e) {
			throw new YopResourceInvocationException(
				"SQL Error invoking REST resource [" + restRequest.toString() + "]",
				e
			);
		}

		String serialized = method.serialize(out.output(), restRequest);
		out.headers().forEach(entry -> resp.setHeader(entry.getKey(), entry.getValue()));
		resp.setStatus(out.statusCode());

		if (StringUtils.isNotBlank(serialized)) {
			method.write(serialized, restRequest);
		}
	}

	/**
	 * A functional interface to a connection.
	 */
	public interface Connector {

		/**
		 * Get a connection to the underlying database
		 * @return a connection (e.g. {@link JDBCConnection})
		 */
		IConnection getConnection();
	}

	/**
	 * An interface to add extra checking logic on a REST request.
	 * <br>
	 * Implement this interface and set the class name as {@link #REQUEST_CHECKER_INIT_PARAM} into the servlet.
	 */
	public interface RequestChecker {
		/**
		 * Init method. Read the servlet config if you need it.
		 * @param config the REST servlet config
		 */
		default void init(ServletConfig config){}

		/**
		 * Check the incoming REST request.
		 * <br>
		 * Throw a runtime exception if you need to stop REST request execution :
		 * <ul>
		 *     <li>{@link YopBadContentException} → HTTP 400</li>
		 *     <li>{@link YopNoAuthException} → HTTP 401</li>
		 *     <li>{@link YopForbiddenException} → HTTP 403</li>
		 *     <li>{@link YopNoResourceException} / {@link YopNoResultException} → HTTP 404</li>
		 *     <li>any other {@link RuntimeException} → HTTP 500</li>
		 * </ul>
		 * @param request    the incoming request
		 * @param connection the underlying connection (e.g. do some security check on user credentials)
		 */
		default <T> void checkResource(RestRequest<T> request, IConnection connection){}
	}
}
