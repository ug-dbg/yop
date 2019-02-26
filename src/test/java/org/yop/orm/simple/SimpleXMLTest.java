package org.yop.orm.simple;

import com.thoughtworks.xstream.converters.ConversionException;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Element;
import org.yop.orm.query.Join;
import org.yop.orm.query.JoinSet;
import org.yop.orm.query.serialize.xml.XML;
import org.yop.orm.simple.model.Jopo;
import org.yop.orm.simple.model.Other;
import org.yop.orm.simple.model.Pojo;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SimpleXMLTest {

	@Test
	public void testXML_1st_level() {
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

		String xml = XML.from(Pojo.class)
			.joinAll()
			.onto(pojo)
			.rootAlias("pojos")
			.execute();

		Collection<Pojo> pojos = XML.deserialize(xml, Pojo.class, "pojos");
		Assert.assertEquals(1, pojos.size());

		Pojo deserialized = pojos.iterator().next();
		Assert.assertEquals(deserialized, pojo);
		Assert.assertEquals(deserialized.getJopos(), pojo.getJopos());
		Assert.assertEquals(deserialized.getOthers(), pojo.getOthers());
	}

	@Test
	public void testXML_single_DOM_object() {
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

		String xml = XML.from(Pojo.class)
			.joinAll()
			.onto(pojo)
			.rootAlias("pojos")
			.execute();

		List<Element> elements = XML.getFirstLevelElements(xml, StandardCharsets.UTF_8);
		Assert.assertEquals(1, elements.size());

		Pojo deserialized = XML.deserialize(elements.get(0), Pojo.class);
		Assert.assertEquals(deserialized, pojo);
		Assert.assertEquals(deserialized.getJopos(), pojo.getJopos());
		Assert.assertEquals(deserialized.getOthers(), pojo.getOthers());
	}

	@Test
	public void testXML_2nd_level() {
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

		String xml = XML.from(Pojo.class)
			.joinAll()
			.join(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo)))
			.join(JoinSet.to(Pojo::getOthers).join(JoinSet.to(Other::getPojos)))
			.onto(Collections.singleton(pojo))
			.execute();

		Collection<Pojo> pojos = XML.deserialize(xml, Pojo.class, "pojos", Pojo.class, Jopo.class, Other.class);
		Pojo deserialized = pojos.iterator().next();
		Assert.assertEquals(deserialized, pojo);
		Assert.assertNotNull(deserialized.getJopos());
		Assert.assertEquals(1, deserialized.getJopos().size());
		Assert.assertTrue(deserialized == deserialized.getJopos().iterator().next().getPojo());
	}

	@Test(expected = ConversionException.class)
	public void testXML_2nd_level_unallowed_class() {
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

		String xml = XML.from(Pojo.class)
			.joinAll()
			.join(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo)))
			.join(Pojo::getOthers, Other::getPojos)
			.onto(Collections.singleton(pojo))
			.execute();

		// Don't allow 'Other' class → it should raise a ForbiddenClassException and then a ConversionException.
		XML.deserialize(xml, Pojo.class, "pojos", Pojo.class, Jopo.class);
	}
}
