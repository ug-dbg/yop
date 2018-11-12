package org.yop.orm.simple;

import org.junit.Assert;
import org.junit.Test;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.query.Select;
import org.yop.orm.query.Where;
import org.yop.orm.simple.model.withschema.*;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;
import org.yop.orm.sql.SimpleQuery;
import org.yop.orm.sql.adapter.IConnection;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import static org.yop.orm.Yop.*;
import static org.yop.orm.Yop.select;
import static org.yop.orm.Yop.to;

/**
 * Tests from {@link SimpleTest} but with models where the schema is set.
 */
public class SimpleWithSchemaTest extends DBMSSwitch {

	@Override
	protected String getPackagePrefixes() {
		return "org.yop.orm.simple.model.withschema";
	}

	@Override
	protected boolean check() {
		// There is no 'schema' in SQLite. The test is pointless and fails.
		return super.check() && !"sqlite".equals(dbms());
	}

	@Test
	public void testCRUD() throws SQLException, ClassNotFoundException {
	try (IConnection connection = this.getConnection()) {
		Pojo newPojo = new org.yop.orm.simple.model.withschema.Pojo();
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
			.where(Where.compare(Pojo::getVersion, Operator.EQ, newPojo.getVersion()))
			.joinAll()
			.join(toSet(Pojo::getOthers).join(
				to(Other::getExtra).join(to(Extra::getOther))
			))
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
			.join(toSet(Pojo::getOthers).join(
				to(Other::getExtra).join(to(Extra::getSuperExtra))
			))
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
				"SELECT COUNT(*) FROM yop.POJO_JOPO_relation",
				Query.Type.SELECT,
				new Parameters(),
				connection.config()),
			action
		);
		}
	}
}
