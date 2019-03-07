package org.yop.orm.transform;

import org.yop.orm.annotations.Column;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.reflection.Reflection;

import java.util.HashMap;
import java.util.Map;

/**
 * A transformer is set on a field using the {@link org.yop.orm.annotations.Column} annotation.
 * <br>
 * Then :
 * <ul>
 *     <li>when setting the field value as a query parameter → {@link #forSQL(Object, Column)}</li>
 *     <li>when reading a JDBC value for the field → {@link #fromSQL(Object, Class)}</li>
 * </ul>
 * @param <What> the field type
 */
public interface ITransformer<What> {

	/**
	 * Transformers instances (singletons)
	 */
	Map<Class, ITransformer> INSTANCES = new HashMap<>();

	/**
	 * Transform the field value into anything else for SQL querying
	 * @param what   the field value to transform
	 * @param column the column annotation, if you need to read some info (e.g. max length)
	 * @return the transformed value
	 */
	Object forSQL(What what, Column column);

	/**
	 * Transform a JDBC value for the field to be set
	 * @param fromJDBC the JDBC value, from query result
	 * @param into     the target field type
	 * @return the transformed value
	 */
	What fromSQL(Object fromJDBC, Class into);

	/**
	 * Find the singleton instance of the given transformer class.
	 * If it does not exist in {@link #INSTANCES}, instantiate the transformer, add it the map and return the instance.
	 * @param clazz the transformer class
	 * @return the transformer instance.
	 */
	static ITransformer getTransformer(Class<? extends ITransformer> clazz) {
		if (! INSTANCES.containsKey(clazz)) {
			try {
				INSTANCES.put(clazz, Reflection.newInstanceNoArgs(clazz));
			} catch (RuntimeException e) {
				throw new YopRuntimeException("Could not instantiate transformer [" + clazz.getName() + "]", e);
			}
		}
		return INSTANCES.get(clazz);
	}

	/**
	 * A fall back transformer is used when the {@link java.sql.ResultSet#getObject(int, Class)} fails.
	 * @return the {@link FallbackTransformer} instance from the singletons map
	 */
	static ITransformer<Object> fallbackTransformer() {
		return FallbackTransformer.INSTANCE;
	}

	/**
	 * A void transformer does nothing and returns the parameter object to transform 'as is'.
	 * @return the singleton instance of the {@link VoidTransformer}
	 */
	static ITransformer<Object> voidTransformer() {
		return VoidTransformer.INSTANCE;
	}

}
