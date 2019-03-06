package org.yop.orm.util;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A static cache for anything related to {@link ORMUtil}.
 * <br>
 * {@link java.lang.reflect} methods can take a bit of time. Let's try to use some cache !
 * <br>
 * Very similar to {@link ReflectionCache} but I would like to keep pure reflection and YOP ORM concepts separated.
 */
class ORMUtilCache {

	/** {@link JoinUtil#joinedFields(Class)} fields for a given class */
	private static final MultiValuedMap<Class, Field> JOINED_FIELDS   = new ArrayListValuedHashMap<>();

	/** Is this field a collection field, a Yopable field or something else ? */
	private static final Map<Field, ORMUtil.FieldType> FIELD_TYPES  = new HashMap<>();

	/**
	 * Is this field a {@link ORMUtil.FieldType} ?
	 * @param field the field to check
	 * @return true if the given field matches the given field type.
	 */
	static boolean isOfType(Field field, ORMUtil.FieldType type) {
		if (! FIELD_TYPES.containsKey(field)) {
			FIELD_TYPES.put(field, ORMUtil.FieldType.fromField(field));
		}
		return FIELD_TYPES.get(field) == type;
	}

	/**
	 * Get the joined fields ({@link org.yop.orm.annotations.JoinColumn} and {@link org.yop.orm.annotations.JoinTable}).
	 * @param clazz the given class
	 * @return all the @JoinColumn/@JoinTable fields from the given class
	 */
	static Collection<Field> getJoinedFields(Class clazz) {
		if (! JOINED_FIELDS.containsKey(clazz)) {
			JOINED_FIELDS.putAll(clazz, JoinUtil.joinedFields(clazz));
		}
		return JOINED_FIELDS.get(clazz);
	}
}
