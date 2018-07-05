package org.yop.orm.model;

import org.junit.Assert;
import org.junit.Test;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.exception.YopRuntimeException;

import java.lang.reflect.Field;
import java.util.Collection;

public class YopableTest {

	@Test
	public void test_accessors_id(){
		AccessibleYopable yopable = new AccessibleYopable();
		yopable.setId(17L);
		Assert.assertEquals(17L, (long) yopable.getId());
	}

	@Test(expected = YopRuntimeException.class)
	public void test_set_id_not_accessible(){
		NonAccessibleIdYopable yopable = new NonAccessibleIdYopable();
		yopable.setId(17L);
	}

	@Test(expected = YopRuntimeException.class)
	public void test_get_id_not_accessible(){
		NonAccessibleIdYopable yopable = new NonAccessibleIdYopable();
		yopable.getId();
	}

	@Test
	public void test_equals_accessible() {
		AccessibleYopable yopable = new AccessibleYopable();
		yopable.email = "foo@bar.com";
		yopable.name = "foo";
		yopable.version = 1;
		yopable.deleted = false;

		AccessibleYopable other = new AccessibleYopable();
		other.email = "foo@bar.com";
		other.name = "foo-foo";
		other.version = 1;
		other.deleted = false;

		Assert.assertTrue(yopable.equals(other));

		other.deleted = true;
		Assert.assertFalse(yopable.equals(other));
	}

	@Test
	public void test_equals_accessible_null_target_or_null_id() {
		AccessibleYopable yopable = new AccessibleYopable();
		yopable.email = "foo@bar.com";
		yopable.name = "foo";
		yopable.version = 1;
		yopable.deleted = false;
		yopable.id = 1L;
		Assert.assertFalse(yopable.equals(null));

		AccessibleYopable other = new AccessibleYopable();
		other.email = "foo@bar.com";
		other.name = "foo";
		other.version = 1;
		other.deleted = false;
		other.id = 2L;
		Assert.assertFalse(yopable.equals(other));
	}

	@Test(expected = YopRuntimeException.class)
	public void test_equals_not_accessible_id() {
		NonAccessibleIdYopable yopable = new NonAccessibleIdYopable();
		yopable.setEmail("foo@bar.com");
		yopable.name = "foo";
		yopable.version = 1;
		yopable.deleted = false;

		NonAccessibleIdYopable other = new NonAccessibleIdYopable();
		other.setEmail("foo@bar.com");
		other.name = "foo-foo";
		other.version = 1;
		other.deleted = false;

		Assert.assertTrue(yopable.equals(other));
	}

	@Test(expected = YopRuntimeException.class)
	public void test_equals_not_accessible_natural_id() {
		NonAccessibleNaturalIdYopable yopable = new NonAccessibleNaturalIdYopable();
		yopable.setEmail("foo@bar.com");
		yopable.name = "foo";
		yopable.version = 1;
		yopable.deleted = false;

		NonAccessibleNaturalIdYopable other = new NonAccessibleNaturalIdYopable();
		other.setEmail("foo@bar.com");
		other.name = "foo-foo";
		other.version = 1;
		other.deleted = false;

		Assert.assertTrue(yopable.equals(other));

		other.deleted = true;
		Assert.assertFalse(yopable.equals(other));
	}

	@Test
	public void test_hashcode_accessible() {
		AccessibleYopable yopable = new AccessibleYopable();
		yopable.email = "foo@bar.com";
		yopable.name = "foo";
		yopable.version = 1;
		yopable.deleted = false;
		Assert.assertEquals(-1505031309, Yopable.hashCode(yopable));
	}

	@Test(expected = YopRuntimeException.class)
	public void test_hashcode_not_accessible() {
		NonAccessibleNaturalIdYopable yopable = new NonAccessibleNaturalIdYopable();
		yopable.setEmail("foo@bar.com");
		yopable.name = "foo";
		yopable.version = 1;
		yopable.deleted = false;
		Assert.assertEquals(-1505031309, Yopable.hashCode(yopable));
	}

	@Test(expected = NullPointerException.class)
	public void test_hashcode_null() {
		Assert.assertEquals(-1505031309, Yopable.hashCode(null));
	}

	private static class AccessibleYopable implements Yopable {
		@Id
		private Long id;

		@NaturalId
		private String email;

		@NaturalId
		Integer version;

		@NaturalId
		boolean deleted;

		String name;

		public void setEmail(String email) {
			this.email = email;
		}
	}

	private static class NonAccessibleNaturalIdYopable extends AccessibleYopable {
		@Override
		public Collection<Field> getNaturalId() {
			Collection<Field> fields = super.getNaturalId();
			fields.forEach(f -> f.setAccessible(false));
			return fields;
		}
	}

	private static class NonAccessibleIdYopable extends NonAccessibleNaturalIdYopable {
		@Override
		public Field getIdField() {
			Field idField = super.getIdField();
			idField.setAccessible(false);
			return idField;
		}
	}
}
