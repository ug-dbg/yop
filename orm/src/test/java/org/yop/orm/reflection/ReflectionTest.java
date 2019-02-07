package org.yop.orm.reflection;

import org.junit.Assert;
import org.junit.Test;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.simple.model.Jopo;
import org.yop.orm.simple.model.Pojo;
import org.yop.orm.util.ORMUtil;
import org.yop.reflection.Reflection;
import org.yop.reflection.ReflectionException;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Testing {@link Reflection} and {@link ORMUtil} utility classes.
 */
public class ReflectionTest {

	@Test
	public void test_fieldToString() {
		Field idField = ORMUtil.getIdField(Pojo.class);
		String idFieldAsString = Reflection.fieldToString(idField);
		Assert.assertEquals("org.yop.orm.simple.model.Pojo#id", idFieldAsString);
	}


	@Test(expected = ReflectionException.class)
	public void test_setField_bad_type() {
		Field idField = ORMUtil.getIdField(Pojo.class);
		Pojo target = new Pojo();
		Reflection.set(idField, target, 12);
	}

	@Test(expected = ReflectionException.class)
	public void test_setField_field_not_accessible() {
		Field idField = ORMUtil.getIdField(Pojo.class);
		idField.setAccessible(false);
		Pojo target = new Pojo();
		Reflection.set(idField, target, 12L);
	}

	@Test(expected = ReflectionException.class)
	public void test_setFieldFrom_field_not_accessible() {
		Field idField = ORMUtil.getIdField(Pojo.class);
		idField.setAccessible(false);
		Pojo source = new Pojo();
		Pojo target = new Pojo();
		Reflection.setFrom(idField, source, target);
	}

	@Test
	public void test_get1ArgParameter() {
		Field field = Reflection.getFields(Pojo.class, JoinTable.class).get(0);
		Type argParameter = Reflection.get1ArgParameter(field);
		Assert.assertEquals(Jopo.class, argParameter);
	}

	@Test(expected = ReflectionException.class)
	public void test_get1ArgParameter_no_arg_field() {
		Field field = Reflection.getFields(Pojo.class, Column.class).get(0);
		Reflection.get1ArgParameter(field);
	}

	@Test
	public void test_getTarget() {
		Class<Object> target = Reflection.getTarget(Reflection.findField(Pojo.class, Pojo::getType));
		Assert.assertEquals(Pojo.Type.class, target);
	}

	@Test
	public void test_getTarget_collection() {
		Class<Object> target = Reflection.getTarget(Reflection.findField(Pojo.class, Pojo::getJopos));
		Assert.assertEquals(Jopo.class, target);
	}

	@Test
	public void test_getGetterTarget() {
		Class<Object> target = Reflection.getGetterTarget(Pojo.class, Pojo::getType);
		Assert.assertEquals(Pojo.Type.class, target);
	}

	@Test
	public void test_getGetterTarget_collection() {
		Class<Jopo> target = Reflection.getGetterCollectionTarget(Pojo.class, Pojo::getJopos);
		Assert.assertEquals(Jopo.class, target);
	}

	@Test
	public void test_getSetterTarget() {
		Class<Pojo.Type> target = Reflection.getSetterTarget(Pojo.class, Pojo::setType);
		Assert.assertEquals(Pojo.Type.class, target);
	}

	@Test
	public void test_getSetterTarget_collection() {
		BiConsumer<Pojo, Set<Jopo>> setJopos = Pojo::setJopos;
		Class<Jopo> target = Reflection.getSetterCollectionTarget(Pojo.class, setJopos);
		Assert.assertEquals(Jopo.class, target);
	}
}
