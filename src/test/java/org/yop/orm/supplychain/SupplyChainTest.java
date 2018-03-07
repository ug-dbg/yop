package org.yop.orm.supplychain;

import org.junit.Assert;
import org.junit.Test;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.query.Select;
import org.yop.orm.query.Upsert;
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
		Connection connection = this.getConnection();
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
	}

}
