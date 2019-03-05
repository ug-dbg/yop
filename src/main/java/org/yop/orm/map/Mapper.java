package org.yop.orm.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.Column;
import org.yop.orm.exception.YopMapperException;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Results;
import org.yop.orm.transform.ITransformer;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Maps results of an SQL query to a target class.
 * <br>
 * <b>This mapper is pretty basic for now</b>
 * <br>
 * A {@link FirstLevelCache} is used when reading a ResultSet, but it might not be very effective.
 */
public class Mapper {

	private static final Logger logger = LoggerFactory.getLogger(Mapper.class);

	/**
	 * Map the results of an SQL SELECT request on to a target class.
	 * <br>
	 * <b>⚠⚠⚠ This method iterates over the resultset ! ⚠⚠⚠</b>
	 * @param results the SQL query results (resultset + query + parameters...)
	 * @param clazz   the target class
	 * @param cache   First level cache to use when mapping objects. The idea is to share it among requests.
	 * @param <T>     the target type
	 * @return a set of T, read from the result set
	 */
	public static <T extends Yopable> Set<T> map(Results results, Class<T> clazz, FirstLevelCache cache) {
		try {
			return map(results, clazz, ORMUtil.getTargetName(clazz), cache);
		} catch (IllegalAccessException e) {
			throw new YopMapperException("Error mapping resultset to [" + clazz.getName() + "]", e);
		} catch (YopSQLException e) {
			throw new YopSQLException(
				"An SQL error occurred mapping resultset to [" + clazz.getName() + "]",
				results.getQuery(),
				e
			);
		}
	}

	/**
	 * Map the results of an SQL SELECT request on to a target class, starting from a root context.
	 * <br>
	 * <b>⚠⚠⚠ This method iterates over the resultset ! ⚠⚠⚠</b>
	 * <br><br>
	 * Query data order is preserved : this method returns a {@link LinkedHashSet}.
	 * @param results the results of the query
	 * @param clazz   the target class
	 * @param context the root context (mostly, the simple name of the target class)
	 * @param cache   First level cache to use
	 * @param <T>     the target type
	 * @return a {@link LinkedHashSet} of Ts from the result set. (The order can be quite important with an ORDER BY).
	 * @throws IllegalAccessException could not read a field
	 * @throws YopSQLException        error reading the resultset
	 */
	private static <T extends Yopable> Set<T> map(
		Results results,
		Class<T> clazz,
		String context,
		FirstLevelCache cache)
		throws IllegalAccessException {

		Map<Comparable, T> out = new LinkedHashMap<>();
		while (results.getCursor().next()) {
			T element = Reflection.newInstanceNoArgs(clazz);
			element = mapSimpleFields(results, element, context, cache);
			element = searchForSelf(element, out, cache);
			mapRelationFields(results, element, context, cache);
			out.put(element.getId(), element);
		}
		return new LinkedHashSet<>(out.values());
	}

