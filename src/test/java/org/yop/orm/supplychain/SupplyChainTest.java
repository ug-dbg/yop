package org.yop.orm.supplychain;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.evaluation.In;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.query.*;
import org.yop.orm.supplychain.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Some tests using a rather classic supply chain model.
 * <br>
 * I must admit I am not very good at designing good test models :-/
 */
public class SupplyChainTest extends DBMSSwitch {

	private static final Logger logger = LoggerFactory.getLogger(SupplyChainTest.class);

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
				warehouse.setCapacity(i);
				organisation.getWarehouses().add(warehouse);
			}
			Upsert.from(Organisation.class).checkNaturalID().onto(organisation).joinAll().execute(connection);
			connection.commit();

			// field IN restriction
			Set<Long> capacities = new TreeSet<>(Arrays.asList(2L, 4L, 6L));
			Set<Warehouse> warehouses = Select
				.from(Warehouse.class)
				.where(new In(Warehouse::getCapacity, capacities))
				.execute(connection);
			Assert.assertEquals(capacities.size(), warehouses.size());
			Assert.assertEquals(
				capacities,
				new TreeSet<>(warehouses.stream().map(Warehouse::getCapacity).collect(Collectors.toList()))
			);

			Set<String> addresses = new TreeSet<>(Arrays.asList("1 My warehouses avenue", "2 My warehouses avenue"));
			warehouses = Select
				.from(Warehouse.class)
				.where(new In(Warehouse::getAddress, addresses))
				.execute(connection);
			Assert.assertEquals(addresses.size(), warehouses.size());
			Assert.assertEquals(
				addresses,
				new TreeSet<>(warehouses.stream().map(Warehouse::getAddress).collect(Collectors.toList()))
			);

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
			warehouses = Select
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

	@Test
	public void testBuyingStuff() throws SQLException, ClassNotFoundException {
		try (Connection connection = this.getConnection()) {
			Organisation organisation = new Organisation();
			organisation.setName("world company & Co");
			organisation.setSomeDummyFloat(3.1415F);

			Warehouse warehouse = new Warehouse();
			warehouse.setActive(true);
			warehouse.setAddress("1337 leet Road");
			warehouse.setCapacity(456724234234L);
			organisation.getWarehouses().add(warehouse);

			Employee ceo = new Employee();
			ceo.setActive(true);
			ceo.setName("Roger");
			ceo.setRole(Employee.Role.CEO);
			organisation.getEmployees().add(ceo);

			Employee cto = new Employee();
			cto.setActive(true);
			cto.setName("Régis");
			cto.setRole(Employee.Role.CTO);
			organisation.getEmployees().add(cto);

			Employee guru = new Employee();
			guru.setActive(true);
			guru.setName("Raoul");
			guru.setRole(Employee.Role.GURU);
			organisation.getEmployees().add(guru);

			Employee theUnknwonGuy = new Employee();
			theUnknwonGuy.setActive(true);
			theUnknwonGuy.setName("?????");
			theUnknwonGuy.setRole(Employee.Role.NO_IDEA);
			organisation.getEmployees().add(theUnknwonGuy);

			Manufacturer manufacturer = new Manufacturer();
			manufacturer.setName("Pickle Corp.");
			manufacturer.setAddress("42 Pickle Road");
			Reference pickleReference = new Reference();
			pickleReference.setManufacturerReference("#pickle");
			pickleReference.setManufacturer(manufacturer);

			Product pickle = new Product();
			pickle.setName("I'm a pickle !");
			pickle.setReference(pickleReference);
			pickle.setPrice(0.07F);
			warehouse.getProducts().add(pickle);

			Upsert.from(Organisation.class).onto(organisation).joinAll().execute(connection);

			// Find me a pickle ! But a pickle with some reference !
			Set<Product> products = Select
				.from(Product.class)
				.joinAll()
				.join(Join.to(Product::getReference).where(Where.naturalId(pickleReference)))
				.execute(connection);
			Assert.assertEquals(1, products.size());

			Reference actualReference = products.iterator().next().getReference();
			Assert.assertEquals(pickleReference, actualReference);
			Assert.assertEquals(manufacturer.getName(), actualReference.getManufacturer().getName());

			// Call me the guru before I buy my pickle !
			Set<Employee> guruWannabes = Select
				.from(Employee.class)
				.where(Where.compare(Employee::getRole, Operator.EQ, Employee.Role.GURU))
				.execute(connection);
			Assert.assertEquals(1 ,guruWannabes.size());
			Assert.assertEquals(guru.getName(), guruWannabes.iterator().next().getName());

			// All right, I want my pickle !
			Customer me = new Customer();
			me.setName("ug_dbg");
			me.setAbout("It's me !");
			me.setDateOfBirth(LocalDate.parse("1984-12-08"));
			me.setNice(false);
			me.setPhoneNumber(6_66_66_66_66);
			me.setSockSize((short) 41);
			Order order = new Order();
			order.getProducts().add(products.iterator().next());
			order.setCustomer(me);

			try {
				Upsert.from(Order.class).joinAll().join(Join.to(Order::getCustomer)).onto(order).execute(connection);
				logger.warn("Order#orderTimeStamp is marked not null! Is this MySQL with strict mode OFF ?");
			} catch (YopSQLException e) {
				logger.trace("Exception on inserting an Order with null orderTimeStamp. That's OK !", e);
			}

			order.setOrderTimeStamp(LocalDateTime.now());
			Upsert.from(Order.class).joinAll().join(Join.to(Order::getCustomer)).onto(order).execute(connection);
			Assert.assertNotNull(order.getId());

			// Oh well, I have to pay, I guess
			Payment payment = new Payment();
			payment.setOrder(order);
			payment.setWhen(LocalDateTime.now());
			payment.setMethod(Payment.Method.MONOPOLY_MONEY);
			order.setPayment(payment);
			Upsert.from(Order.class).join(Join.to(Order::getPayment)).onto(order).execute(connection);

			// How much was it already ?
			Set<Customer> meIGuess = Select.from(Customer.class).joinAll().where(Where.naturalId(me)).execute(connection);
			Assert.assertEquals(1, meIGuess.size());
			Assert.assertEquals(me, meIGuess.iterator().next());
			Long myID = me.getId();
			me.setId(null);
			Assert.assertEquals(me, meIGuess.iterator().next());
			me.setId(myID);

			Order myOrder = meIGuess.iterator().next().getOrders().iterator().next();
			Assert.assertEquals(
				pickle.getReference().getManufacturerReference(),
				myOrder.getProducts().iterator().next().getReference().getManufacturerReference()
			);
			Assert.assertEquals(pickle.getPrice(), myOrder.getProducts().iterator().next().getPrice(), 0.01);
		}
	}

}
