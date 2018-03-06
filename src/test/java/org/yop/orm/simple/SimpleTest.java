package org.yop.orm.simple;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.simple.model.Jopo;
import org.yop.orm.simple.model.Other;
import org.yop.orm.simple.model.Pojo;
import org.yop.orm.map.IdMap;
import org.yop.orm.query.*;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;

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
public class SimpleTest extends DBMSSwitch {

	private static final Logger logger = LoggerFactory.getLogger(SimpleTest.class);

	@Test
	public void testCRUD() throws SQLException, ClassNotFoundException {
		try (Connection connection = this.getConnection()) {
			Pojo newPojo = new Pojo();
			newPojo.setVersion(10564337);
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
				.where(Where.compare(Pojo::getVersion, Operator.EQ, newPojo.getVersion()))
				.joinAll()
				.execute(connection, Select.Strategy.EXISTS);
			Assert.assertEquals(1, found.size());

			found = Select
				.from(Pojo.class)
				.where(Where.compare(Pojo::getVersion, Operator.EQ, newPojo.getVersion() + 1))
				.joinAll()
				.execute(connection, Select.Strategy.EXISTS);
			Assert.assertEquals(0, found.size());

			found = Select
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
				Assert.assertEquals(0, results.getResultSet().getInt(1));
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
		try (Connection connection = this.getConnection()) {
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
				Assert.assertEquals(0, results.getResultSet().getInt(1));
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
		try (Connection connection = this.getConnection()) {
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
					.join(Join.to(Jopo::getPojo).join(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo)
						.join(JoinSet.to(Pojo::getJopos)
							.join(Join.to(Jopo::getPojo).join(JoinSet.to(Pojo::getJopos))))
					)))
				)
				.execute(connection);
			Assert.assertEquals(1, found.size());
			Assert.assertEquals(pojo, found.iterator().next());
		}
	}

	@Test
	public void testSelectToDelete() throws SQLException, ClassNotFoundException {
		try (Connection connection = this.getConnection()) {
			Pojo newPojo = new Pojo();
			newPojo.setVersion(1337);
			newPojo.setType(Pojo.Type.FOO);

			Upsert
				.from(Pojo.class)
				.onto(newPojo)
				.checkNaturalID()
				.execute(connection);

			Select<Pojo> select = Select.from(Pojo.class).where(Where.naturalId(newPojo));
			Set<Pojo> found = select.execute(connection, Select.Strategy.EXISTS);
			Assert.assertEquals(1, found.size());

			Delete<Pojo> delete = select.toDelete();
			delete.executeQuery(connection);

			found = select.execute(connection, Select.Strategy.EXISTS);
			Assert.assertEquals(0, found.size());
		}
	}

	@Test
	public void testIdMap() throws SQLException, ClassNotFoundException {
		try (Connection connection = this.getConnection()) {
				Pojo newPojo = new Pojo();
				newPojo.setVersion(1337);
				newPojo.setType(Pojo.Type.FOO);
				Jopo jopo = new Jopo();
				jopo.setName("jopo From code !");
				jopo.setPojo(newPojo);
				newPojo.getJopos().add(jopo);

				jopo = new Jopo();
				jopo.setName("jopo From code #2 !");
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

				Select<Pojo> select = Select.from(Pojo.class).where(Where.naturalId(newPojo)).joinAll();
				Set<Pojo> found = select.execute(connection, Select.Strategy.EXISTS);
				Assert.assertEquals(1, found.size());

				IdMap idMap = select.executeForIds(connection);
				logger.debug("IdMap is : \n[{}]", idMap);
				Assert.assertEquals(idMap.getIdsForClass(Pojo.class),  Sets.newHashSet(1L));
				Assert.assertEquals(idMap.getIdsForClass(Other.class), Sets.newHashSet(1L));
				Assert.assertEquals(idMap.getIdsForClass(Jopo.class),  Sets.newHashSet(1L, 2L));

				select.toDelete().executeQueries(connection);

				found = select.execute(connection, Select.Strategy.EXISTS);
				Assert.assertEquals(0, found.size());
			}
		}
}
