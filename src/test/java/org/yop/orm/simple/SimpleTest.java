package org.yop.orm.simple;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.yop.orm.example.Jopo;
import org.yop.orm.example.Other;
import org.yop.orm.example.Pojo;
import org.yop.orm.gen.Prepare;
import org.yop.orm.query.*;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Set;

/**
 * Simple test with simple objects for simple CRUD.
 * I should find a more explicit name. Sorry about that.
 */
public class SimpleTest {

	private File db;

	@Before
	public void setUp() throws SQLException, IOException, ClassNotFoundException {
		this.db = Prepare.createSQLiteDatabase(SimpleTest.class.getName(), "org.yop.orm");
	}

	@Test
	public void testCRUD() throws SQLException, IOException, ClassNotFoundException {
		try (Connection connection = Prepare.getConnection(this.db)) {
			Pojo newPojo = new Pojo();
			newPojo.setVersion(1337);
			newPojo.setType(Pojo.Type.FOO);
			Jopo jopo = new Jopo();
			jopo.setName("jopo From code !");
			jopo.setPojo(newPojo);
			newPojo.getJopos().add(jopo);

			Other other = new Other();
			other.setTimestamp(Instant.now());
			other.setName("other name :)");
			newPojo.getOthers().add(other);

			Upsert
				.from(Pojo.class)
				.onto(newPojo)
				.join(JoinSet.to(Pojo::getJopos))
				.join(JoinSet.to(Pojo::getOthers))
				.checkNaturalID()
				.execute(connection);

			Set<Pojo> found = Select
				.from(Pojo.class)
				.where(Where.naturalId(newPojo))
				.joinAll()
				.execute(connection, Select.STRATEGY.EXISTS);

			Assert.assertEquals(1, found.size());

			Pojo newPojoFromSelect = found.iterator().next();
			Assert.assertEquals(newPojo, newPojoFromSelect);

			Assert.assertEquals(1, newPojoFromSelect.getJopos().size());
			Assert.assertEquals(1, newPojoFromSelect.getOthers().size());

			Assert.assertEquals(newPojo.getOthers().iterator().next(), newPojoFromSelect.getOthers().iterator().next());
			Assert.assertEquals(newPojo.getJopos().iterator().next(), newPojoFromSelect.getJopos().iterator().next());

			Delete.from(Pojo.class).executeQuery(connection);

			Set<Pojo> afterDelete = Select.from(Pojo.class).joinAll().execute(connection, Select.STRATEGY.EXISTS);
			Assert.assertEquals(0, afterDelete.size());
		}
	}

}