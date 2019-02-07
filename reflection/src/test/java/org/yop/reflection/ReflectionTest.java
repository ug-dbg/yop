package org.yop.reflection;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	public void test_fieldToString_null_field() {
		String fieldAsString = Reflection.fieldToString(null);
		Assert.assertEquals("null", fieldAsString);
	}

	@Test(expected = ReflectionException.class)
	public void test_get1ArgParameter_several_args_field() {
		Field field = Reflection.getFields(Local.class).get(0);
		Reflection.get1ArgParameter(field);
	}

	@Test()
	public void test_findField_with_getter() {
		Field field = Reflection.findField(Local.class, Local::getMap);
		Assert.assertEquals("org.yop.reflection.ReflectionTest$Local#map", Reflection.fieldToString(field));
	}

	@Test(expected = ReflectionException.class)
	public void test_findField_bad_getter() {
		Reflection.findField(Local.class, Local::getMap_Bad);
	}

	@Test()
	public void test_findField_with_setter() {
		Field field = Reflection.findField(Local.class, Local::setMap);
		Assert.assertEquals("org.yop.reflection.ReflectionTest$Local#map", Reflection.fieldToString(field));
	}

	@Test(expected = ReflectionException.class)
	public void test_findField_with_bad_setter() {
		Reflection.findField(Local.class, Local::setMap_Bad);
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
