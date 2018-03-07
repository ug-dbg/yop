package org.yop.orm.supplychain;

import org.junit.Assert;
import org.junit.Test;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.query.*;
import org.yop.orm.supplychain.model.Organisation;
import org.yop.orm.supplychain.model.Warehouse;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/**
 * Some tests using a rather classic supply chain model.
 * <br>
 * I must admit I am not very good at designing good test models :-/
 */
public class SupplyChainTest extends DBMSSwitch {

	@Override
	protected String getPackagePrefix() {
		return "org.yop.orm.supplychain.model";
	}

	@Test
	public void testCreateOrganisation() throws SQLException, ClassNotFoundException {
		try (Connection connection = this.getConnection()) {
			connection.setAutoCommit(false);

			Organisation organisation = new Organisation();
			organisation.setName("world company & Co");
			organisation.setSomeDummyFloat(3.1415F);

			Upsert.from(Organisation.class).checkNaturalID().onto(organisation).execute(connection);
			connection.commit();

			Set<Organisation> organisations = Select.from(Organisation.class).joinAll().execute(connection);
			Assert.assertEquals(1, organisations.size());
			Assert.assertEquals(organisation, organisations.iterator().next());

			Warehouse warehouse = new Warehouse();
			warehouse.setActive(true);
			warehouse.setAddress("1337 leet Road");
			warehouse.setCapacity(456724234234L);
			organisation.getWarehouses().add(warehouse);

			Upsert.from(Organisation.class).checkNaturalID().onto(organisation).joinAll().execute(connection);
			connection.commit();

			organisations = Select.from(Organisation.class).joinAll().execute(connection);
			Assert.assertEquals(1, organisations.size());
			Assert.assertEquals(organisation, organisations.iterator().next());
			Assert.assertEquals(organisation.getWarehouses(), organisations.iterator().next().getWarehouses());

			// Let's add a lot of warehouses !
			for (int i = 0; i < 150; i++) {
				warehouse = new Warehouse();
				warehouse.setActive(i % 2 == 0);
				warehouse.setAddress(i + " My warehouses avenue");
				warehouse.setCapacity(i * 100);
				organisation.getWarehouses().add(warehouse);
			}
			Upsert.from(Organisation.class).checkNaturalID().onto(organisation).joinAll().execute(connection);
			connection.commit();

			// Fetch a transient relation ! (Warehouse → Organisation)
			organisations = Select
				.from(Organisation.class)
				.join(JoinSet.to(Organisation::getWarehouses).join(Join.to(Warehouse::getOwner)))
				.execute(connection);
			Assert.assertEquals(1, organisations.size());
			Assert.assertEquals(organisation, organisations.iterator().next());
			Assert.assertEquals(151, organisations.iterator().next().getWarehouses().size());
			Assert.assertEquals(organisation.getWarehouses(), organisations.iterator().next().getWarehouses());
			Assert.assertEquals(organisation, organisations.iterator().next().getWarehouses().iterator().next().getOwner());

			// Deleting a warehouse
			organisation.getWarehouses().remove(0);
			Upsert.from(Organisation.class).checkNaturalID().onto(organisation).joinAll().execute(connection);
			organisations = Select.from(Organisation.class)
				.join(JoinSet.to(Organisation::getWarehouses).join(Join.to(Warehouse::getOwner)))
				.execute(connection);
			Assert.assertEquals(1, organisations.size());
			Assert.assertEquals(organisation, organisations.iterator().next());
			Assert.assertEquals(150, organisations.iterator().next().getWarehouses().size());
			Assert.assertEquals(organisation.getWarehouses(), organisations.iterator().next().getWarehouses());
			Assert.assertEquals(organisation, organisations.iterator().next().getWarehouses().iterator().next().getOwner());

			// Adding a new organisation and warehouses
			organisation = new Organisation();
			organisation.setName("Evil & Co");
			organisation.setSomeDummyFloat(2.718F);

			warehouse = new Warehouse();
			warehouse.setActive(false);
			warehouse.setAddress("17 Yolo Avenue");
			warehouse.setCapacity(-3);
			organisation.getWarehouses().add(warehouse);

			Upsert.from(Organisation.class).joinAll().onto(organisation).execute(connection);
			connection.commit();
			Set<Warehouse> warehouses = Select
				.from(Warehouse.class)
				.where(Where.compare(Warehouse::getAddress, Operator.LIKE, "%Yolo%"))
				.join(Join.to(Warehouse::getOwner))
				.execute(connection);
			Assert.assertEquals(1, warehouses.size());
			Assert.assertEquals(warehouse, warehouses.iterator().next());
			Assert.assertEquals(organisation, warehouses.iterator().next().getOwner());

			// Select with where clause on relation
			warehouses = Select
				.from(Warehouse.class)
				.where(Where.compare(Warehouse::getOwner, Operator.EQ, organisation.getId()))
				.join(Join.to(Warehouse::getOwner))
				.execute(connection);
			Assert.assertEquals(1, warehouses.size());

			Select<Warehouse> select = Select
				.from(Warehouse.class)
				.join(Join.to(Warehouse::getOwner).where(Where.naturalId(organisation)));
			warehouses = select.execute(connection);
			Assert.assertEquals(1, warehouses.size());

			organisation.setSomeDummyFloat(1.337F);
			Upsert.from(Organisation.class).onto(organisation).execute(connection);
			organisations = Select.from(Organisation.class).where(Where.id(organisation.getId())).execute(connection);
			Assert.assertEquals(1, organisations.size());
			Assert.assertEquals(organisation.getSomeDummyFloat(), organisations.iterator().next().getSomeDummyFloat(), 0.01);

			// And delete !
			Delete<Warehouse> delete = select.toDelete();
			delete.executeQueries(connection);

			warehouses = select.execute(connection);
			Assert.assertEquals(0, warehouses.size());
			organisations = Select.from(Organisation.class).where(Where.naturalId(organisation)).execute(connection);
			Assert.assertEquals(0, organisations.size());
		}
	}

}
