package org.yop.reflection;

import org.junit.Assert;
import org.junit.Test;
import org.yop.reflection.model.Book;

import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Testing {@link Reflection} utility class.
 */
public class ReflectionTest {

	@Test
	public void testForName() {
		Class<Book> clazz = Reflection.forName("org.yop.reflection.model.Book", this.getClass().getClassLoader());
		Assert.assertEquals(Book.class, clazz);

		clazz = Reflection.forName("org.yop.reflection.model.Book");
		Assert.assertEquals(Book.class, clazz);
	}

	@Test
	public void testForName_EmptyClassLoader() {
		Class<Book> clazz = Reflection.forName("org.yop.reflection.model.Book", new URLClassLoader(new URL[0], null));
		Assert.assertEquals(Book.class, clazz);
	}

	@Test(expected = ReflectionException.class)
	public void testForName_ClassNotFound() {
		Reflection.forName("org.yop.reflection.model.BadBook");
	}

	@Test
	public void testPackageName() {
		Assert.assertEquals("", Reflection.packageName(Reflection.forName("NoPackageModel")));
		Assert.assertEquals("org.yop.reflection.model", Reflection.packageName(Book.class));
	}

	@Test
	public void test_getConstructor() throws InvocationTargetException, IllegalAccessException, InstantiationException {
		Constructor<NoDefaultConstructor> init = Reflection.getConstructor(NoDefaultConstructor.class, String.class);
		if (init == null) {
			Assert.fail("Could not find constructor.");
		}
		NoDefaultConstructor object = init.newInstance("parameter");
		Assert.assertEquals("parameter", object.string);
	}

	@Test
	public void test_getNoConstructor() {
		Constructor<NoDefaultConstructor> init = Reflection.getConstructor(NoDefaultConstructor.class, Integer.class);
		Assert.assertNull(init);
	}

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
	public void test_getMethods() {
		Set<String> methods = Reflection.getMethods(List.class).stream().map(Method::getName).collect(Collectors.toSet());
		Assert.assertTrue(methods.contains("add"));
		Assert.assertTrue(methods.contains("removeIf"));

		methods = Reflection.getMethods(ArrayList.class).stream().map(Method::getName).collect(Collectors.toSet());
		Assert.assertTrue(methods.contains("add"));
		Assert.assertTrue(methods.contains("grow"));
		Assert.assertTrue(methods.contains("removeIf"));
	}

	@Test
	public void test_getMethod_does_not_exist() {
		Method get = Reflection.getMethod(List.class, "thisMethodDoesNotExist", int.class);
		Assert.assertNull(get);
	}

	@Test
	public void test_getAnnotation() {
		Assert.assertNotNull(Reflection.getAnnotation(Book.class, Book.Entity.class));
		Assert.assertNull(Reflection.getAnnotation(Book.class, Book.TechnicalID.class));
		Assert.assertNotNull(Reflection.getAnnotation(Book.IsEmptyPredicate.class, FunctionalInterface.class));
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

	@Test()
	public void test_findPrimitiveField_with_getter() {
		Field field = Reflection.findField(Book.class, Book::getId);
		Assert.assertEquals("org.yop.reflection.model.Book#id", Reflection.fieldToString(field));
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
	public void test_findField_fallback_unsafe() {
		Field field = Reflection.findField(NoDefaultConstructorField.class, NoDefaultConstructorField::getTrickyField);
		Assert.assertNotNull(field);
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

	@Test
	public void test_fieldToString() {
		Field isbnField = Reflection.get(Book.class, "isbn");
		String isbnFieldAsString = Reflection.fieldToString(isbnField);
		Assert.assertEquals("org.yop.reflection.model.Book#isbn", isbnFieldAsString);
	}

	@Test(expected = ReflectionException.class)
	public void test_setField_bad_type() {
		Field isbnField = Reflection.get(Book.class, "isbn");
		Book book = new Book("1234567891011");
		Reflection.set(isbnField, book, 12);
	}

	@Test(expected = ReflectionException.class)
	public void test_setField_field_not_accessible() {
		Field isbnField = Reflection.get(Book.class, "isbn");
		if (isbnField == null) {
			Assert.fail("Could not find isbn field on Book !");
		}
		isbnField.setAccessible(false);
		Book target = new Book("1234567891012");
		Reflection.set(isbnField, target, "1234567891011");
		Assert.assertEquals("1234567891011", target.getIsbn());
	}

	@Test(expected = ReflectionException.class)
	public void test_setFieldFrom_field_not_accessible() {
		Field isbnField = Reflection.get(Book.class, "isbn");
		if (isbnField == null) {
			Assert.fail("Could not find isbn field on Book !");
		}
		isbnField.setAccessible(false);
		Book target = new Book("1234567891012");
		Reflection.setFrom(isbnField, new Book("1234567891011"), new Book("1234567891011"));
		Assert.assertEquals("1234567891011", target.getIsbn());
	}

	@Test
	public void test_get1ArgParameter() {
		Field field = Reflection.getFields(Book.class, Book.ComposedOf.class).get(0);
		Type argParameter = Reflection.get1ArgParameter(field);
		Assert.assertEquals(Book.Sheet.class, argParameter);
	}

	@Test(expected = ReflectionException.class)
	public void test_get1ArgParameter_no_arg_field() {
		Field field = Reflection.getFields(Book.class, Book.TechnicalID.class).get(0);
		Reflection.get1ArgParameter(field);
	}

	@Test
	public void test_getTarget() {
		Class<String> target = Reflection.getTarget(Reflection.findField(Book.class, Book::getIsbn));
		Assert.assertEquals(String.class, target);
	}

	@Test
	public void test_getTarget_collection() {
		Class<Book.Sheet> target = Reflection.getTarget(Reflection.findField(Book.class, Book::getSheets));
		Assert.assertEquals(Book.Sheet.class, target);
	}

	@Test
	public void test_getGetterTarget() {
		Class<String> target = Reflection.getGetterTarget(Book.class, Book::getIsbn);
		Assert.assertEquals(String.class, target);
	}

	@Test
	public void test_getGetterTarget_collection() {
		Class<Book.Sheet> target = Reflection.getGetterCollectionTarget(Book.class, Book::getSheets);
		Assert.assertEquals(Book.Sheet.class, target);
	}

	@Test
	public void test_getSetterTarget() {
		Class<String> target = Reflection.getSetterTarget(Book.class, Book::setIsbn);
		Assert.assertEquals(String.class, target);
	}

	@Test
	public void test_getSetterTarget_collection() {
		BiConsumer<Book, List<Book.Sheet>> setJopos = Book::setSheets;
		Class<Book.Sheet> target = Reflection.getSetterCollectionTarget(Book.class, setJopos);
		Assert.assertEquals(Book.Sheet.class, target);
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

	private static class NoDefaultConstructorField {
		@SuppressWarnings("unused")
		private NoDefaultConstructor trickyField;

		NoDefaultConstructor getTrickyField() {
			return this.trickyField;
		}
	}

	private static class NoDefaultConstructor {
		private String string;

		// overload the default constructor
		private NoDefaultConstructor(String string){
			this.string = string;
		}
	}
}
