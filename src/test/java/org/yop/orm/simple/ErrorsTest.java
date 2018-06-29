package org.yop.orm.simple;

import org.junit.Test;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.Yop;
import org.yop.orm.annotations.Column;
import org.yop.orm.exception.YopMapperException;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.reflection.DynamicEnum;
import org.yop.orm.simple.invalid_model.*;
import org.yop.orm.simple.model.Pojo;
import org.yop.orm.sql.adapter.IConnection;

import java.sql.SQLException;
import java.util.Map;

/**
 * This test class uses some invalid Yopables for error test cases.
 */
public class ErrorsTest extends DBMSSwitch {

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

	@SuppressWarnings("unchecked")
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

	@SuppressWarnings("unchecked")
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
}
