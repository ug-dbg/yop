package org.yop.orm.simple;

import org.junit.Assert;
import org.junit.Test;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.query.Delete;
import org.yop.orm.query.Select;
import org.yop.orm.query.Upsert;
import org.yop.orm.query.Where;
import org.yop.orm.simple.model.Jopo;
import org.yop.orm.simple.model.Pojo;
import org.yop.orm.sql.adapter.IConnection;

import java.sql.SQLException;

/**
 * Test class for {@link org.yop.orm.evaluation.NaturalKey} and {@link Upsert#checkNaturalID} features.
 */
public class NaturalIdTest extends DBMSSwitch {

	@Override
	protected String getPackagePrefixes() {
		return "org.yop.orm.simple.model";
	}

	@Test
	public void testUpsertNaturalID() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo();
			pojo.setVersion(10564337);
			pojo.setType(Pojo.Type.FOO);
			pojo.setActive(true);

			Upsert.from(Pojo.class).onto(pojo).execute(connection);
			Assert.assertEquals(Pojo.Type.FOO, Select.from(Pojo.class).uniqueResult(connection).getType());

			pojo.setActive(false);
			Upsert.from(Pojo.class).onto(pojo).execute(connection);
			Assert.assertEquals(Pojo.Type.FOO, Select.from(Pojo.class).uniqueResult(connection).getType());
			Assert.assertEquals(false, Select.from(Pojo.class).uniqueResult(connection).isActive());
		}
	}

	@Test
	public void testSelectNaturalIDNullColumn() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo();
			pojo.setVersion(10564337);
			pojo.setType(Pojo.Type.FOO);
			pojo.setActive(true);

			Upsert.from(Pojo.class).onto(pojo).execute(connection);
			Assert.assertEquals(Pojo.Type.FOO, Select.from(Pojo.class).uniqueResult(connection).getType());

			pojo.setVersion(null);
			Upsert.from(Pojo.class).onto(pojo).execute(connection);

			Pojo result = Select.from(Pojo.class).where(Where.naturalId(pojo)).uniqueResult(connection);
			Assert.assertNull(result.getVersion());
			Assert.assertEquals(pojo, result);
		}
	}

	@Test(expected = YopSQLException.class)
	public void testUpsertNaturalIDAlreadyExist() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo();
			pojo.setVersion(10564337);
			pojo.setType(Pojo.Type.FOO);
			pojo.setActive(true);

			Upsert.from(Pojo.class).onto(pojo).execute(connection);
			Assert.assertEquals(Pojo.Type.FOO, Select.from(Pojo.class).uniqueResult(connection).getType());

			Pojo newPojo = new Pojo();
			newPojo.setVersion(10564337);
			newPojo.setType(Pojo.Type.BAR);
			newPojo.setActive(true);

			Upsert.from(Pojo.class).onto(newPojo).execute(connection);
		}
	}

	@Test
	public void testUpsertCheckNaturalID() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo();
			pojo.setVersion(10564337);
			pojo.setType(Pojo.Type.FOO);
			pojo.setActive(true);

			Upsert.from(Pojo.class).onto(pojo).execute(connection);
			Assert.assertEquals(Pojo.Type.FOO, Select.from(Pojo.class).uniqueResult(connection).getType());

			Pojo newPojo = new Pojo();
			newPojo.setVersion(10564337);
			newPojo.setType(Pojo.Type.BAR);
			newPojo.setActive(true);

			Upsert.from(Pojo.class).checkNaturalID().onto(newPojo).execute(connection);
		}
	}

	@Test
	public void testUpsertCheckNaturalIDWithJoin() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo();
			pojo.setVersion(10564337);
			pojo.setType(Pojo.Type.FOO);
			pojo.setActive(true);

			Jopo jopo = new Jopo();
			jopo.setName("Jopo !");
			jopo.setPojo(pojo);

			Upsert.from(Jopo.class).onto(jopo).join(Jopo::getPojo).execute(connection);
			Assert.assertEquals(Pojo.Type.FOO, Select.from(Pojo.class).uniqueResult(connection).getType());

			Pojo newPojo = new Pojo();
			newPojo.setVersion(10564337);
			newPojo.setType(Pojo.Type.BAR);
			newPojo.setActive(true);
			jopo.setPojo(newPojo);

			Upsert.from(Jopo.class).checkNaturalID().onto(jopo).join(Jopo::getPojo).execute(connection);
		}
	}

	@Test(expected = YopSQLException.class)
	public void testUpsertCheckNaturalIDWithJoinAlreadyExist() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo();
			pojo.setVersion(10564337);
			pojo.setType(Pojo.Type.FOO);
			pojo.setActive(true);

			Jopo jopo = new Jopo();
			jopo.setName("Jopo !");
			jopo.setPojo(pojo);

			Upsert.from(Jopo.class).onto(jopo).join(Jopo::getPojo).execute(connection);
			Assert.assertEquals(Pojo.Type.FOO, Select.from(Pojo.class).uniqueResult(connection).getType());

			Pojo newPojo = new Pojo();
			newPojo.setVersion(10564337);
			newPojo.setType(Pojo.Type.BAR);
			newPojo.setActive(true);
			jopo.setPojo(newPojo);

			Upsert.from(Jopo.class).onto(jopo).join(Jopo::getPojo).execute(connection);
		}
	}

	@Test
	public void testDeleteNaturalID() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo();
			pojo.setVersion(10564337);
			pojo.setType(Pojo.Type.FOO);
			pojo.setActive(true);

			Upsert.from(Pojo.class).onto(pojo).execute(connection);
			Assert.assertEquals(Pojo.Type.FOO, Select.from(Pojo.class).uniqueResult(connection).getType());

			pojo.setVersion(null);
			Upsert.from(Pojo.class).onto(pojo).execute(connection);

			Delete.from(Pojo.class).where(Where.naturalId(pojo)).executeQueries(connection);
			Pojo result = Select.from(Pojo.class).where(Where.naturalId(pojo)).uniqueResult(connection);
			Assert.assertNull(result);
		}
	}
}
