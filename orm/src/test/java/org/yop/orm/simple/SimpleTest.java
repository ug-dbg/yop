package org.yop.orm.simple;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.evaluation.Comparison;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.evaluation.Path;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.map.IdMap;
import org.yop.orm.query.*;
import org.yop.orm.simple.model.*;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;
import org.yop.orm.sql.SimpleQuery;
import org.yop.orm.sql.adapter.IConnection;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.yop.orm.Yop.*;

/**
 * Simple test with simple objects for simple CRUD.
 * I should find a more explicit name. Sorry about that.
 */
public class SimpleTest extends DBMSSwitch {

	private static final Logger logger = LoggerFactory.getLogger(SimpleTest.class);

	@Override
	protected String getPackagePrefixes() {
		return "org.yop.orm.simple.model";
	}

	@Test
	public void testPathRef() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo newPojo = new Pojo();
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

			Path<Pojo, String> jopoName = Path.pathSet(Pojo::getJopos).to(Jopo::getName);
			Set<Pojo> matches = select(Pojo.class)
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers).where(Where.compare(Other::getName, Operator.EQ, jopoName)))
				.execute(connection);
			Assert.assertEquals(1, matches.size());

			matches = select(Pojo.class)
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers).where(Where.compare(Other::getName, Operator.NE, jopoName)))
				.execute(connection);
			Assert.assertEquals(0, matches.size());
		}
	}

	@Test(expected = YopSQLException.class)
	public void testPathRef_undeclared_join() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			// Path to Jopo does not match a declared join !
			Path<Pojo, String> jopoName = Path.pathSet(Pojo::getJopos).to(Jopo::getName);
			select(Pojo.class)
				.join(toSet(Pojo::getOthers).where(Where.compare(Other::getName, Operator.EQ, jopoName)))
				.execute(connection);
		}
	}

	@Test
	public void testCRUD() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo newPojo = new Pojo();
			newPojo.setVersion(10564337);
			newPojo.setType(Pojo.Type.FOO);
			newPojo.setActive(true);
			Jopo jopo = new Jopo();
			jopo.setName("jopo From code !");
			jopo.setPojo(newPojo);
			newPojo.getJopos().add(jopo);

			Other other = new Other();
			other.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
			other.setName("other name :)");
			newPojo.getOthers().add(other);

			Extra extra = new Extra();
			extra.setStyle("rad");
			extra.setUserName("roger");
			extra.setOther(other);
			other.setExtra(extra);

			SuperExtra superExtra = new SuperExtra();
			superExtra.setSize(123456789L);
			extra.setSuperExtra(superExtra);

			upsert(Pojo.class)
				.onto(newPojo)
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers).join(to(Other::getExtra)
					.join(to(Extra::getOther))
					.join(to(Extra::getSuperExtra))
				))
				.checkNaturalID()
				.execute(connection);

			Set<Pojo> found = select(Pojo.class)
				.where(Where.compare(Pojo::getVersion, Operator.EQ, newPojo.getVersion()))
				.joinAll()
				.join(toSet(Pojo::getOthers).join(to(Other::getExtra)
					.join(to(Extra::getOther))
					.join(to(Extra::getSuperExtra))
				))
				.execute(connection, Select.Strategy.EXISTS);
			Assert.assertEquals(1, found.size());
			Pojo foundPojo = found.iterator().next();
			Other foundOther = foundPojo.getOthers().iterator().next();
			Assert.assertTrue(foundOther == foundOther.getExtra().getOther());
			Assert.assertEquals(extra, foundOther.getExtra());
			Assert.assertTrue(superExtra.acceptable(foundOther.getExtra().getSuperExtra()));

			Set<Pojo> foundWith2Queries = select(Pojo.class)
				.where(new Comparison(Pojo::getVersion, Operator.EQ, newPojo.getVersion()))
				.joinAll()
				.join(toSet(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getOther))))
				.executeWithTwoQueries(connection);
			Assert.assertEquals(found, foundWith2Queries);
			foundPojo = foundWith2Queries.iterator().next();
			foundOther = foundPojo.getOthers().iterator().next();
			Assert.assertTrue(foundOther == foundOther.getExtra().getOther());

			found = select(Pojo.class)
				.where(Where.compare(Pojo::getVersion, Operator.EQ, newPojo.getVersion() + 1))
				.joinAll()
				.execute(connection, Select.Strategy.EXISTS);
			Assert.assertEquals(0, found.size());

			found = select(Pojo.class)
				.where(Where.compare(Pojo::isActive, Operator.EQ, true))
				.joinAll()
				.execute(connection, Select.Strategy.EXISTS);
			Assert.assertEquals(1, found.size());

			found = select(Pojo.class)
				.where(Where.compare(Pojo::isActive, Operator.EQ, false))
				.joinAll()
				.execute(connection, Select.Strategy.EXISTS);
			Assert.assertEquals(0, found.size());

			found = select(Pojo.class)
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

			delete(Pojo.class)
				.join(toSet(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getSuperExtra))))
				.executeQueries(connection);

			Set<Pojo> afterDelete = select(Pojo.class).joinAll().execute(connection);
			Assert.assertEquals(0, afterDelete.size());

			Set<Extra> extras = select(Extra.class).execute(connection);
			Assert.assertEquals(0, extras.size());

			Set<SuperExtra> superExtras = select(SuperExtra.class).execute(connection);
			Assert.assertEquals(0, superExtras.size());

			// Assertion that the relation was cleaned in the association table.
			Executor.Action<String> action = results -> {
				results.getCursor().next();
				Assert.assertEquals(0, results.getCursor().getLong(1).longValue());
				return "";
			};

			Executor.executeQuery(
				connection,
				new SimpleQuery(
					"SELECT COUNT(*) FROM POJO_JOPO_relation",
					Query.Type.SELECT,
					new Parameters(),
					connection.config()
				),
				action
			);
		}
	}

	@Test
	public void testSelectStrategies() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo newPojo = new Pojo();
			newPojo.setVersion(10564337);
			newPojo.setType(Pojo.Type.FOO);
			newPojo.setActive(true);
			Jopo jopo = new Jopo();
			jopo.setName("jopo From code !");
			jopo.setPojo(newPojo);
			newPojo.getJopos().add(jopo);

			Other other = new Other();
			other.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
			other.setName("other name :)");
			newPojo.getOthers().add(other);

			Extra extra = new Extra();
			extra.setStyle("rad");
			extra.setUserName("roger");
			extra.setOther(other);
			other.setExtra(extra);

			SuperExtra superExtra = new SuperExtra();
			superExtra.setSize(123456789L);
			extra.setSuperExtra(superExtra);

			upsert(Pojo.class)
				.onto(newPojo)
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers).join(to(Other::getExtra)
					.join(to(Extra::getOther))
					.join(to(Extra::getSuperExtra))
				))
				.checkNaturalID()
				.execute(connection);

			Set<Pojo> found = select(Pojo.class)
				.where(Where.compare(Pojo::getVersion, Operator.EQ, newPojo.getVersion()))
				.joinAll()
				.join(toSet(Pojo::getOthers).join(to(Other::getExtra)
					.join(to(Extra::getOther))
					.join(to(Extra::getSuperExtra))
				))
				.execute(connection, Select.Strategy.EXISTS);
			Assert.assertEquals(1, found.size());
			Pojo foundPojo = found.iterator().next();
			Other foundOther = foundPojo.getOthers().iterator().next();
			Assert.assertTrue(foundOther == foundOther.getExtra().getOther());
			Assert.assertEquals(extra, foundOther.getExtra());
			Assert.assertTrue(superExtra.acceptable(foundOther.getExtra().getSuperExtra()));

			Set<Pojo> foundWithIN = select(Pojo.class)
				.where(Where.compare(Pojo::getVersion, Operator.EQ, newPojo.getVersion()))
				.joinAll()
				.join(toSet(Pojo::getOthers).join(to(Other::getExtra)
					.join(to(Extra::getOther))
					.join(to(Extra::getSuperExtra))
				))
				.execute(connection, Select.Strategy.IN);
			Assert.assertEquals(1, found.size());
			Assert.assertEquals(found, foundWithIN);
			Pojo foundPojoWithIN = found.iterator().next();
			Other foundOtherWithIN = foundPojoWithIN.getOthers().iterator().next();
			Assert.assertTrue(foundOther == foundOtherWithIN.getExtra().getOther());
			Assert.assertEquals(extra, foundOtherWithIN.getExtra());
			Assert.assertTrue(superExtra.acceptable(foundOtherWithIN.getExtra().getSuperExtra()));
		}
	}

	@Test
	public void testJoinColumn() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			SuperExtra superExtra = new SuperExtra();
			superExtra.setSize(11L);

			Extra extra = new Extra();
			extra.setStyle("rough");
			extra.setUserName("Raoul");
			superExtra.getExtras().add(extra);

			extra = new Extra();
			extra.setStyle("sharp");
			extra.setUserName("Léon");
			superExtra.getExtras().add(extra);

			upsert(SuperExtra.class).onto(superExtra).joinAll().execute(connection);

			Set<Extra> extras = select(Extra.class)
				.join(to(Extra::getSuperExtra).join(toSet(SuperExtra::getExtras)))
				.execute(connection);
			Assert.assertEquals(2, extras.size());
			for (Extra extraFromDB : extras) {
				Assert.assertTrue(extraFromDB.getSuperExtra().getExtras().contains(extraFromDB));
			}

			superExtra.setSize(13L);
			upsert(SuperExtra.class).onto(superExtra).execute(connection);

			extras = select(Extra.class)
				.join(to(Extra::getSuperExtra).join(toSet(SuperExtra::getExtras)))
				.execute(connection);
			for (Extra extraFromDB : extras) {
				Assert.assertTrue(superExtra.acceptable(extraFromDB.getSuperExtra()));
			}
		}
	}

	@Test
	public void testLotOfChildren() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo();
			pojo.setVersion(1337);
			pojo.setType(Pojo.Type.FOO);

			for(int i = 0; i < 50; i++) {
				Jopo jopo = new Jopo();
				jopo.setName("jopo [" + i + "]");
				jopo.setPojo(pojo);
				pojo.getJopos().add(jopo);
			}

			upsert(Pojo.class).joinAll().onto(pojo).execute(connection);

			Set<Jopo> jopos = select(Jopo.class).join(to(Jopo::getPojo)).execute(connection);
			Assert.assertEquals(50, jopos.size());

			for (Jopo jopo : jopos) {
				Assert.assertEquals(pojo, jopo.getPojo());
			}

			delete(Jopo.class).executeQuery(connection);
			jopos = select(Jopo.class).join(to(Jopo::getPojo)).execute(connection);
			Assert.assertEquals(0, jopos.size());

			// Assertion that the relation was cleaned in the association table.
			Executor.Action<String> action = results -> {
				results.getCursor().next();
				Assert.assertEquals(0, results.getCursor().getLong(1).longValue());
				return "";
			};

			Executor.executeQuery(
				connection,
				new SimpleQuery(
					"SELECT COUNT(*) FROM POJO_JOPO_relation",
					Query.Type.SELECT,
					new Parameters(),
					connection.config()
				),
				action
			);
		}
	}

	@Test
	public void testSafeAlias() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo();
			pojo.setVersion(1337);
			pojo.setType(Pojo.Type.FOO);

			for (int i = 0; i < 3; i++) {
				Jopo jopo = new Jopo();
				jopo.setName("jopo [" + i + "]");
				jopo.setPojo(pojo);
				pojo.getJopos().add(jopo);
			}

			upsert(Pojo.class).joinAll().onto(pojo).execute(connection);

			// Stupid joins that should make some column/table aliases length > 64 characters
			Set<Pojo> found = select(Pojo.class)
				.join(toSet(Pojo::getJopos)
					.join(to(Jopo::getPojo).join(toSet(Pojo::getJopos).join(to(Jopo::getPojo)
						.join(toSet(Pojo::getJopos)
							.join(to(Jopo::getPojo).join(toSet(Pojo::getJopos))))
					)))
				)
				.execute(connection);
			Assert.assertEquals(1, found.size());
			Assert.assertEquals(pojo, found.iterator().next());
		}
	}

	@Test
	public void testSelectToDelete() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo newPojo = new Pojo();
			newPojo.setVersion(1337);
			newPojo.setType(Pojo.Type.FOO);

			upsert(Pojo.class)
				.onto(newPojo)
				.checkNaturalID()
				.execute(connection);

			Select<Pojo> select = select(Pojo.class).where(Where.naturalId(newPojo));
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
		try (IConnection connection = this.getConnection()) {
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

			upsert(Pojo.class)
				.onto(newPojo)
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers))
				.checkNaturalID()
				.execute(connection);

			Select<Pojo> select = select(Pojo.class).where(Where.naturalId(newPojo)).joinAll();
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

	@Test
	public void testHydrate() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
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

			upsert(Pojo.class)
				.onto(newPojo)
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers))
				.checkNaturalID()
				.execute(connection);

			Select<Pojo> select = select(Pojo.class).where(Where.naturalId(newPojo));
			Set<Pojo> found = select.execute(connection);
			Assert.assertEquals(1, found.size());
			Pojo fromNaturalID = found.iterator().next();

			hydrate(Pojo.class).onto(fromNaturalID).fetchSet(Pojo::getJopos).execute(connection);
			Assert.assertEquals(newPojo.getJopos(), fromNaturalID.getJopos());
			Assert.assertEquals(0, fromNaturalID.getOthers().size());

			found = select.execute(connection);
			Assert.assertEquals(1, found.size());
			fromNaturalID = found.iterator().next();

			hydrate(Pojo.class).onto(fromNaturalID).fetchAll().execute(connection);
			Assert.assertEquals(newPojo.getJopos(),  fromNaturalID.getJopos());
			Assert.assertEquals(newPojo.getOthers(), fromNaturalID.getOthers());

			Pojo parent = new Pojo();
			parent.setActive(false);
			parent.setType(Pojo.Type.BAR);
			parent.setVersion(0);
			newPojo.setParent(parent);

			upsert(Pojo.class)
				.onto(newPojo)
				.join(to(Pojo::getParent))
				.checkNaturalID()
				.execute(connection);

			found = select.execute(connection);
			Assert.assertEquals(1, found.size());
			fromNaturalID = found.iterator().next();
			hydrate(Pojo.class).onto(fromNaturalID).fetchAll().fetch(Pojo::getParent).execute(connection);
			Assert.assertEquals(newPojo.getJopos(),  fromNaturalID.getJopos());
			Assert.assertEquals(newPojo.getOthers(), fromNaturalID.getOthers());
			Assert.assertEquals(parent, fromNaturalID.getParent());

			Pojo pojo1 = new Pojo();
			pojo1.setId(newPojo.getId());
			pojo1.setVersion(newPojo.getVersion());

			Pojo pojo2 = new Pojo();
			pojo2.setId(parent.getId());

			hydrate(Pojo.class)
				.onto(Arrays.asList(pojo1, pojo2))
				.fetchAll()
				.fetch(Pojo::getParent)
				.fetchSet(Pojo::getChildren)
				.execute(connection);

			Assert.assertEquals(parent, pojo1.getParent());
			Assert.assertEquals(Collections.singletonList(pojo1), pojo2.getChildren());
		}
	}

	@Test
	public void testRecurse() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
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

			upsert(Pojo.class)
				.onto(newPojo)
				.join(toSet(Pojo::getJopos))
				.join(toSet(Pojo::getOthers))
				.checkNaturalID()
				.execute(connection);

			Select<Pojo> select = select(Pojo.class).where(Where.naturalId(newPojo));
			Pojo fromNaturalID = select.uniqueResult(connection);

			recurse(Pojo.class).onto(fromNaturalID).joinAll().execute(connection);
			Assert.assertEquals(newPojo.getJopos(), fromNaturalID.getJopos());
			Assert.assertEquals(1, fromNaturalID.getOthers().size());

			Pojo parent = new Pojo();
			parent.setActive(false);
			parent.setType(Pojo.Type.BAR);
			parent.setVersion(0);
			newPojo.setParent(parent);

			upsert(Pojo.class)
				.onto(newPojo)
				.join(to(Pojo::getParent))
				.checkNaturalID()
				.execute(connection);

			fromNaturalID = select.uniqueResult(connection);
			recurse(Pojo.class).onto(fromNaturalID).joinAll().join(to(Pojo::getParent)).execute(connection);
			Assert.assertEquals(newPojo.getJopos(),  fromNaturalID.getJopos());
			Assert.assertEquals(newPojo.getOthers(), fromNaturalID.getOthers());
			Assert.assertEquals(parent, fromNaturalID.getParent());

			Pojo pojo1 = select(Pojo.class).where(Where.id(newPojo.getId())).uniqueResult(this.getConnection());
			Pojo pojo2 = select(Pojo.class).where(Where.id(parent.getId())).uniqueResult(this.getConnection());

			recurse(Pojo.class)
				.onto(Arrays.asList(pojo1, pojo2))
				.joinAll()
				.join(to(Pojo::getParent))
				.join(toSet(Pojo::getChildren))
				.join(toSet(Pojo::getOthers).join(toSet(Other::getPojos)))
				.join(toSet(Pojo::getJopos).join(to(Jopo::getPojo)))
				.execute(connection);

			Assert.assertEquals(parent, pojo1.getParent());
			Assert.assertEquals(Collections.singletonList(pojo1), pojo2.getChildren());
			Assert.assertTrue(pojo1.getParent() == pojo2);
		}
	}

	@Test
	public void testArbitraryPrecisionFields() throws SQLException, ClassNotFoundException {
		Pojo pojo = new Pojo();
		pojo.setVersion(1337);
		pojo.setActive(true);
		pojo.setType(Pojo.Type.FOO);

		// BigInteger does not work very well with MS-SQL when precision > 20.
		// I don't really want to know why. Sue me.
		pojo.setaVeryLongInteger(new BigInteger("1234567890123456780"));
		pojo.setaVeryLongFloat(new BigDecimal("123.456465665685454949454484964654"));

		upsert(Pojo.class).onto(pojo).execute(this.getConnection());

		Pojo pojoFromDB = select(Pojo.class).where(Where.naturalId(pojo)).uniqueResult(this.getConnection());
		Assert.assertEquals(pojo.getaVeryLongInteger(), pojoFromDB.getaVeryLongInteger());
		Assert.assertEquals(pojo.getaVeryLongFloat(),   pojoFromDB.getaVeryLongFloat());
	}
}
