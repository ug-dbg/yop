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
import org.yop.orm.exception.YopInvalidJoinException;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.map.IdMap;
import org.yop.orm.query.join.IJoin;
import org.yop.orm.query.join.Join;
import org.yop.orm.query.sql.*;
import org.yop.orm.query.batch.BatchUpsert;
import org.yop.orm.simple.model.*;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Query;
import org.yop.orm.sql.SimpleQuery;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.JoinUtil;
import org.yop.reflection.Reflection;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.yop.orm.Yop.*;

/**
 * Simple test with simple objects for simple CRUD.
 * I should find a more explicit name. Sorry about that.
 */
public class SimpleTest extends DBMSSwitch {

	private static final Logger logger = LoggerFactory.getLogger(SimpleTest.class);

	@Override
	protected String getPackageNames() {
		return "org.yop.orm.simple.model";
	}

	@Test
	public void testPrintJoins() {
		IJoin.Joins<Pojo> joins = new IJoin.Joins<>();
		JoinUtil.joinAll(Pojo.class, joins);
		joins.add(SQLJoin.to(Pojo::getParent));
		joins.add(Join.toN(Pojo::getChildren));
		joins.print(Pojo.class);
		joins.print(Pojo.class, logger::info);

		joins = new IJoin.Joins<>();
		JoinUtil.joinProfiles(Pojo.class, joins, "pojo_profile1", "pojo_children_and_parent");
		joins.print(Pojo.class);
	}

	@Test
	public void testJoinNoProfile() {
		List<String> profiles = JoinUtil.joinProfiles(Reflection.get(Pojo.class, "children"));
		Assert.assertEquals(1, profiles.size());

		IJoin.Joins<Pojo> joins = new IJoin.Joins<>();
		JoinUtil.joinProfiles(Pojo.class, joins);
		Assert.assertEquals(2, joins.size());

		joins = new IJoin.Joins<>();
		JoinUtil.joinProfiles(Pojo.class, joins, "this profile does not exist");
		Assert.assertEquals(0, joins.size());

		Select<Pojo> select = Select.from(Pojo.class).joinProfiles();
		Assert.assertEquals(0, ((Collection)Reflection.readField("joins", select)).size());
	}

	@Test
	public void testProfileJoins() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo();
			pojo.setVersion(1);
			pojo.setType(Pojo.Type.FOO);
			pojo.setActive(true);

			Pojo parent = new Pojo();
			parent.setVersion(0);
			parent.setType(Pojo.Type.FOO);
			parent.setActive(true);
			pojo.setParent(parent);

			for (int i = 1; i <= 10; i++) {
				Pojo child = new Pojo();
				child.setVersion(pojo.getVersion() + i);
				child.setType(Pojo.Type.BAR);
				child.setActive(true);
				pojo.getChildren().add(child);
			}

			Upsert.from(Pojo.class).onto(pojo).joinProfiles("pojo_children_and_parent").execute(connection);

			Pojo fromDB = Select
				.from(Pojo.class)
				.where(Pojo::getVersion, Operator.EQ, pojo.getVersion())
				.joinProfiles("pojo_children_and_parent")
				.uniqueResult(connection);
			Assert.assertEquals(pojo, fromDB);
			Assert.assertEquals(10, fromDB.getChildren().size());
			Assert.assertEquals(new HashSet<>(pojo.getChildren()), new HashSet<>(fromDB.getChildren()));
			Assert.assertNotNull(pojo.getParent());
			Assert.assertEquals(pojo.getParent(), fromDB.getParent());

