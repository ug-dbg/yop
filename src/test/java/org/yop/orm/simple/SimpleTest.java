package org.yop.orm.simple;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yop.orm.example.Jopo;
import org.yop.orm.example.Other;
import org.yop.orm.example.Pojo;
import org.yop.orm.gen.Prepare;
import org.yop.orm.query.*;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * Simple test with simple objects for simple CRUD.
 * I should find a more explicit name. Sorry about that.
 */
public class SimpleTest {

	private File db;

	@BeforeClass
	public static void init() {
		System.setProperty("yop.show_sql", "true");
	}

	@Before
	public void setUp() throws SQLException, IOException, ClassNotFoundException {
		this.db = Prepare.createSQLiteDatabase(SimpleTest.class.getName(), "org.yop.orm");
	}

	@Test
	public void testCRUD() throws SQLException, ClassNotFoundException {
		try (Connection connection = Prepare.getConnection(this.db)) {
			Pojo newPojo = new Pojo();
			newPojo.setVersion(1337);
			newPojo.setType(Pojo.Type.FOO);
			Jopo jopo = new Jopo();
			jopo.setName("jopo From code !");
			jopo.setPojo(newPojo);
			newPojo.getJopos().add(jopo);

			Other other = new Other();
			other.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
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
				.execute(connection, Select.Strategy.EXISTS);

			Assert.assertEquals(1, found.size());

			Pojo newPojoFromSelect = found.iterator().next();
			Assert.assertEquals(newPojo, newPojoFromSelect);

			Assert.assertEquals(1, newPojoFromSelect.getJopos().size());
			Assert.assertEquals(1, newPojoFromSelect.getOthers().size());

			Assert.assertEquals(newPojo.getOthers().iterator().next(), newPojoFromSelect.getOthers().iterator().next());
			Assert.assertEquals(newPojo.getJopos().iterator().next(), newPojoFromSelect.getJopos().iterator().next());

			Delete.from(Pojo.class).executeQuery(connection);

			Set<Pojo> afterDelete = Select.from(Pojo.class).joinAll().execute(connection);
			Assert.assertEquals(0, afterDelete.size());

			// Assertion that the relation was cleaned in the association table.
			Executor.Action action = results -> {
				results.getResultSet().next();
				Assert.assertEquals(0, results.getResultSet().getObject(1));
				return "";
			};

			Executor.executeQuery(
				connection,
				new Query("SELECT COUNT(*) FROM POJO_JOPO_relation", new Parameters()),
				action
			);
		}
	}

	@Test
	public void testLotOfChildren() throws SQLException, ClassNotFoundException {
		try (Connection connection = Prepare.getConnection(this.db)) {
			Pojo pojo = new Pojo();
			pojo.setVersion(1337);
			pojo.setType(Pojo.Type.FOO);

			for(int i = 0; i < 50; i++) {
				Jopo jopo = new Jopo();
				jopo.setName("jopo [" + i + "]");
				jopo.setPojo(pojo);
				pojo.getJopos().add(jopo);
			}

			Upsert.from(Pojo.class).joinAll().onto(pojo).execute(connection);

			Set<Jopo> jopos = Select.from(Jopo.class).join(Join.to(Jopo::getPojo)).execute(connection);
			Assert.assertEquals(50, jopos.size());

			for (Jopo jopo : jopos) {
				Assert.assertEquals(pojo, jopo.getPojo());
			}

			Delete.from(Jopo.class).executeQuery(connection);
			jopos = Select.from(Jopo.class).join(Join.to(Jopo::getPojo)).execute(connection);
			Assert.assertEquals(0, jopos.size());

			// Assertion that the relation was cleaned in the association table.
			Executor.Action action = results -> {
				results.getResultSet().next();
				Assert.assertEquals(0, results.getResultSet().getObject(1));
				return "";
			};

			Executor.executeQuery(
				connection,
				new Query("SELECT COUNT(*) FROM POJO_JOPO_relation", new Parameters()),
				action
			);
		}
	}

	@Test
		public void testSafeAlias() throws SQLException, ClassNotFoundException {
		try (Connection connection = Prepare.getConnection(this.db)) {
			Pojo pojo = new Pojo();
			pojo.setVersion(1337);
			pojo.setType(Pojo.Type.FOO);

			for (int i = 0; i < 3; i++) {
				Jopo jopo = new Jopo();
				jopo.setName("jopo [" + i + "]");
				jopo.setPojo(pojo);
				pojo.getJopos().add(jopo);
			}

			Upsert.from(Pojo.class).joinAll().onto(pojo).execute(connection);

			// Stupid joins that should make some column/table aliases length > 64 charcters
			Set<Pojo> found = Select.from(Pojo.class)
				.join(
					JoinSet.to(Pojo::getJopos)
					.join(Join.to(Jopo::getPojo).join(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo).join(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo).join(JoinSet.to(Pojo::getJopos)))))))
				)
				.execute(connection);
			Assert.assertEquals(1, found.size());
			Assert.assertEquals(pojo, found.iterator().next());
		}
	}

}