	/**
	 * Map simple fields from a Resultset line for a given context.
	 * <br>
	 * This method checks the cache first.
	 * <br>
	 * This method iterates over the @Column fields of the target and search for data in the resultset entry.
	 * <br>
	 * <b>⚠⚠⚠ This method DOES NOT iterate over the resultset ! ⚠⚠⚠</b>
	 * @param results the SQL query results
	 * @param element the target element
	 * @param context the target element context
	 * @param <T> the target type
	 * @return the input element, or the cached element of it !
	 * @throws YopMapperException Unable to map a field, because of an underlying exception
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Yopable> T mapSimpleFields(
		Results results,
		T element,
		String context,
		FirstLevelCache cache) {

		T fromCache = cache.tryCache(results, (Class<T>) element.getClass(), context);
		if(fromCache != null) {
			return fromCache;
		}

		List<Field> fields = ORMUtil.getFields(element.getClass(), Column.class);
		for (Field field : fields) {
			try {
				setFieldValue(field, element, context, results);
			} catch (RuntimeException e) {
				throw new YopMapperException(
					"Unable to map field [" + Reflection.fieldToString(field) + "] " +
					"for context [" + context + "] from result set",
					e
				);
			}
		}
		return cache.put(element);
	}

	/**
	 * Read the current results row for the given field and context.
	 * <br>
	 * <b>⚠⚠⚠ This method DOES NOT iterate over the resultset ! ⚠⚠⚠</b>
	 * @param results the SQL query results
	 * @param field   the target element field
	 * @param context the target element context
	 * @return the value for the field and context, at the current row of the results. Maybe null.
	 * @throws org.yop.orm.exception.YopSQLException an error occurred reading the resultset
	 */
	static Object read(Results results, Field field, String context) {
		String columnName = field.getAnnotation(Column.class).name();
		columnName = context + results.getQuery().getConfig().sqlSeparator() + columnName;
		String shortened = results.getQuery().getShortened(columnName);

		Object rawValue = results.getCursor().getObject(shortened);
		if (rawValue == null) {
			return null;
		}

		Class<?> fieldType = field.getType();
		ITransformer transformer = ORMUtil.getTransformerFor(field);
		try {
			return transformer.fromSQL(results.getCursor().getObject(shortened, fieldType), fieldType);
		} catch (YopSQLException | AbstractMethodError e) {
			logger.debug("Error mapping [{}] of type [{}]. Manual fallback.", columnName, fieldType);
			Object out = transformer.fromSQL(ITransformer.fallbackTransformer().fromSQL(rawValue, fieldType), fieldType);
			logger.debug("Mapping [{}] of type [{}]. Manual fallback success.", columnName, fieldType);
			return out;
		}
	}

	/**
	 * Map a given field from a Resultset line for a given context.
	 * <br>
	 * If the field @Column defines a {@link ITransformer}, this method uses it.
	 * <br>
	 * If it fails, it tries to use {@link org.yop.orm.transform.FallbackTransformer#fromSQL(Object, Class)} first.
	 * <br>
	 * <b>⚠⚠⚠ This method DOES NOT iterate over the resultset ! ⚠⚠⚠</b>
	 * @param field   the field to map
	 * @param element the target element
	 * @param context the target element context
	 * @param results the SQL query results
	 * @throws YopSQLException        an error occurred reading the resultset
	 * @throws org.yop.orm.exception.YopRuntimeException could not access a field on the target instance
	 */
	@SuppressWarnings("unchecked")
	private static void setFieldValue(Field field, Yopable element, String context, Results results) {
		Object value = read(results, field, context);
		if (value != null) {
			if (field.getType().isEnum()) {
				setEnumValue(field, value, element);
			} else {
				Reflection.set(field, element, value);
			}
		} else if (!field.getType().isPrimitive()){
			Reflection.set(field, element, null);
		}
	}

	/**
	 * Map an enum value onto a field of an element.
	 * <br>
	 * This method simply search the value of the field in the result set, the enum strategy from @Column
	 * and set the field value if some data was found in the resultset.
	 * <br>
	 * @param enumField  the enum field. TYPE MUST BE ENUM
	 * @param element    the element on which the field must be set
	 * @throws YopMapperException Error accessing the enum field on the element
	 */
	@SuppressWarnings("unchecked")
	private static void setEnumValue(Field enumField, Object value, Yopable element) {
		Column.EnumStrategy strategy = enumField.getAnnotation(Column.class).enum_strategy();
		Class<? extends Enum> enumType = (Class<? extends Enum>) enumField.getType();

		if(value == null) {
			Reflection.set(enumField, element, null);
			return;
		}

		try {
			switch (strategy) {
				case NAME:
					Reflection.set(enumField, element, Enum.valueOf(enumType, String.valueOf(value)));
					break;
				case ORDINAL:
					// Integer.valueOf(Objects.toString(val)) → ordinal is stored as a string... A bit preposterous !
					Reflection.set(
						enumField,
						element,
						enumType.getEnumConstants()[Integer.valueOf(Objects.toString(value))]
					);
					break;
				default:
					throw new YopMappingException("Unknown enum strategy [" + strategy.name() + "] !");
			}
		} catch (RuntimeException e) {
			throw new YopMapperException(
				"Could not map enum [" + strategy + ":" + value + "] on [" + enumType.getName() + "]",
				e
			);
		}
	}

