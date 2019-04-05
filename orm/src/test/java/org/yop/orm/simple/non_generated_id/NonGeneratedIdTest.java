package org.yop.orm.simple.non_generated_id;

import org.junit.Assert;
import org.junit.Test;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.query.sql.Delete;
import org.yop.orm.query.sql.Hydrate;
import org.yop.orm.query.sql.Select;
import org.yop.orm.query.sql.Upsert;
import org.yop.orm.simple.non_generated_id.model.Pojo;
import org.yop.orm.sql.adapter.IConnection;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Yop tries to deal with non-generated ID objects, even if it breaks the root paradigm :
 * <ul>
 *     <li>ID is set     → UPDATE</li>
 *     <li>ID is not set → INSERT</li>
 * </ul>
 */
public class NonGeneratedIdTest extends DBMSSwitch {

	@Override
	protected String getPackageNames() {
		return "org.yop.orm.simple.non_generated_id.model";
	}

	@Test
	public void testCRUD_forceInsert() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo("anIDThatIsAString", "Hello world", LocalDate.now());
			Upsert.from(Pojo.class).onto(pojo).forceInsert().execute(connection);

			Pojo pojoFromDB = Select.from(Pojo.class).uniqueResult(connection);
			Assert.assertEquals(pojo, pojoFromDB);

			Delete.from(Pojo.class).whereId(pojo.getId()).executeQueries(connection);
			Assert.assertEquals(0, Select.from(Pojo.class).count(connection).intValue());
		}
	}

	@Test
	public void testCRUD_checkNaturalID() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo("anIDThatIsAString", "Hello world", LocalDate.now());
			Upsert.from(Pojo.class).onto(pojo).checkNaturalID().execute(connection);

			Pojo pojoFromDB = Select.from(Pojo.class).uniqueResult(connection);
			Assert.assertEquals(pojo, pojoFromDB);

			Delete.from(Pojo.class).whereId(pojo.getId()).executeQueries(connection);
			Assert.assertEquals(0, Select.from(Pojo.class).count(connection).intValue());
		}
	}

	@Test
	public void testCRUD_WithJoin() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo("anIDThatIsAString", "Hello world", LocalDate.now());
			Pojo parent = new Pojo("ParentIDString",  "Hello world from parent", LocalDate.now());
			pojo.setParent(parent);
			Upsert.from(Pojo.class).onto(pojo).join(Pojo::getParent).checkNaturalID(true).execute(connection);

			Pojo pojoFromDB = Select.from(Pojo.class).whereId(pojo.getId()).joinAll().uniqueResult(connection);
			Assert.assertEquals(pojo, pojoFromDB);
			Assert.assertEquals(pojo.getParent(), pojoFromDB.getParent());

			Hydrate
				.from(Pojo.class)
				.onto(pojoFromDB)
				.join(Pojo::getParent, Pojo::getChild)
				.recurse()
				.execute(connection);
			Assert.assertEquals(pojo, pojoFromDB.getParent().getChild());

			Delete.from(Pojo.class).whereId(pojo.getId()).join(Pojo::getParent).executeQueries(connection);
			Assert.assertEquals(0, Select.from(Pojo.class).count(connection).intValue());
		}
	}
}
