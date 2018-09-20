package org.yop.rest.simple.model;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.query.json.JSON;
import org.yop.orm.simple.model.Jopo;
import org.yop.orm.simple.model.Other;
import org.yop.orm.simple.model.Pojo;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.servlet.RestServletTest;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.yop.orm.Yop.toSet;
import static org.yop.orm.Yop.upsert;

public class SimpleModelRestTest extends RestServletTest {

	private static final Logger logger = LoggerFactory.getLogger(SimpleModelRestTest.class);

	@Override
	protected String getPackagePrefix() {
		return "org.yop.orm.simple.model";
	}

	@Test
	public void test() throws SQLException, ClassNotFoundException, IOException {
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
}
