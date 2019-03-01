package org.yop.orm.simple;

import org.junit.Assert;
import org.junit.Test;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.query.*;
import org.yop.orm.simple.model.CyclePojo;
import org.yop.orm.sql.adapter.IConnection;

import java.sql.SQLException;

public class JoinAllTest extends DBMSSwitch {

	@Override
	protected String getPackageNames() {
		return "org.yop.orm.simple.model";
	}

	@Test
	public void testCycle() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()){
			CyclePojo pojo = new CyclePojo();
			pojo.setJopo(new CyclePojo.CycleJopo());
			pojo.setParent(pojo);
			pojo.getJopo().setPojo(pojo);

			Upsert.from(CyclePojo.class).onto(pojo).joinAll().execute(connection);
			CyclePojo fromDB = Select.from(CyclePojo.class).joinAll().uniqueResult(connection);
			Assert.assertTrue(fromDB.getParent() == fromDB);
			Assert.assertTrue(fromDB.getJopo().getPojo() ==  fromDB);

			fromDB = Select.from(CyclePojo.class).uniqueResult(connection);
			Hydrate.from(CyclePojo.class).onto(fromDB).joinAll().recurse().execute(connection);
			Assert.assertTrue(fromDB.getParent() == fromDB);
			Assert.assertTrue(fromDB.getJopo().getPojo() ==  fromDB);
		}
	}

	@Test
	public void testLongCycle() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()){
			CyclePojo pojo1 = new CyclePojo();
			pojo1.setJopo(new CyclePojo.CycleJopo());
			pojo1.getJopo().setString("1");
			pojo1.getJopo().setPojo(pojo1);

			CyclePojo pojo2 = new CyclePojo();
			pojo2.setJopo(new CyclePojo.CycleJopo());
			pojo2.setParent(pojo1);
			pojo2.getJopo().setString("2");
			pojo2.getJopo().setPojo(pojo2);

			CyclePojo pojo3 = new CyclePojo();
			pojo3.setJopo(new CyclePojo.CycleJopo());
			pojo3.setParent(pojo2);
			pojo3.getJopo().setString("3");
			pojo3.getJopo().setPojo(pojo3);
			pojo1.setParent(pojo3);

			Upsert.from(CyclePojo.class).onto(pojo1).joinAll().execute(connection);
			Upsert.from(CyclePojo.class).onto(pojo2).joinAll().execute(connection);
			Upsert.from(CyclePojo.class).onto(pojo3).joinAll().execute(connection);

			CyclePojo fromDB = Select
				.from(CyclePojo.class)
				.join(SQLJoin.to(CyclePojo::getJopo).where(Where.compare(CyclePojo.CycleJopo::getString, Operator.EQ, "3")))
				.uniqueResult(connection);
			Hydrate.from(CyclePojo.class).onto(fromDB).joinAll().recurse().execute(connection);
			Assert.assertEquals("3", fromDB.getJopo().getString());
			Assert.assertNotNull(fromDB.getParent());
			Assert.assertEquals("2", fromDB.getParent().getJopo().getString());
			Assert.assertNotNull(fromDB.getParent().getParent());
			Assert.assertEquals("1", fromDB.getParent().getParent().getJopo().getString());
			Assert.assertNotNull(fromDB.getParent().getParent().getParent());
			Assert.assertTrue(fromDB.getParent().getParent().getParent() == fromDB);
		}
	}
}
