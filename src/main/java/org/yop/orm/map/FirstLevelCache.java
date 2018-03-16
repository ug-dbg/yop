package org.yop.orm.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Constants;
import org.yop.orm.sql.Results;
import org.yop.orm.util.ORMUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * A very basic cache mechanism.
 * <br>
 * It relies on a double map and cache key for an object is (class, id).
 */
public class FirstLevelCache {

	private static final Logger logger = LoggerFactory.getLogger(FirstLevelCache.class);

	/** The cache map */
	private Map<Class<? extends Yopable>, Map<Long, Yopable>> cache = new HashMap<>();

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
}
