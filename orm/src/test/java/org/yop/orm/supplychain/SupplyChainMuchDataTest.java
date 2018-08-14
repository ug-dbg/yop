package org.yop.orm.supplychain;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.query.Delete;
import org.yop.orm.query.Select;
import org.yop.orm.query.Upsert;
import org.yop.orm.query.Where;
import org.yop.orm.query.batch.BatchUpsert;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.supplychain.model.Organisation;
import org.yop.orm.supplychain.model.Product;
import org.yop.orm.supplychain.model.Warehouse;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * A unit test where there are more data inserted than really required :-D
 */
public class SupplyChainMuchDataTest extends DBMSSwitch {

	private static final Logger logger = LoggerFactory.getLogger(SupplyChainMuchDataTest.class);

	private static final int NB_WAREHOUSES;
	private static final int NB_PRODUCTS;

	static {
		switch (dbms()){
			case "sqlite" : NB_WAREHOUSES = 15;  NB_PRODUCTS = 15; break;
			default:        NB_WAREHOUSES = 100; NB_PRODUCTS = 50; break;
		}
	}

	@Override
	protected String getPackagePrefix() {
		return "org.yop.orm.supplychain.model";
	}

	private void createData(IConnection connection, String suffix) {
		Organisation organisation = new Organisation();
		organisation.setName("Company" + suffix);
		organisation.setSomeDummyFloat(3.1415F + suffix.length());

		Upsert.from(Organisation.class).checkNaturalID().onto(organisation).execute(connection);

		Upsert.from(Organisation.class).checkNaturalID().onto(organisation).joinAll().execute(connection);

		// Let's add a lot of warehouses !
		for (int i = 0; i < NB_WAREHOUSES; i++) {
			Warehouse warehouse = new Warehouse();
			warehouse.setActive(i % 2 == 0);
			warehouse.setAddress(i + " My warehouses avenue " + suffix);
			warehouse.setCapacity(i);
			organisation.getWarehouses().add(warehouse);

			for(int j = 0; j < NB_PRODUCTS; j++) {
				Product product = new Product();
				product.setPrice(j);
				product.setName("Product" + suffix + "#" + i + "_" + j);
				product.setComment("This is me, adding the product #" + j + " in warehouse #" + i);
				product.setDescription("product #" + j + " in warehouse #" + i);
				product.setHeight(150);
				product.setWeight(1.32F);
				product.setCreationDate(LocalDateTime.now());
				product.setAvailableFrom(LocalDateTime.now());
				product.setAvailableUntil(LocalDateTime.now().plus(Duration.ofDays(200)));
				warehouse.getProducts().add(product);
			}
		}
		Upsert.from(Organisation.class).checkNaturalID().onto(organisation).joinAll().execute(connection);

		// Adding a new organisation and warehouses
		organisation = new Organisation();
		organisation.setName("Evil & Co " + suffix);
		organisation.setSomeDummyFloat(2.718F + suffix.length());

		Warehouse warehouse = new Warehouse();
		warehouse.setActive(false);
		warehouse.setAddress("17 Yolo Avenue " + suffix);
		warehouse.setCapacity(-3);
		organisation.getWarehouses().add(warehouse);

		Upsert.from(Organisation.class).joinAll().onto(organisation).execute(connection);
	}

	@Test
	public void testCRUD() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			connection.setAutoCommit(true);
			this.createData(connection, "Foo");
			this.createData(connection, "Bar");

			long start = System.currentTimeMillis();
			Set<Warehouse> warehouses = Select.from(Warehouse.class).joinAll().execute(connection);
			logger.info("Big select [" + warehouses.size() + "] in [" + (System.currentTimeMillis() - start) + "] ms");

			float newWeight = 1.11111F;
			Product product_1 = Select
				.from(Product.class)
				.where(Where.compare(Product::getName, Operator.LIKE, "%#1_1"))
				.uniqueResult(connection);
			product_1.setWeight(newWeight);

			Upsert.from(Product.class).onto(product_1).execute(connection);

			product_1 = Select.from(Product.class).where(Where.id(product_1.getId())).uniqueResult(connection);
			Assert.assertEquals(newWeight, product_1.getWeight(), 0.0001);

			newWeight = 2.22222F;
			product_1.setId(null);
			product_1.setWeight(newWeight);
			Upsert.from(Product.class).checkNaturalID().onto(product_1).execute(connection);
			product_1 = Select.from(Product.class).where(Where.id(product_1.getId())).uniqueResult(connection);
			Assert.assertEquals(newWeight, product_1.getWeight(), 0.0001);

			// Batch INSERT + UPDATE
			for (Warehouse warehouse : warehouses) {
				warehouse.setAddress(String.valueOf(warehouse.getAddress()) + "-updated !");
			}
			Warehouse warehouse = new Warehouse();
			warehouse.setActive(true);
			warehouse.setAddress("1, Batch Upsert Boulevard");
			warehouse.setCapacity(2);
			warehouses.add(warehouse);
			int nbOfWarehouses = warehouses.size();

			BatchUpsert.from(Warehouse.class).onto(warehouses).checkNaturalID().execute(connection);

			warehouses = Select.from(Warehouse.class).joinAll().execute(connection);
			Assert.assertEquals(nbOfWarehouses, warehouses.size());
			for (Warehouse toCheck : warehouses) {
				Assert.assertTrue(
					"Warehouse [" + warehouse + "] was not correctly inserted / updated.",
					toCheck.getAddress().endsWith("-updated !")
					|| toCheck.getAddress().equals("1, Batch Upsert Boulevard")
				);
			}

			Delete.from(Organisation.class).joinAll().executeQueries(connection);
			Set<Product> left = Select.from(Product.class).execute(connection);
			Assert.assertEquals(0, left.size());
		}
	}
}
