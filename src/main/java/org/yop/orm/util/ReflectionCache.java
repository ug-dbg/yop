package org.yop.orm.util;

import org.reflections.Reflections;
import org.yop.orm.model.Yopable;

import java.lang.reflect.Field;
import java.util.*;

/**
 * A static cache for anything related to {@link Reflection}.
 * <br>
 * {@link java.lang.reflect} methods can take a bit of time. Let's try to use some cache !
 */
class ReflectionCache {

	/** Declared fields for a given class */
	private static final Map<Class, Collection<Field>> DECLARED_FIELDS = new HashMap<>();

	/** {@link ORMUtil#joinedFields(Class)} fields for a given class */
	private static final Map<Class, Collection<Field>> JOINED_FIELDS   = new HashMap<>();

	/** Is this field a collection field, a Yopable field or something else ? */
	private static final Map<Field, Reflection.FieldType> FIELD_TYPES  = new HashMap<>();

	/**
	 * Reference implementations for common interfaces
	 */
	private static final Map<Class<?>, Class<?>> KNOWN_IMPLEMENTATIONS = new HashMap<Class<?>, Class<?>>() {{
		this.put(Iterable.class,   ArrayList.class);
		this.put(Collection.class, ArrayList.class);
		this.put(List.class,       ArrayList.class);
		this.put(Set.class,        HashSet.class);
		this.put(Queue.class,      LinkedList.class);
		this.put(Map.class,        HashMap.class);
		this.put(Calendar.class,   GregorianCalendar.class);
	}};

	static Collection<Field> getDeclaredFields(Class clazz) {
		if (! DECLARED_FIELDS.containsKey(clazz)) {
			DECLARED_FIELDS.put(clazz, Arrays.asList(clazz.getDeclaredFields()));
		}
		return DECLARED_FIELDS.get(clazz);
	}

	/**
	 * Is this field a {@link Reflection.FieldType#COLLECTION} ?
	 * @param field the field to check
	 * @return true if a {@link Collection} is assignable from the field type.
	 */
	static boolean isCollection(Field field) {
		if (! FIELD_TYPES.containsKey(field)) {
			FIELD_TYPES.put(field, Reflection.FieldType.fromField(field));
		}
		return FIELD_TYPES.get(field) == Reflection.FieldType.COLLECTION;
	}

	/**
	 * Is this field a {@link Reflection.FieldType#YOPABLE} ?
	 * @param field the field to check
	 * @return true if a {@link Yopable} is assignable from the field type.
	 */
	static boolean isYopable(Field field) {
		if (! FIELD_TYPES.containsKey(field)) {
			FIELD_TYPES.put(field, Reflection.FieldType.fromField(field));
		}
		return FIELD_TYPES.get(field) == Reflection.FieldType.YOPABLE;
	}

	/**
	 * Get the joined fields ({@link org.yop.orm.annotations.JoinColumn} and {@link org.yop.orm.annotations.JoinTable}).
	 * @param clazz the given class
	 * @return all the @JoinColumn/@JoinTable fields from the given class
	 */
	static Collection<Field> getJoinedFields(Class clazz) {
		if (! JOINED_FIELDS.containsKey(clazz)) {
			JOINED_FIELDS.put(clazz, ORMUtil.joinedFields(clazz));
		}
		return JOINED_FIELDS.get(clazz);
	}

	/**
	 * Returns the first known implementation of a class.
	 * <br>
	 * It can be itself if the class {@link Reflection#isConcrete(Class)}.
	 * <br>
	 * Several strategies :
	 * <ul>
	 *     <li>concrete → return the class itself</li>
	 *     <li>{@link #KNOWN_IMPLEMENTATIONS} has a reference implementation → go for it</li>
	 *     <li>
	 *         else → use {@link Reflections} to find all the subtypes in the whole context.
	 *         <br>
	 *         Take the first concrete sub type, add it to the {@link #KNOWN_IMPLEMENTATIONS} and return !
	 *     </li>
	 * </ul>
	 * @param clazz the class whose implementation is sought
	 * @param <T> the class generic type
	 * @return the first implementation found, self if concrete, null if no known implementation
	 */
	@SuppressWarnings("unchecked")
	static <T> Class<? extends T> implementationOf(Class<T> clazz) {
		if(Reflection.isConcrete(clazz)) {
			return clazz;
		}

		if(KNOWN_IMPLEMENTATIONS.containsKey(clazz)){
			return (Class<? extends T>) KNOWN_IMPLEMENTATIONS.get(clazz);
		}

		Set<Class<? extends T>> subTypes = new Reflections().getSubTypesOf(clazz);
		Class<? extends T> impl = subTypes.stream().filter(Reflection::isConcrete).findFirst().orElse(null);
		KNOWN_IMPLEMENTATIONS.put(clazz, impl);
		return impl;
	}
}
