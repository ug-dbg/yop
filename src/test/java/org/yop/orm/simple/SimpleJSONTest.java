package org.yop.orm.simple;

import com.google.gson.JsonPrimitive;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.yop.orm.query.join.Join;
import org.yop.orm.query.serialize.json.JSON;
import org.yop.orm.simple.model.Jopo;
import org.yop.orm.simple.model.Other;
import org.yop.orm.simple.model.Pojo;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

import static org.yop.orm.Yop.json;
import static org.yop.orm.Yop.select;

public class SimpleJSONTest {

	@Test
	public void testJSON_1st_level() throws IOException, JSONException {
		String password = "ThisIsMyPasswordYouFool";

		Pojo pojo = new Pojo();
		pojo.setId(1L);
		pojo.setVersion(1337);
		pojo.setActive(true);
		pojo.setType(Pojo.Type.FOO);
		pojo.setPassword(password);

		Jopo jopo = new Jopo();
		jopo.setId(11L);
		jopo.setName("jopo From code !");
		jopo.setPojo(pojo);
		pojo.getJopos().add(jopo);

		Other other = new Other();
		other.setId(111L);
		other.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
		other.setName("other name :)");
		pojo.getOthers().add(other);

		String json = json(Pojo.class)
			.joinAll()
			.onto(pojo)
			.register(LocalDateTime.class, (src, typeOfSrc, context) -> new JsonPrimitive("2000-01-01T00:00:00.000"))
			.toJSON();
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/simple/json/testJSON_1st_level_expected.json")
		);
		JSONAssert.assertEquals("", expected, json, true);
	}

	@Test
	public void testJSON_2nd_level() throws IOException, JSONException {
		Pojo pojo = new Pojo();
		pojo.setId(1L);
		pojo.setVersion(1337);
		pojo.setActive(true);
		pojo.setType(Pojo.Type.FOO);

		Jopo jopo = new Jopo();
		jopo.setId(11L);
		jopo.setName("jopo From code !");
		jopo.setPojo(pojo);
		pojo.getJopos().add(jopo);

		Other other = new Other();
		other.setId(111L);
		other.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
		other.setName("other name :)");
		other.getPojos().add(pojo);
		pojo.getOthers().add(other);

		Pojo anotherPojo = new Pojo();
		anotherPojo.setId(2L);
		other.getPojos().add(anotherPojo);

		String json = json(Pojo.class)
			.joinAll()
			.join(Pojo::getJopos, Jopo::getPojo)
			.join(Join.toN(Pojo::getOthers).join(Join.toN(Other::getPojos)))
			.register(LocalDateTime.class, (src, typeOfSrc, context) -> new JsonPrimitive("2000-01-01T00:00:00.000"))
			.onto(Collections.singleton(pojo))
			.toJSON();
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/simple/json/testJSON_2nd_level_expected.json")
		);
		JSONAssert.assertEquals("", expected, json, true);
	}

	@Test
	public void testSelect_to_JSON() throws IOException, JSONException {
		Pojo pojo = new Pojo();
		pojo.setId(1L);
		pojo.setVersion(1337);
		pojo.setActive(true);
		pojo.setType(Pojo.Type.FOO);

		Jopo jopo = new Jopo();
		jopo.setId(11L);
		jopo.setName("jopo From code !");
		jopo.setPojo(pojo);
		pojo.getJopos().add(jopo);

		Other other = new Other();
		other.setId(111L);
		other.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
		other.setName("other name :)");
		pojo.getOthers().add(other);

		JSON<Pojo> jsonQuery = select(Pojo.class)
			.joinAll()
			.to(JSON.from(Pojo.class))
			.register(LocalDateTime.class, (src, typeOfSrc, context) -> new JsonPrimitive("2000-01-01T00:00:00.000"));
		String json = jsonQuery.toJSON(pojo);
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/simple/json/testSelect_to_JSON_expected.json")
		);
		JSONAssert.assertEquals("", expected, json, true);
	}
}
