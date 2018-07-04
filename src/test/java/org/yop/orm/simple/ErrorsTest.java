package org.yop.orm.simple;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.Yop;
import org.yop.orm.annotations.Column;
import org.yop.orm.exception.YopMapperException;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.reflection.DynamicEnum;
import org.yop.orm.simple.invalid_model.*;
import org.yop.orm.simple.model.Pojo;
import org.yop.orm.sql.adapter.IConnection;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Map;

/**
 * This test class uses some invalid Yopables for error test cases.
 */
public class ErrorsTest extends DBMSSwitch {

	private static final Logger logger = LoggerFactory.getLogger(ErrorsTest.class);

	@Override
	protected String getPackagePrefix() {
		// Load the 'simple' model, but not the 'invalid_model' package.
		// Then preparation does not prepare tables for the 'invalid_model' pojos.
		return "org.yop.orm.simple.model";
	}

	@Test(expected = YopMappingException.class)
	public void test_no_id() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Yop.select(PojoNoId.class).execute(connection);
		}
	}

	// This test is actually not an error test.
	@Test
	public void test_no_annotated_id() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Yop.select(PojoNoAnnotatedId.class).execute(connection);
		}
	}

	@Test(expected = YopMappingException.class)
	public void test_not_long_id() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Yop.select(PojoNotLongId.class).execute(connection);
		}
	}

	@Test(expected = YopMappingException.class)
	public void test_several_ids() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Yop.select(PojoSeveralIds.class).execute(connection);
		}
	}


	@Test(expected = YopSQLException.class)
	public void test_table_does_not_exist() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Yop.select(PojoInvalidTable.class).execute(connection);
		}
	}

	@Test(expected = YopSQLException.class)
	public void test_column_does_not_exist() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			Yop.select(PojoInvalidField.class).execute(connection);
		}
	}

	@Test
	public void test_array_type_column() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			PojoInvalidTypeField pojo = new PojoInvalidTypeField();
			pojo.setVersion(new Integer[]{1, 2});
			try {
				// Fault tolerant SQLite driver does not fail at upsert, but at select.
				Yop.upsert(PojoInvalidTypeField.class).onto(pojo).execute(connection);
				Yop.select(PojoInvalidTypeField.class).execute(connection);
				Assert.fail("There is an invalid @column array field. An exception should have occurred.");
			} catch (YopSQLException | YopMapperException e) {
				logger.debug("Error with an @Column array field. That's OK");
			}
		}
	}

	@Test(expected = YopMappingException.class)
	public void test_unknown_type_jointable_field() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			PojoUnknownTypeField pojo = new PojoUnknownTypeField();
			pojo.setVersion(4);
			Yop.upsert(PojoUnknownTypeField.class).onto(pojo).execute(connection);
			Yop.select(PojoUnknownTypeField.class).execute(connection);
		}
	}

	@Test(expected = YopMapperException.class)
	public void test_static_final_field() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			PojoStaticFinalField pojo = new PojoStaticFinalField();
			pojo.setType(PojoStaticFinalField.Type.BAR);
			pojo.setVersion(13);
			Yop.upsert(PojoStaticFinalField.class).onto(pojo).execute(connection);
			Yop.select(PojoStaticFinalField.class).execute(connection);
		}
	}

	@Test(expected = YopMapperException.class)
	public void test_null_enum_field() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			PojoNotNullableEnumField pojo = new PojoNotNullableEnumField();
			pojo.setType(null);
			pojo.setVersion(13);
			Yop.upsert(PojoNotNullableEnumField.class).onto(pojo).execute(connection);
		}
	}

	@Test(expected = YopMapperException.class)
	public void test_upsert_silly_enum_strategy() throws SQLException, ReflectiveOperationException {
		Map<String, Object> typeAnnotationValues = DynamicEnum.getFieldAnnotationValues(
			Pojo.class,
			Pojo::getType,
			Column.class
		);
		try (IConnection connection = this.getConnection()) {
			DynamicEnum.addEnum(Column.EnumStrategy.class, "SILLY");
			typeAnnotationValues.put("enum_strategy", Column.EnumStrategy.valueOf("SILLY"));

			Pojo pojo = new Pojo();
			pojo.setType(Pojo.Type.BAR);
			pojo.setVersion(13);
			Yop.upsert(Pojo.class).onto(pojo).execute(connection);
		} finally {
			typeAnnotationValues.put("enum_strategy", Column.EnumStrategy.NAME);
		}
	}

	@Test(expected = YopMapperException.class)
	public void test_select_silly_enum_strategy() throws SQLException, ReflectiveOperationException {
		Map<String, Object> typeAnnotationValues = DynamicEnum.getFieldAnnotationValues(
			Pojo.class,
			Pojo::getType,
			Column.class
		);
		try (IConnection connection = this.getConnection()) {
			Pojo pojo = new Pojo();
			pojo.setType(Pojo.Type.BAR);
			pojo.setVersion(13);
			Yop.upsert(Pojo.class).onto(pojo).execute(connection);

			DynamicEnum.addEnum(Column.EnumStrategy.class, "SILLY");
			typeAnnotationValues.put("enum_strategy", Column.EnumStrategy.valueOf("SILLY"));

			Yop.select(Pojo.class).execute(connection);
		} finally {
			typeAnnotationValues.put("enum_strategy", Column.EnumStrategy.NAME);
		}
	}

	@Test(expected = YopRuntimeException.class)
	public void test_bad_transformer() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()) {
			PojoBadTransformer pojo = new PojoBadTransformer();
			pojo.setaVeryLongInteger(new BigInteger("123456"));
			Yop.upsert(PojoBadTransformer.class).onto(pojo).execute(connection);
		}
	}
}
