package org.yop.rest.simple.model;

import org.apache.commons.codec.digest.Crypt;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.query.Delete;
import org.yop.orm.query.Where;
import org.yop.orm.query.json.JSON;
import org.yop.orm.simple.model.Jopo;
import org.yop.orm.simple.model.Other;
import org.yop.orm.simple.model.Pojo;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.servlet.LoginServlet;
import org.yop.rest.servlet.RestServletTest;
import org.yop.rest.users.model.Action;
import org.yop.rest.users.model.Profile;
import org.yop.rest.users.model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

import static org.yop.orm.Yop.toSet;
import static org.yop.orm.Yop.upsert;

/**
 * Do some actions ont the {@link org.yop.rest.servlet.YopRestServlet} deployed in an embedded Tomcat.
 * <br>
 * We use Apache HTTP commons to build and execute HTTP requests.
 */
public class SimpleModelRestTest extends RestServletTest {

	private static final Logger logger = LoggerFactory.getLogger(SimpleModelRestTest.class);

	private static final String USER_NAME = "roger";
	private static final String USER_MAIL = "roger@roger.com";
	private static final String PASS_WORD = "ThisIsRoger'sPassword";

	@Override
	protected String getPackagePrefixes() {
		return "org.yop.orm.simple.model, org.yop.rest.users.model";
	}

	@Override
	public void setUp() throws SQLException, IOException, ClassNotFoundException {
		super.setUp();
	}

	@Test
	public void test_openAPI() throws IOException, SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			rogerCanRead(connection);
		}

		String sessionCookie = login();

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet("http://localhost:1234/yop/openapi");
			httpGet.setHeader("Cookie", sessionCookie);
			Response response = doRequest(httpclient, httpGet);
			Assert.assertEquals(200, response.statusCode);
			Assert.assertEquals(classpathResource("/openapi/expected_openapi.yaml"), response.content);
		}
	}

	@Test
	public void test_CRUD() throws SQLException, ClassNotFoundException, IOException {
		Pojo newPojo;
		try (IConnection connection = this.getConnection()) {
			newPojo = new Pojo();
			newPojo.setVersion(1);
			newPojo.setType(Pojo.Type.FOO);
			newPojo.setActive(true);
			newPojo.setStringColumn("This is a string that will be set in the string column");
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

			rogerCanRead(connection);
		}

		String sessionCookie = login();

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			// GET with joinAll, user logged in, user can read, not write → 200 with content
			HttpGet httpGet = new HttpGet("http://localhost:1234/yop/rest/pojo?joinAll");
			httpGet.setHeader("Cookie", sessionCookie);
			Response response = doRequest(httpclient, httpGet);
			Assert.assertEquals(200, response.statusCode);
			Assert.assertEquals(1, new JSONArray(response.content).length());

			// GET with joinAll, user not logged in → 401
			httpGet = new HttpGet("http://localhost:1234/yop/rest/pojo?joinAll");
			httpGet.setHeader("Cookie", "");
			response = doRequest(httpclient, httpGet);
			Assert.assertEquals(401, response.statusCode);
			Assert.assertEquals("{\"error\":\"No user logged in !\"}", response.content);

			// UPSERT,  user logged in, user can read, not write → 403
			HttpUpsert httpUpsert = new HttpUpsert("http://localhost:1234/yop/rest/pojo");
			httpUpsert.setEntity(new StringEntity("This is a test"));
			httpUpsert.setHeader("Cookie", sessionCookie);
			response = doRequest(httpclient, httpUpsert);
			Assert.assertEquals(403, response.statusCode);
			Assert.assertEquals(
				"{\"error\":\"User [roger@roger.com] is not allowed to write resource !\"}",
				response.content
			);

			try (IConnection connection = this.getConnection()) {
				rogerCanWrite(connection);
			}
			sessionCookie = login();

			// POST, /search/{search_string}  user logged in, user can read and write → 200
			HttpPost httpPost = new HttpPost("http://localhost:1234/yop/rest/pojo/search/will%20be%20set");
			httpPost.setHeader("Cookie", sessionCookie);
			response = doRequest(httpclient, httpPost);
			Assert.assertEquals(200, response.statusCode);
			Assert.assertEquals(
				"[{\"id\":1,\"version\":1,\"active\":true,"
				+ "\"stringColumn\":\"This is a string that will be set in the string column\",\"type\":\"FOO\"}]",
				response.content
			);

			// UPSERT,  user logged in, user can read and write, invalid JSON content → 400
			httpUpsert = new HttpUpsert("http://localhost:1234/yop/rest/pojo");
			httpUpsert.setEntity(new StringEntity("This is a test"));
			httpUpsert.setHeader("Cookie", sessionCookie);
			response = doRequest(httpclient, httpUpsert);
			Assert.assertEquals(400, response.statusCode);
			Assert.assertEquals("{\"error\":\"Unable to parse JSON array [This is a test]\"}", response.content);

			// UPSERT,  user logged in, user can read and write → 200
			httpUpsert = new HttpUpsert("http://localhost:1234/yop/rest/pojo");
			httpUpsert.setEntity(new StringEntity(JSON.from(Pojo.class).onto(newPojo).toJSON()));
			httpUpsert.setHeader("Cookie", sessionCookie);
			response = doRequest(httpclient, httpUpsert);
			Assert.assertEquals(200, response.statusCode);
			Assert.assertEquals(1, new JSONArray(response.content).length());
		}
	}

	private static void rogerCanRead(IConnection connection) {
		User user = new User();
		user.setName(USER_NAME);
		user.setPasswordHash(Crypt.crypt(PASS_WORD, LoginServlet.SALT));
		user.setEmail(USER_MAIL);

		Delete.from(User.class).joinAll().where(Where.naturalId(user)).executeQueries(connection);

		Profile admin = new Profile();
		admin.setName("admin");
		user.getProfiles().add(admin);

		Action read = new Action();
		read.setName("read");
		admin.getActionsForProfile().add(read);

		upsert(User.class).onto(user).joinAll().execute(connection);
	}

	private static void rogerCanWrite(IConnection connection) {
		User user = new User();
		user.setName(USER_NAME);
		user.setPasswordHash(Crypt.crypt(PASS_WORD, LoginServlet.SALT));
		user.setEmail(USER_MAIL);

		Delete.from(User.class).joinAll().where(Where.naturalId(user)).executeQueries(connection);

		Profile admin = new Profile();
		admin.setName("admin");
		user.getProfiles().add(admin);

		Action read = new Action();
		read.setName("read");
		admin.getActionsForProfile().add(read);

		Action write = new Action();
		write.setName("write");
		admin.getActionsForProfile().add(write);

		upsert(User.class).onto(user).joinAll().execute(connection);
	}

	private static String login() throws IOException {
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpPost loginPost = new HttpPost("http://localhost:1234/yop/login");
			loginPost.setEntity(new UrlEncodedFormEntity(Arrays.asList(
				new BasicNameValuePair("login", USER_MAIL),
				new BasicNameValuePair("password", PASS_WORD)
			)));
			try (CloseableHttpResponse response = httpclient.execute(loginPost)) {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode >= 400) {
					throw new RuntimeException("Login error !");
				}
				String sessionCookie = response.getHeaders("Set-Cookie")[0].getValue();
				logger.info("Login Status → [{}]", statusCode);
				Assert.assertEquals(200, statusCode);
				return sessionCookie;
			}
		}
	}
}
