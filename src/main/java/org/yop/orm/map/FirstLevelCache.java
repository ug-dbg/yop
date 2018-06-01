package org.yop.orm.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Constants;
import org.yop.orm.sql.Results;
import org.yop.orm.util.ORMUtil;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A very basic cache mechanism.
 * You will find here :
 * <ul>
 *     <li>a {@link Yopable} objects cache, a double map whose key for a given object is : [class, id]</li>
 *     <li>an association cache, a triple map whose key for an associated object is [field, source id, target id]</li>
 * </ul>
 */
public class FirstLevelCache {

	private static final Logger logger = LoggerFactory.getLogger(FirstLevelCache.class);

	/** The {@link Yopable} objects cache map. Cache key for an object is : [class, id] */
	private final Map<Class<? extends Yopable>, Map<Long, Yopable>> cache = new HashMap<>();

	/** Association cache : for a given collection field, then a Yopable ID → a map of associated objects, by ID */
	private final Map<Field, Map<Long, Map<Long, Yopable>>> associationsCache = new HashMap<>();

	/**
	 * Try to hit the cache.
	 * <br>
	 * <b>Return null if there is nothing in the cache !</b>
	 * @param results the query results
	 * @param clazz   the target class
	 * @param context the current context
	 * @param <T> the target type
	 * @return the value from the cache, or null
	 * @throws org.yop.orm.exception.YopSQLException an error occurred reading the resultset
	 */
	public <T extends Yopable> T tryCache(Results results, Class<T> clazz, String context) {
		String idShortened = results.getQuery().getShortened(
			context + Constants.SQL_SEPARATOR + ORMUtil.getIdColumn(clazz)
		);
		long id = results.getCursor().getLong(idShortened);
		if(this.has(clazz, id)) {
			return this.get(clazz, id);
		}
		return null;
	}

	/**
	 * Does the cache have an entry for my object ?
	 * @param clazz the target class
	 * @param id    the object ID
	 * @return true if there is a cache entry
	 */
	public boolean has(Class<? extends Yopable> clazz, Long id) {
		return this.cache.containsKey(clazz) && this.cache.get(clazz).containsKey(id);
	}

	/**
	 * Get the cache entry. Please check if there is some cache first ! No control is done here !
	 * @param clazz the target class
	 * @param id    the object ID
	 * @param <T> the target type
	 * @return the cache entry
	 * @throws NullPointerException if there is no cache entry for the element class
	 */
	@SuppressWarnings("unchecked")
	public <T extends Yopable> T get(Class<T> clazz, Long id) {
		logger.trace("Cache hit for [{}#{}]", clazz.getName(), id);
		return (T) this.cache.get(clazz).get(id);
	}

	/**
	 * Add a cache entry.
	 * @param element the element to cache
	 * @param <T> the target type
	 * @return the cached element, for chaining purposes
	 */
	public <T extends Yopable> T put(T element) {
		if(element == null) {
			return null;
		}

		if (!this.cache.containsKey(element.getClass())) {
			this.cache.put(element.getClass(), new HashMap<>());
		}

		this.cache.get(element.getClass()).putIfAbsent(element.getId(), element);
		return element;
	}

	/**
	 * Search for the target element into the cache associated
	 * to the given {@link org.yop.orm.util.Reflection.FieldType#COLLECTION} field and the given source.
	 * <br>
	 * If not found, add the target to the source object (through the association field) and to association cache.
	 * <br>
	 * The name of this method is after {@link Map#getOrDefault(Object, Object)}. This might not be a good idea.
	 * @param collectionField the collection field (from the source object).
	 * @param source          the source object
	 * @param target          the target object
	 * @param <T> the target type
	 * @return the element from cache.
	 * @throws IllegalAccessException could not read the collection field
	 */
	@SuppressWarnings("unchecked")
	public <T extends Yopable> T getOrDefault(
		Field collectionField,
		Yopable source,
		T target)
		throws IllegalAccessException {

		if (! this.associationsCache.containsKey(collectionField)) {
			this.associationsCache.put(collectionField, new HashMap<>());
		}

		if (! this.associationsCache.get(collectionField).containsKey(source.getId())) {
			this.associationsCache.get(collectionField).put(source.getId(), new HashMap<>());
		}

		Map<Long, Yopable> fieldValueAsMap = this.associationsCache.get(collectionField).get(source.getId());
		if (! fieldValueAsMap.containsKey(target.getId())) {
			((Collection) collectionField.get(source)).add(target);
			fieldValueAsMap.put(target.getId(), target);
		}
		return (T) fieldValueAsMap.get(target.getId());
	}
}
