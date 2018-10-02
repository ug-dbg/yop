package org.yop.orm.util;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.*;

/**
 * A static cache for anything related to {@link Reflection}.
 * <br>
 * {@link java.lang.reflect} methods can take a bit of time. Let's try to use some cache !
 */
class ReflectionCache {

	/** Declared fields for a given class */
	private static final MultiValuedMap<Class, Field> DECLARED_FIELDS = new ArrayListValuedHashMap<>();

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
			DECLARED_FIELDS.putAll(clazz, Arrays.asList(clazz.getDeclaredFields()));
		}
		return DECLARED_FIELDS.get(clazz);
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