	/**
	 * Map the relation fields of the target instance from the resultset entry.
	 * <br>
	 * This is where it gets a bit tricky. For every @JoinTable/@JoinTable field of the target element :
	 * <ol>
	 *     <li>build a new context : context = context→fieldName</li>
	 *     <li>
	 *         check field type :
	 *         <ul>
	 *             <li>Yopable → new instance and map data</li>
	 *             <li>Collection → check if new instance or if already exists, map and add to the collection</li>
	 *             <li>Other → {@link YopMappingException}, unsupported</li>
	 *         </ul>
	 *     </li>
	 *     <li>Recurse on the @JoinTable fields of the related instance</li>
	 * </ol>
	 * <br>
	 * <b>⚠⚠⚠ This method DOES NOT iterate over the resultset ! ⚠⚠⚠</b>
	 * @param results the results of the SQL SELECT query
	 * @param element the target element
	 * @param context the target element context
	 * @param <T> the target type
	 * @throws IllegalAccessException could not read a field
	 * @throws YopSQLException        error reading the resultset
	 * @throws YopMappingException    Incorrect mapping. Mostly a non Yopable/Collection of Yopable relationship.
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Yopable> void mapRelationFields(
		Results results,
		T element,
		String context,
		FirstLevelCache cache)
		throws IllegalAccessException {

		Collection<Field> fields = ORMUtil.getJoinedFields(element.getClass());
		String separator = results.getQuery().getConfig().sqlSeparator();
		for (Field field : fields) {
			String newContext = context + separator + field.getName() + separator;
			Yopable target;
			if(ORMUtil.isCollection(field)) {
				Class<? extends Yopable> targetClass = ORMUtil.getRelationFieldType(field);

				newContext += ORMUtil.getTargetName(targetClass);
				if(results.noContext(newContext, targetClass)) continue;

				target = Reflection.newInstanceNoArgs(targetClass);
				target = mapSimpleFields(results, target, newContext, cache);
				target = cache.getOrDefault(field, element, target);
				mapRelationFields(results, target, newContext, cache);
			} else if (ORMUtil.isYopable(field)){
				Class<? extends Yopable> targetClass = (Class<? extends Yopable>) field.getType();
				newContext += ORMUtil.getTargetName(targetClass);
				if(results.noContext(newContext, targetClass)) continue;

				target = (Yopable) ORMUtil.readField(field, element);
				if(target == null) {
					target = (Yopable) Reflection.newInstanceNoArgs(field.getType());
				}

				target = mapSimpleFields(results, target, newContext, cache);
				field.set(element, target);
				mapRelationFields(results, target, newContext, cache);
			} else {
				throw new YopMappingException(
					" Field type [" + field.getType().getName()
					+ "] @ [" + Reflection.fieldToString(field)
					+ "] is unsupported. Sorry about that :-( "
				);
			}
		}
	}

	/**
	 * Check for an element in the given collection. Add to the collection if does not exist.
	 * @param element  the element to find
	 * @param elements the elements to check into
	 * @param cache    the first level cache. If 'element' was not in 'elements', it gets added to the cache too.
	 * @param <T> the target type
	 * @return the found element or the input element after it is added in the collection
	 */
	private static <T extends Yopable> T searchForSelf(T element, Map<Comparable, T> elements, FirstLevelCache cache) {
		if(elements.containsKey(element.getId())) {
			return elements.get(element.getId());
		}
		elements.put(element.getId(), cache.put(element));
		return element;
	}
}
