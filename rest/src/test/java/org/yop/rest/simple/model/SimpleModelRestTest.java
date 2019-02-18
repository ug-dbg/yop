package org.yop.rest.simple.model;

import com.google.gson.JsonParser;
import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.query.*;
import org.yop.orm.query.serialize.json.JSON;
import org.yop.orm.simple.model.Jopo;
import org.yop.orm.simple.model.Other;
import org.yop.orm.simple.model.Pojo;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.servlet.HttpMethod;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

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
	protected String getPackageNames() {
		return "org.yop.rest.simple.model, org.yop.rest.users.model";
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
			jopo.setName("JOPO test CRUD with REST");
			jopo.setPojo(newPojo);
			newPojo.getJopos().add(jopo);

			Other other = new Other();
			other.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
			other.setName("OTHER test CRUD with REST");
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
			int contentLength = response.content.getBytes().length;
			Long id = ((JSONObject) new JSONArray(response.content).get(0)).getLong("id");

			// HEAD with joinAll, user logged in, user can read, not write → 200 with no content
			HttpHead httpHead = new HttpHead("http://localhost:1234/yop/rest/pojo?joinAll");
			httpHead.setHeader("Cookie", sessionCookie);
			response = doRequest(httpclient, httpHead);
			Assert.assertEquals(200, response.statusCode);
			Assert.assertEquals(0, StringUtils.length(response.content));
			Assert.assertEquals(contentLength, response.contentLength);

			// GET by ID, user logged in, user can read → 200 with content
			httpGet = new HttpGet("http://localhost:1234/yop/rest/pojo/" + id);
			httpGet.setHeader("Cookie", sessionCookie);
			response = doRequest(httpclient, httpGet);
			Assert.assertEquals(200, response.statusCode);
			Assert.assertEquals((long) id, ((JSONObject) (new JSONArray(response.content).get(0))).getLong("id"));

			// GET by unknown ID, user logged in, user can read → 404
			httpGet = new HttpGet("http://localhost:1234/yop/rest/pojo/1337");
			httpGet.setHeader("Cookie", sessionCookie);
			response = doRequest(httpclient, httpGet);
			Assert.assertEquals(404, response.statusCode);

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

			// PUT (same as UPSERT),  user logged in, user can read and write → 200
			HttpPut httpPut = new HttpPut("http://localhost:1234/yop/rest/pojo");
			httpPut.setEntity(new StringEntity(JSON.from(Pojo.class).onto(newPojo).toJSON()));
			httpPut.setHeader("Cookie", sessionCookie);
			response = doRequest(httpclient, httpPut);
			Assert.assertEquals(200, response.statusCode);
			Assert.assertEquals(1, new JSONArray(response.content).length());

			// DELETE by ID,  user logged in, user can read and write → 200
			HttpDelete httpDelete = new HttpDelete("http://localhost:1234/yop/rest/pojo/" + id);
			httpDelete.setHeader("Cookie", sessionCookie);
			response = doRequest(httpclient, httpDelete);
			Assert.assertEquals(200, response.statusCode);

			// DELETE all,  user logged in, user can read and write → 200
			httpDelete = new HttpDelete("http://localhost:1234/yop/rest/pojo");
			httpDelete.setHeader("Cookie", sessionCookie);
			response = doRequest(httpclient, httpDelete);
			Assert.assertEquals(200, response.statusCode);
		}
	}

	@Test
	public void test_custom_query_POST() throws SQLException, ClassNotFoundException, IOException {
		Pojo newPojo;
		try (IConnection connection = this.getConnection()) {
			newPojo = new org.yop.rest.simple.model.Pojo();
			newPojo.setVersion(18);
			newPojo.setType(Pojo.Type.FOO);
			newPojo.setActive(true);
			newPojo.setStringColumn("This is a string that will be set in the string column");
			Jopo jopo = new Jopo();
			jopo.setName("JOPO test REST custom query");
			jopo.setPojo(newPojo);
			newPojo.getJopos().add(jopo);

			Other other = new Other();
			other.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
			other.setName("OTHER test REST custom query");
			newPojo.getOthers().add(other);

			upsert(Pojo.class)
				.onto(newPojo)
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers))
				.checkNaturalID()
				.execute(connection);

			rogerCanWrite(connection);
		}

		String sessionCookie = login();
		String jsonQuery = Select
			.from(org.yop.rest.simple.model.Pojo.class)
			.where(Where.compare(Pojo::getType, Operator.EQ, Pojo.Type.FOO))
			.join(JoinSet.to(Pojo::getJopos))
			.join(JoinSet.to(Pojo::getOthers))
			.toJSON()
			.toString();

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			// POST with custom json query, user logged in, user can read and write, wrong Yopable endpoint → 500
			HttpPost httpPost = new HttpPost("http://localhost:1234/yop/rest/action?queryType=select");
			httpPost.setEntity(new StringEntity(jsonQuery));
			httpPost.setHeader("Cookie", sessionCookie);
			Response response = doRequest(httpclient, httpPost);
			Assert.assertEquals(500, response.statusCode);
			Assert.assertEquals(
				"{\"error\":\"The Select request for [org.yop.rest.simple.model.Pojo] "
				+ "should be invoked on the appropriate REST resource "
				+ "instead of [org.yop.rest.users.model.Action]\"}",
				response.content
			);
		}

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			// POST with custom json query, user logged in, user can read and write → 200 with content
			HttpPost httpPost = new HttpPost("http://localhost:1234/yop/rest/pojo?queryType=select");
			httpPost.setEntity(new StringEntity(jsonQuery));
			httpPost.setHeader("Cookie", sessionCookie);
			Response response = doRequest(httpclient, httpPost);
			Assert.assertEquals(200, response.statusCode);
			Assert.assertEquals(1, new JSONArray(response.content).length());
			Collection<Pojo> out = JSON.from(Pojo.class, new JsonParser().parse(response.content).getAsJsonArray());
			Pojo fromREST = out.iterator().next();
			Assert.assertEquals(newPojo.getVersion(), fromREST.getVersion());
			Assert.assertEquals(1, fromREST.getJopos().size());
			Assert.assertEquals(1, fromREST.getOthers().size());
		}

		newPojo.setType(Pojo.Type.BAR);
		jsonQuery = Upsert
			.from(org.yop.rest.simple.model.Pojo.class)
			.onto((org.yop.rest.simple.model.Pojo) newPojo)
			.toJSON()
			.toString();

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			// POST with custom json query, user logged in, user can read and write → 200 with content
			HttpPost httpPost = new HttpPost("http://localhost:1234/yop/rest/pojo?queryType=upsert");
			httpPost.setEntity(new StringEntity(jsonQuery));
			httpPost.setHeader("Cookie", sessionCookie);
			Response response = doRequest(httpclient, httpPost);
			Assert.assertEquals(200, response.statusCode);

			try (IConnection connection = this.getConnection()) {
				Pojo fromDB = Select.from(Pojo.class).uniqueResult(connection);
				Assert.assertEquals(Pojo.Type.BAR, fromDB.getType());
			}
		}

		jsonQuery = Delete
			.from(org.yop.rest.simple.model.Pojo.class)
			.where(Where.naturalId(newPojo))
			.toJSON()
			.toString();

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			// POST with custom json query, user logged in, user can read and write → 200 with content
			HttpPost httpPost = new HttpPost("http://localhost:1234/yop/rest/pojo?queryType=delete");
			httpPost.setEntity(new StringEntity(jsonQuery));
			httpPost.setHeader("Cookie", sessionCookie);
			Response response = doRequest(httpclient, httpPost);
			Assert.assertEquals(200, response.statusCode);

			try (IConnection connection = this.getConnection()) {
				Pojo fromDB = Select.from(Pojo.class).uniqueResult(connection);
				Assert.assertNull(fromDB);
			}
		}
	}

	@Test
	public void test_CRUD_Paging() throws SQLException, ClassNotFoundException, IOException {
		Pojo newPojo;
		try (IConnection connection = this.getConnection()) {
			Collection<Pojo> pojos = new ArrayList<>(20);
			for (int i = 1; i <= 20; i++) {
				newPojo = new Pojo();
				newPojo.setVersion(i);
				newPojo.setType(Pojo.Type.FOO);
				newPojo.setActive(true);
				newPojo.setStringColumn("This is a string that will be set in the string column");

				Jopo jopo1 = new Jopo();
				jopo1.setName("JOPO 1 test CRUD with REST");
				jopo1.setPojo(newPojo);
				newPojo.getJopos().add(jopo1);

				Jopo jopo2 = new Jopo();
				jopo2.setName("JOPO 2 test CRUD with REST");
				jopo2.setPojo(newPojo);
				newPojo.getJopos().add(jopo2);

				Other other = new Other();
				other.setTimestamp(LocalDateTime.of(1999, 1, i, 13, 37));
				other.setName("OTHER test CRUD with REST");
				newPojo.getOthers().add(other);

				pojos.add(newPojo);
			}

			upsert(Pojo.class)
				.onto(pojos)
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers))
				.checkNaturalID()
				.execute(connection);

			rogerCanRead(connection);
		}

		String sessionCookie = login();

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			// HEAD with joinAll and paging/count header, user logged in, user can read → 200 with no content
			HttpGet httpHead = new HttpGet("http://localhost:1234/yop/rest/pojo?joinAll");
			httpHead.setHeader("Cookie", sessionCookie);
			httpHead.setHeader(HttpMethod.PARAM_COUNT, Boolean.TRUE.toString());
			httpHead.setHeader(HttpMethod.PARAM_LIMIT, "5");
			Response response = doRequest(httpclient, httpHead);
			Assert.assertEquals(200, response.statusCode);
			Assert.assertEquals("20", response.getHeaderValue("count"));

			// GET with joinAll and paging/count header, user logged in, user can read → 200 with content
			HttpGet httpGet = new HttpGet("http://localhost:1234/yop/rest/pojo?joinAll");
			httpGet.setHeader("Cookie", sessionCookie);
			httpGet.setHeader(HttpMethod.PARAM_COUNT, Boolean.TRUE.toString());
			httpGet.setHeader(HttpMethod.PARAM_LIMIT, "5");
			response = doRequest(httpclient, httpGet);
			Assert.assertEquals(200, response.statusCode);
			Assert.assertEquals(5, new JSONArray(response.content).length());
			Assert.assertEquals("20", response.getHeaderValue("count"));

			// GET with joinAll and paging/count header, user logged in, user can read → 200 with content
			httpGet = new HttpGet("http://localhost:1234/yop/rest/pojo?joinAll");
			httpGet.setHeader("Cookie", sessionCookie);
			httpGet.setHeader(HttpMethod.PARAM_COUNT, Boolean.TRUE.toString());
			httpGet.setHeader(HttpMethod.PARAM_LIMIT,  "5");
			httpGet.setHeader(HttpMethod.PARAM_OFFSET, "18");
			response = doRequest(httpclient, httpGet);
			Assert.assertEquals(200, response.statusCode);
			Assert.assertEquals(2, new JSONArray(response.content).length());
			Assert.assertEquals("20", response.getHeaderValue("count"));
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
