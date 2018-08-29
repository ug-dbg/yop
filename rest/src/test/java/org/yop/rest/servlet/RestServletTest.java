package org.yop.rest.servlet;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.query.json.JSON;
import org.yop.orm.simple.model.Jopo;
import org.yop.orm.simple.model.Other;
import org.yop.orm.simple.model.Pojo;
import org.yop.orm.sql.adapter.IConnection;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.yop.orm.Yop.toSet;
import static org.yop.orm.Yop.upsert;

/**
 * Testing the {@link YopRestServlet} into an embedded Tomcat : {@link #tomcat}.
 * <br>
 * Using Apache http components to send requests to the embedded tomcat.
 */
public class RestServletTest extends DBMSSwitch {

	private static final Logger logger = LoggerFactory.getLogger(RestServletTest.class);

	private class YopRestServletWithConnection extends YopRestServlet {
		@Override
		protected IConnection getConnection() {
			try {
				return RestServletTest.this.getConnection();
			} catch (SQLException | ClassNotFoundException e) {
				throw new RuntimeException("Could not get connection !", e);
			}
		}
	}

	private Tomcat tomcat;

	@Override
	protected String getPackagePrefix() {
		return "org.yop.orm.simple.model";
	}

	@Before
	@Override
	public void setUp() throws SQLException, IOException, ClassNotFoundException {
		super.setUp();
		this.tomcat = new Tomcat();
		this.tomcat.setPort(1234);
		this.tomcat.enableNaming();

		Context context = this.tomcat.addContext("/", new File(".").getAbsolutePath());
		org.apache.catalina.Wrapper wrapper = Tomcat.addServlet(
			context,
			YopRestServletWithConnection.class.getSimpleName(),
			new YopRestServletWithConnection()
		);
		wrapper.addInitParameter(YopRestServlet.PACKAGE_INIT_PARAM, "org.yop");
		context.addServletMappingDecoded("/yop/rest/*", YopRestServletWithConnection.class.getSimpleName());

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

	@Test
	public void test() throws SQLException, ClassNotFoundException, IOException, InterruptedException {
		Pojo newPojo;
		try (IConnection connection = this.getConnection()) {
			newPojo = new Pojo();
			newPojo.setVersion(1);
			newPojo.setType(Pojo.Type.FOO);
			newPojo.setActive(true);
			Jopo jopo = new Jopo();
			jopo.setName("test path ref");
			jopo.setPojo(newPojo);
			newPojo.getJopos().add(jopo);

			Other other = new Other();
			other.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
			other.setName("test path ref");
			newPojo.getOthers().add(other);

			upsert(Pojo.class)
				.onto(newPojo)
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers))
				.checkNaturalID()
				.execute(connection);
		}

		// This keeps the tomcat server running, e.g. if you want to use the browser
//		while (true) {
//			Thread.sleep(1000);
//			if (false) break;
//		}

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet("http://localhost:1234/yop/rest/pojo?joinAll");
			try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
				logger.info("GET Status → [{}]", response.getStatusLine().getStatusCode());
				String output = IOUtils.toString(response.getEntity().getContent());
				Assert.assertNotNull(output);
			}

			HttpPost httpPost = new HttpPost("http://localhost:1234/yop/rest/pojo/search?joinAll");
			httpPost.setEntity(new StringEntity("This is a test"));
			try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
				logger.info("POST Status → [{}]", response.getStatusLine().getStatusCode());
				String output = IOUtils.toString(response.getEntity().getContent());
				Assert.assertNotNull(output);
			}

			HttpUpsert httpUpsert = new HttpUpsert("http://localhost:1234/yop/rest/pojo");
			httpUpsert.setEntity(new StringEntity("This is a test"));
			try (CloseableHttpResponse response = httpclient.execute(httpUpsert)) {
				logger.info("UPSERT Status → [{}]", response.getStatusLine().getStatusCode());
				Assert.assertEquals(400, response.getStatusLine().getStatusCode());
				String output = IOUtils.toString(response.getEntity().getContent());
				Assert.assertNotNull(output);
			}

			httpUpsert = new HttpUpsert("http://localhost:1234/yop/rest/pojo");
			httpUpsert.setEntity(new StringEntity(JSON.from(Pojo.class).onto(newPojo).toJSON()));
			try (CloseableHttpResponse response = httpclient.execute(httpUpsert)) {
				logger.info("UPSERT Status → [{}]", response.getStatusLine().getStatusCode());
				Assert.assertEquals(200, response.getStatusLine().getStatusCode());
				String output = IOUtils.toString(response.getEntity().getContent());
				Assert.assertNotNull(output);
			}
		}
	}

	private static class HttpUpsert extends HttpEntityEnclosingRequestBase {
		HttpUpsert(final String uri) {
			super();
			setURI(URI.create(uri));
		}

		@Override
		public String getMethod() {
			return Upsert.UPSERT;
		}
	}
}
