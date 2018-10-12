package org.yop.rest.servlet;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.sql.adapter.IConnection;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

/**
 * Testing the {@link YopRestServlet} into an embedded Tomcat : {@link #tomcat}.
 * <br>
 * Using Apache http components to send requests to the embedded tomcat.
 * <br>
 * 3 servlets are exposed in the embedded tomcat :
 * <ul>
 *     <li>{@link LoginServlet}   → /yop/login</li>
 *     <li>{@link YopRestServlet} → /yop/rest/*</li>
 *     <li>{@link OpenAPIServlet} → /yop/openapi</li>
 * </ul>
 */
public abstract class RestServletTest extends DBMSSwitch {

	private static final Logger logger = LoggerFactory.getLogger(RestServletTest.class);

	/**
	 * Override the {@link #getConnection()} method in the YOP servlet :
	 * it is quite tedious to add a JNDI datasource programmatically in the embedded Tomcat.
	 */
	public class YopRestServletWithConnection extends YopRestServlet {
		@Override
		protected IConnection getConnection() {
			try {
				return RestServletTest.this.getConnection();
			} catch (SQLException | ClassNotFoundException e) {
				throw new RuntimeException("Could not get connection !", e);
			}
		}
	}

	protected Tomcat tomcat;

	@Before
	@Override
	public void setUp() throws SQLException, IOException, ClassNotFoundException {
		super.setUp();
		this.tomcat = new Tomcat();
		this.tomcat.setPort(1234);
		this.tomcat.enableNaming();

		Context context = this.tomcat.addContext("/", new File(".").getAbsolutePath());
		this.onContextCreation(context);
		this.addLoginServlet(context);
		this.addYopServlet(context);
		this.addOpenAPIServlet(context);

		try {
			logger.info("Starting embedded Tomcat on port [{}]", this.tomcat.getServer().getPort());
			this.tomcat.getConnector();
			this.tomcat.start();
		} catch (LifecycleException e) {
			throw new RuntimeException("Embedded Tomcat Lifecycle exception when starting !", e);
		}
	}

	@After
	public void finish() {
		try {
			logger.info("Stopping embedded Tomcat.");
			this.tomcat.stop();
		} catch (LifecycleException e) {
			throw new RuntimeException("Embedded Tomcat Lifecycle exception when stopping !", e);
		}
	}

	protected void addLoginServlet(Context context) {
		Tomcat.addServlet(
		context,
		LoginServlet.class.getSimpleName(),
		new LoginServlet(){
			@Override
			protected IConnection getConnection() throws ClassNotFoundException, SQLException {
				return RestServletTest.this.getConnection();
			}
		});
		context.addServletMappingDecoded("/yop/login", LoginServlet.class.getSimpleName());
	}

	protected void addYopServlet(Context context) {
		Wrapper wrapper = Tomcat.addServlet(
			context,
			YopRestServletWithConnection.class.getSimpleName(),
			new YopRestServletWithConnection()
		);
		wrapper.addInitParameter(YopRestServlet.PACKAGE_INIT_PARAM, "org.yop");
		wrapper.addInitParameter(YopRestServlet.REQUEST_CHECKER_INIT_PARAM, CredentialsChecker.class.getName());
		wrapper.addInitParameter(YopRestServlet.DATASOURCE_JNDI_INIT_PARAM, null);
		context.addServletMappingDecoded("/yop/rest/*", YopRestServletWithConnection.class.getSimpleName());
	}

	private void addOpenAPIServlet(Context context) {
		Wrapper wrapper = Tomcat.addServlet(context, OpenAPIServlet.class.getSimpleName(), new OpenAPIServlet());
		wrapper.addInitParameter(OpenAPIServlet.PACKAGE_INIT_PARAM, "org.yop");
		wrapper.addInitParameter(OpenAPIServlet.EXPOSITION_PATH, "/yop/rest");
		context.addServletMappingDecoded("/yop/openapi", OpenAPIServlet.class.getSimpleName());
	}

	protected void onContextCreation(Context context) {}

	protected static class HttpUpsert extends HttpEntityEnclosingRequestBase {
		public HttpUpsert(final String uri) {
			super();
			this.setURI(URI.create(uri));
		}

		@Override
		public String getMethod() {
			return Upsert.UPSERT;
		}
	}
}
