package org.yop.orm.simple;

import org.junit.Assert;
import org.junit.Test;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.query.JoinSet;
import org.yop.orm.query.Where;
import org.yop.orm.simple.model.Jopo;
import org.yop.orm.simple.model.Pojo;
import org.yop.orm.sql.adapter.IConnection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.yop.orm.Yop.select;
import static org.yop.orm.Yop.upsert;

/**
 * Test the {@link org.yop.orm.query.Select#count(IConnection)} operation on the 'simple' data objects.
 */
public class CountTest extends DBMSSwitch {

	@Override
	protected String getPackagePrefixes() {
		return "org.yop.orm.simple.model";
	}

	@Test
	public void test_count_single_instance() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			long count = select(Pojo.class).count(connection);
			Assert.assertEquals(0L, count);

			Pojo newPojo = new Pojo();
			newPojo.setVersion(1);
			newPojo.setType(Pojo.Type.FOO);
			newPojo.setActive(true);
			upsert(Pojo.class).onto(newPojo).execute(connection);

			count = select(Pojo.class).count(connection);
			Assert.assertEquals(1L, count);
		}
	}

	@Test
	public void test_count_several_instances() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			int amount = 201;
			List<Pojo> pojos = new ArrayList<>();
			for (int i = 1; i <= amount; i++) {
				Pojo newPojo = new Pojo();
				newPojo.setVersion(i);
				newPojo.setType(Pojo.Type.FOO);
				newPojo.setActive(true);
				pojos.add(newPojo);
			}
			upsert(Pojo.class).onto(pojos).execute(connection);

			long count = select(Pojo.class).count(connection);
			Assert.assertEquals(amount, count);
		}
	}

	@Test
	public void test_count_with_restriction() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			int amount = 201;
			List<Pojo> pojos = new ArrayList<>();
			for (int i = 1; i <= amount; i++) {
				Pojo newPojo = new Pojo();
				newPojo.setVersion(i);
				newPojo.setType(Pojo.Type.FOO);
				newPojo.setActive(true);
				pojos.add(newPojo);
			}
			upsert(Pojo.class).onto(pojos).execute(connection);

			long count = select(Pojo.class)
				.join(JoinSet.to(Pojo::getJopos).where(Where.isNotNull(Jopo::getId)))
				.count(connection);
			Assert.assertEquals(0, count);

			for (Pojo pojo : pojos) {
				if (pojo.getId() % 3 == 0) {
					pojo.setActive(true);
					Jopo jopo = new Jopo();
					jopo.setName("jopo #1 for Pojo #" + pojo.getId());
					pojo.getJopos().add(jopo);

					jopo = new Jopo();
					jopo.setName("jopo #2 for Pojo #" + pojo.getId());
					pojo.getJopos().add(jopo);
				} else {
					pojo.setActive(false);
				}
			}
			upsert(Pojo.class).onto(pojos).join(JoinSet.to(Pojo::getJopos)).execute(connection);

			count = select(Pojo.class)
				.join(JoinSet.to(Pojo::getJopos).where(Where.isNotNull(Jopo::getId)))
				.count(connection);
			Assert.assertEquals(201/3, count);
		}
	}
}