			Hydrate
				.from(Pojo.class)
				.onto(fromDB)
				.join(SQLJoin.to(Pojo::getParent))
				.join(Join.toN(Pojo::getChildren))
				.recurse()
				.execute(connection);
			Assert.assertTrue(fromDB.getParent().getChildren().get(0) == fromDB);
			Assert.assertEquals(10, fromDB.getChildren().size());
			Assert.assertTrue(fromDB.getChildren().get(0).getParent() == fromDB);
		}
	}

	@Test
	public void testJoinsMultipleBranches() throws SQLException, ClassNotFoundException {
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
				.join(toN(Pojo::getJopos))
				.join(toN(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getOther))))
				.join(toN(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getSuperExtra))))
				.checkNaturalID()
				.execute(connection);

			Collection<Pojo> fromSelectWithBadJoinWhere = Select
				.from(Pojo.class)
				.join(toN(Pojo::getJopos).where(new Comparison(Jopo::getId, Operator.GE, 2L)))
				.join(toN(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getOther))))
				.join(toN(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getSuperExtra))))
				.execute(connection, Select.Strategy.EXISTS);
			Assert.assertEquals(0, fromSelectWithBadJoinWhere.size());

			fromSelectWithBadJoinWhere = Select
				.from(Pojo.class)
				.join(toN(Pojo::getJopos).where(new Comparison(Jopo::getId, Operator.GE, 2L)))
				.join(toN(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getOther))))
				.join(toN(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getSuperExtra))))
				.execute(connection, Select.Strategy.IN);
			Assert.assertEquals(0, fromSelectWithBadJoinWhere.size());

			Pojo found = Select
				.from(Pojo.class)
				.join(toN(Pojo::getJopos).where(new Comparison(Jopo::getId, Operator.GE, 1L)))
				.join(toN(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getOther))))
				.join(toN(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getSuperExtra))))
				.uniqueResult(connection);
			Assert.assertTrue(!found.getOthers().isEmpty());
			Assert.assertTrue(found.getOthers().iterator().next().getExtra().getOther() != null);
			Assert.assertTrue(found.getOthers().iterator().next().getExtra().getSuperExtra() != null);

			found = Select
					.from(Pojo.class)
					.join(Pojo::getJopos)
					.join(Pojo::getOthers, Other::getExtra, Extra::getOther)
					.join(Pojo::getOthers, Other::getExtra, Extra::getSuperExtra)
					.uniqueResult(connection);
			Assert.assertTrue(!found.getOthers().isEmpty());
			Assert.assertTrue(found.getOthers().iterator().next().getExtra().getOther() != null);
			Assert.assertTrue(found.getOthers().iterator().next().getExtra().getSuperExtra() != null);
		}
	}

	@Test (expected = YopInvalidJoinException.class)
	public void testJoinsInvalidLambdaPath() {
		Select
			.from(Pojo.class)
			.join(Pojo::getJopos)
			.join(Pojo::getOthers, Other::getExtra, Extra::getOther)
			.join(Pojo::getOthers, Other::getExtra, String::length)
			.uniqueResult(null);
	}

	@Test (expected = YopInvalidJoinException.class)
	public void testJoinsInvalidTargetOfLambdaPath() {
		Select
			.from(Pojo.class)
			.join(Pojo::getJopos)
			.join(Pojo::getOthers, Other::getExtra, Extra::getOther)
			.join(Pojo::getOthers, Other::getExtra, Extra::getStyle)
			.uniqueResult(null);
	}

	@Test
	public void testPaging() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Collection<Pojo> pojos = new ArrayList<>();
			for (int i = 0; i < 90; i++) {
				Pojo newPojo = new Pojo();
				newPojo.setVersion(i);
				newPojo.setType(Pojo.Type.FOO);
				newPojo.setActive(true);

				Jopo jopo1 = new Jopo();
				jopo1.setName("test limit 1 Pojo#" + i);
				jopo1.setPojo(newPojo);
				newPojo.getJopos().add(jopo1);
				Jopo jopo2 = new Jopo();
				jopo2.setName("test limit 2 Pojo#" + i);
				jopo2.setPojo(newPojo);
				newPojo.getJopos().add(jopo2);

				pojos.add(newPojo);
			}
			upsert(Pojo.class).onto(pojos).join(toN(Pojo::getJopos)).execute(connection);

			Select<Pojo> selectWithPaging = Select.from(Pojo.class).joinAll().page(87L, 10L);
			Set<Pojo> out = selectWithPaging.executeWithTwoQueries(connection);
			Assert.assertEquals(3, out.size());

			out = selectWithPaging.execute(connection, Select.Strategy.IN);
			Assert.assertEquals(3, out.size());

			// Yop does not support paging with 'EXISTS' → there is a fallback to 'IN'
			out = selectWithPaging.execute(connection, Select.Strategy.IN);
			Assert.assertEquals(3, out.size());

			String selectWithPagingAsJSON = selectWithPaging.toJSON().toString();
			selectWithPaging = Select.fromJSON(selectWithPagingAsJSON, connection.config());

			out = selectWithPaging.executeWithTwoQueries(connection);
			Assert.assertEquals(3, out.size());

			out = selectWithPaging.page(91L, 10L).executeWithTwoQueries(connection);
			Assert.assertEquals(0, out.size());

			Select.from(Pojo.class).join(Pojo::getOthers, Other::getExtra, Extra::getSuperExtra);
		}
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
				.join(toN(Pojo::getJopos))
				.join(toN(Pojo::getOthers))
				.checkNaturalID()
				.execute(connection);

			Path<Pojo, String> jopoName = Path.pathSet(Pojo::getJopos).to(Jopo::getName);
			Set<Pojo> matches = select(Pojo.class)
				.join(toN(Pojo::getJopos))
				.join(toN(Pojo::getOthers).where(Where.compare(Other::getName, Operator.EQ, jopoName)))
				.execute(connection);
			Assert.assertEquals(1, matches.size());

			matches = select(Pojo.class)
				.join(toN(Pojo::getJopos))
				.join(toN(Pojo::getOthers).where(Where.compare(Other::getName, Operator.NE, jopoName)))
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
				.join(toN(Pojo::getOthers).where(Where.compare(Other::getName, Operator.EQ, jopoName)))
				.execute(connection);
		}
	}

	@Test
	public void testPartialUpsert() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo newPojo = new Pojo();
			newPojo.setVersion(10564337);
			newPojo.setType(Pojo.Type.FOO);
			newPojo.setActive(true);

			Upsert.from(Pojo.class).onto(newPojo).execute(connection);

			newPojo.setPassword("mypassword");
			newPojo.setaVeryLongInteger(new BigInteger("1235684541964545646"));
			Upsert.from(Pojo.class).onto(newPojo).onFields(Pojo::getPassword, Pojo::getaVeryLongInteger).execute(connection);

			Pojo fromDB = Select.from(Pojo.class).uniqueResult(connection);
			Assert.assertEquals(newPojo.getPassword(), fromDB.getPassword());
			Assert.assertEquals(newPojo.getaVeryLongInteger(), fromDB.getaVeryLongInteger());
		}
	}

	@Test
	public void testPartialBatchUpsert() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			if (! connection.config().useBatchInserts()) {
				logger.warn("Connection does not support batches, skipping test.");
				return;
			}

			List<Pojo> pojos = new ArrayList<>(10);

			for (int i = 0; i < 10; i++) {
				Pojo newPojo = new Pojo();
				newPojo.setVersion(10564337 + i);
				newPojo.setType(Pojo.Type.FOO);
				newPojo.setActive(true);
				pojos.add(newPojo);
			}

			BatchUpsert.from(Pojo.class).onto(pojos).execute(connection);

			for (Pojo pojo : pojos) {
				pojo.setPassword("mypassword");
				pojo.setaVeryLongInteger(new BigInteger("1235684541964545646"));
			}


			BatchUpsert.from(Pojo.class).onto(pojos).onFields(Pojo::getPassword, Pojo::getaVeryLongInteger).execute(connection);

			Set<Pojo> fromDB = Select.from(Pojo.class).execute(connection);
			for (Pojo pojo : fromDB) {
				Assert.assertEquals(pojo.getPassword(), "mypassword");
				Assert.assertEquals(pojo.getaVeryLongInteger(), new BigInteger("1235684541964545646"));
			}

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
				.join(Join.toN(Pojo::getJopos))
				.join(toN(Pojo::getOthers).join(to(Other::getExtra)
					.join(to(Extra::getOther))
					.join(to(Extra::getSuperExtra))
				))
				.checkNaturalID()
				.execute(connection);

			Set<Pojo> found = select(Pojo.class)
				.where(Where.compare(Pojo::getVersion, Operator.EQ, newPojo.getVersion()))
				.joinAll()
				.join(toN(Pojo::getOthers).join(to(Other::getExtra)
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
				.join(toN(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getOther))))
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
				.join(toN(Pojo::getOthers).join(to(Other::getExtra).join(to(Extra::getSuperExtra))))
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
				new SimpleQuery("SELECT COUNT(*) FROM POJO_JOPO_relation", Query.Type.SELECT, connection.config()),
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
				.join(toN(Pojo::getJopos))
				.join(toN(Pojo::getOthers).join(to(Other::getExtra)
					.join(to(Extra::getOther))
					.join(to(Extra::getSuperExtra))
				))
				.checkNaturalID()
				.execute(connection);

			Set<Pojo> found = select(Pojo.class)
				.where(Where.compare(Pojo::getVersion, Operator.EQ, newPojo.getVersion()))
				.joinAll()
				.join(toN(Pojo::getOthers).join(to(Other::getExtra)
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
				.join(toN(Pojo::getOthers).join(to(Other::getExtra)
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
				.join(to(Extra::getSuperExtra).join(toN(SuperExtra::getExtras)))
				.execute(connection);
			Assert.assertEquals(2, extras.size());
			for (Extra extraFromDB : extras) {
				Assert.assertTrue(extraFromDB.getSuperExtra().getExtras().contains(extraFromDB));
			}

			superExtra.setSize(13L);
			upsert(SuperExtra.class).onto(superExtra).execute(connection);

			extras = select(Extra.class)
				.join(to(Extra::getSuperExtra).join(toN(SuperExtra::getExtras)))
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
				new SimpleQuery("SELECT COUNT(*) FROM POJO_JOPO_relation", Query.Type.SELECT, connection.config()),
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
				.join(toN(Pojo::getJopos)
					.join(to(Jopo::getPojo).join(toN(Pojo::getJopos).join(to(Jopo::getPojo)
						.join(toN(Pojo::getJopos)
							.join(to(Jopo::getPojo).join(toN(Pojo::getJopos))))
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
				.join(toN(Pojo::getJopos))
				.join(toN(Pojo::getOthers))
				.checkNaturalID()
				.execute(connection);

			Select<Pojo> select = select(Pojo.class).whereNaturalId(newPojo).joinAll();
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
				.join(toN(Pojo::getJopos))
				.join(toN(Pojo::getOthers))
				.checkNaturalID()
				.execute(connection);

			Select<Pojo> select = select(Pojo.class).where(Where.naturalId(newPojo));
			Set<Pojo> found = select.execute(connection);
			Assert.assertEquals(1, found.size());
			Pojo fromNaturalID = found.iterator().next();

			hydrate(Pojo.class).onto(fromNaturalID).join(Pojo::getJopos).execute(connection);
			Assert.assertEquals(newPojo.getJopos(), fromNaturalID.getJopos());
			Assert.assertEquals(0, fromNaturalID.getOthers().size());

			found = select.execute(connection);
			Assert.assertEquals(1, found.size());
			fromNaturalID = found.iterator().next();

			hydrate(Pojo.class).onto(fromNaturalID).joinAll().execute(connection);
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
			hydrate(Pojo.class).onto(fromNaturalID).joinAll().join(Pojo::getParent).execute(connection);
			Assert.assertEquals(newPojo.getJopos(),  fromNaturalID.getJopos());
			Assert.assertEquals(newPojo.getOthers(), fromNaturalID.getOthers());
			Assert.assertEquals(parent, fromNaturalID.getParent());

			Pojo pojo1 = Select.from(Pojo.class).whereId(newPojo.getId()).uniqueResult(connection);
			Pojo pojo2 = Select.from(Pojo.class).whereId(parent.getId()).uniqueResult(connection);

			hydrate(Pojo.class)
				.onto(Arrays.asList(pojo1, pojo2))
				.joinAll()
				.join(Pojo::getParent)
				.join(Pojo::getChildren)
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
				.join(toN(Pojo::getJopos))
				.join(toN(Pojo::getOthers))
				.checkNaturalID()
				.execute(connection);

			Select<Pojo> select = select(Pojo.class).where(Where.naturalId(newPojo));
			Pojo fromNaturalID = select.uniqueResult(connection);

			hydrate(Pojo.class).onto(fromNaturalID).joinAll().recurse().execute(connection);
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
			hydrate(Pojo.class).onto(fromNaturalID).joinAll().join(to(Pojo::getParent)).recurse().execute(connection);
			Assert.assertEquals(newPojo.getJopos(),  fromNaturalID.getJopos());
			Assert.assertEquals(newPojo.getOthers(), fromNaturalID.getOthers());
			Assert.assertEquals(parent, fromNaturalID.getParent());

			Pojo pojo1 = select(Pojo.class).where(Where.id(newPojo.getId())).uniqueResult(connection);
			Pojo pojo2 = select(Pojo.class).where(Where.id(parent.getId())).uniqueResult(connection);

			hydrate(Pojo.class)
				.onto(Arrays.asList(pojo1, pojo2))
				.joinAll()
				.join(to(Pojo::getParent))
				.join(toN(Pojo::getChildren))
				.join(toN(Pojo::getOthers).join(toN(Other::getPojos)))
				.join(toN(Pojo::getJopos).join(to(Jopo::getPojo)))
				.recurse()
				.execute(connection);

			Assert.assertEquals(parent, pojo1.getParent());
			Assert.assertEquals(Collections.singletonList(pojo1), pojo2.getChildren());
			Assert.assertTrue(pojo1.getParent() == pojo2);
		}
	}

	@Test
	public void testRecurseParentChildren() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo();
			pojo.setVersion(1);
			pojo.setType(Pojo.Type.FOO);
			pojo.setActive(true);

			Pojo parent = new Pojo();
			parent.setVersion(0);
			parent.setType(Pojo.Type.FOO);
			parent.setActive(true);
			pojo.setParent(parent);

			for (int i = 1; i <= 10; i++) {
				Pojo child = new Pojo();
				child.setVersion(pojo.getVersion() + i);
				child.setType(Pojo.Type.BAR);
				child.setActive(true);
				pojo.getChildren().add(child);
			}

			Upsert
				.from(Pojo.class).onto(pojo)
				.join(Pojo::getParent)
				.join(Pojo::getChildren)
				.execute(connection);

			Pojo fromDB = Select
				.from(Pojo.class)
				.where(Where.compare(Pojo::getVersion, Operator.EQ, pojo.getVersion()))
				.join(Pojo::getParent)
				.join(Pojo::getChildren)
				.uniqueResult(connection);

			hydrate(Pojo.class)
				.onto(fromDB)
				.join(SQLJoin.to(Pojo::getParent))
				.join(Join.toN(Pojo::getChildren))
				.recurse()
				.execute(connection);
			Assert.assertTrue(fromDB.getParent().getChildren().get(0) == fromDB);
			Assert.assertEquals(10, fromDB.getChildren().size());
			Assert.assertTrue(fromDB.getChildren().get(0).getParent() == fromDB);
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
		pojo.setaVeryLongFloat(new BigDecimal("123.45646566568545494945448496"));

		try (IConnection connection = this.getConnection()) {
			upsert(Pojo.class).onto(pojo).execute(connection);

			Pojo pojoFromDB = select(Pojo.class).where(Where.naturalId(pojo)).uniqueResult(connection);
			Assert.assertEquals(pojo.getaVeryLongInteger(), pojoFromDB.getaVeryLongInteger());
			Assert.assertEquals(pojo.getaVeryLongFloat(), pojoFromDB.getaVeryLongFloat());
		}
	}
}
