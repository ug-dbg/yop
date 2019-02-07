package org.yop.orm.reflection;

import org.junit.Assert;
import org.junit.Test;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.exception.ReflectionException;
import org.yop.orm.simple.model.Jopo;
import org.yop.orm.simple.model.Pojo;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Testing {@link Reflection} utility class.
 */
public class ReflectionTest {

	@Test
	public void test_getMethod() throws InvocationTargetException, IllegalAccessException {
		Method get = Reflection.getMethod(List.class, "get", int.class);
		if (get == null) {
			Assert.fail("Could not find java.util.List#get(Integer). That's not good.");
		}

		List<String> strings = Arrays.asList("1", "2", "3");
		Object first = get.invoke(strings, 0);
		Assert.assertEquals("1", first);
	}

	@Test
	public void test_getMethod_does_not_exist() {
		Method get = Reflection.getMethod(List.class, "thisMethodDoesNotExist", int.class);
		Assert.assertNull(get);
	}

	@Test
	public void test_fieldToString() {
		Field idField = ORMUtil.getIdField(Pojo.class);
		String idFieldAsString = Reflection.fieldToString(idField);
		Assert.assertEquals("org.yop.orm.simple.model.Pojo#id", idFieldAsString);
	}

	@Test
	public void test_fieldToString_null_field() {
		String fieldAsString = Reflection.fieldToString(null);
		Assert.assertEquals("null", fieldAsString);
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

	@Test(expected = ReflectionException.class)
	public void test_get1ArgParameter_several_args_field() {
		Field field = Reflection.getFields(Local.class).get(0);
		Reflection.get1ArgParameter(field);
	}

	@Test()
	public void test_findField_with_getter() {
		Field field = Reflection.findField(Local.class, Local::getMap);
		Assert.assertEquals("org.yop.orm.reflection.ReflectionTest$Local#map", Reflection.fieldToString(field));
	}

	@Test(expected = ReflectionException.class)
	public void test_findField_bad_getter() {
		Reflection.findField(Local.class, Local::getMap_Bad);
	}

	@Test()
	public void test_findField_with_setter() {
		Field field = Reflection.findField(Local.class, Local::setMap);
		Assert.assertEquals("org.yop.orm.reflection.ReflectionTest$Local#map", Reflection.fieldToString(field));
	}

	@Test(expected = ReflectionException.class)
	public void test_findField_with_bad_setter() {
		Reflection.findField(Local.class, Local::setMap_Bad);
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

	@Test
	public void test_readFieldFromFieldName() {
		Local local = new Local();
		Map<String, String> map = new HashMap<>();
		local.map = map;
		Assert.assertTrue(map == Reflection.readField("map", local));
	}

	@Test (expected = ReflectionException.class)
	public void test_readFieldFromInvalidFieldName() {
		Reflection.readField("mapWithBadName", new Local());
	}

	private static class Local {
		private Map<String, String> map;

		private Map<String, String> getMap() {
			return this.map;
		}

		private Map<String, String> getMap_Bad() {
			return null;
		}

		private void setMap(Map<String, String> map) {
			this.map = map;
		}

		private void setMap_Bad(Map<String, String> map) {
			this.map = new HashMap<>();
			this.map.putAll(map);
		}
	}
}
