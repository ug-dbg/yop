package org.yop.orm.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.exception.YopMapperException;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Results;
import org.yop.orm.transform.ITransformer;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
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

	private static final String SEPARATOR = Context.SQL_SEPARATOR;

	/**
	 * Map the results of an SQL SELECT request on to a target class.
	 * <br>
	 * <b>⚠⚠⚠ This method iterates over the resultset ! ⚠⚠⚠</b>
	 * @param results the SQL query results (resultset + query + parameters...)
	 * @param clazz   the target class
	 * @param <T>     the target type
	 * @return a set of T, read from the result set
	 */
	public static <T extends Yopable> Set<T> map(Results results, Class<T> clazz) {
		try {
			return map(results, clazz, clazz.getSimpleName());
		} catch (IllegalAccessException | InstantiationException e) {
			throw new YopMapperException("Error mapping resultset to [" + clazz.getName() + "]", e);
		} catch (SQLException e) {
			throw new YopSQLException("An SQL error occured mapping resultset to [" + clazz.getName() + "]", e);
		}
	}

	/**
	 * Map the results of an SQL SELECT request on to a target class, starting from a root context.
	 * <br>
	 * <b>⚠⚠⚠ This method iterates over the resultset ! ⚠⚠⚠</b>
	 * @param results the results of the query
	 * @param clazz   the target class
	 * @param context the root context (mostly, the simple name of the target class)
	 * @param <T>     the target type
	 * @return a set of T, read from the result set
	 * @throws IllegalAccessException could not read a field
	 * @throws InstantiationException could not instantiate a target element
	 * @throws SQLException           error reading the resultset
	 */
	private static <T extends Yopable> Set<T> map(
		Results results,
		Class<T> clazz,
		String context)
		throws IllegalAccessException, InstantiationException, SQLException {

		Set<T> out = new HashSet<>();
		FirstLevelCache cache = new FirstLevelCache();
		while (results.getResultSet().next()) {
			T element = clazz.newInstance();
			element = mapSimpleFields(results, element, context, cache);
			element = cycleBreaker(element, out, cache);
			mapRelationFields(results, element, context, cache);
			out.add(element);
		}
		return out;
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
	 * @throws SQLException           an error occured reading the resultset
	 * @throws IllegalAccessException could not access a field on the target instance
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Yopable> T mapSimpleFields(
		Results results,
		T element,
		String context,
		FirstLevelCache cache)
		throws SQLException, IllegalAccessException {

		T fromCache = cache.tryCache(results, (Class<T>) element.getClass(), context);
		if(fromCache != null) {
			return fromCache;
		}

		List<Field> fields = Reflection.getFields(element.getClass(), Column.class);
		for (Field field : fields) {
			String columnName = field.getAnnotation(Column.class).name();
			columnName = context + SEPARATOR + columnName;
			String shortened = results.getQuery().getShortened(columnName);
			Class<?> fieldType = field.getType();

			if (results.getResultSet().getObject(shortened) != null) {
				if (fieldType.isEnum()) {
					setEnumValue(results.getResultSet(), field, shortened, element);
				} else {
					ITransformer transformer = ORMUtil.getTransformerFor(field);
					try {
						field.set(element, transformer.fromSQL(
							results.getResultSet().getObject(shortened, fieldType),
							fieldType)
						);
					} catch (SQLException | AbstractMethodError e) {
						logger.debug("Error mapping [{}] of type [{}]. Manual fallback.", columnName, fieldType);
						Object object = results.getResultSet().getObject(shortened);
						field.set(element, transformer.fromSQL(
							ITransformer.fallbackTransformer().fromSQL(object, fieldType),
							fieldType)
						);
						logger.debug("Mapping [{}] of type [{}]. Manual fallback success.", columnName, fieldType);
					}
				}
			} else if (!fieldType.isPrimitive()){
				field.set(element, null);
			}
		}
		return cache.put(element);
	}

	/**
	 * Map an enum value onto a field of an element.
	 * <br>
	 * This method simply search the value of the field in the result set, the enum strategy from @Column
	 * and set the field value if some data was found in the resultset.
	 * <br>
	 * <b>⚠⚠⚠ This method DOES NOT iterate over the resultset ! ⚠⚠⚠</b>
	 * @param resultSet  the resultset to read
	 * @param enumField  the enum field. TYPE MUST BE ENUM
	 * @param columnName the column name to read in the result set
	 * @param element    the element on which the field must be set
	 * @throws SQLException           Error reading the result set
	 * @throws IllegalAccessException Error accessing the enum field on the element
	 */
	@SuppressWarnings("unchecked")
	private static void setEnumValue(
		ResultSet resultSet,
		Field enumField,
		String columnName,
		Object element)
		throws SQLException, IllegalAccessException {

		Column.EnumStrategy strategy = enumField.getAnnotation(Column.class).enum_strategy();
		Class<? extends Enum> enumType = (Class<? extends Enum>) enumField.getType();
		Object value = resultSet.getObject(columnName);

		if(value == null) {
			enumField.set(element, null);
			return;
		}

		try {
			switch (strategy) {
				case NAME:
					enumField.set(element, Enum.valueOf(enumType, String.valueOf(value)));
					break;
				case ORDINAL:
					// Integer.valueOf(Objects.toString(val)) → ordinal is stored as a string... A bit preposterous !
					enumField.set(element, enumType.getEnumConstants()[Integer.valueOf(Objects.toString(value))]);
					break;
				default:
					throw new YopMappingException("Unknown enum strategy [" + strategy.name() + "] !");
			}
		} catch (RuntimeException e) {
			throw new YopMapperException(
				"Could not map enum [" + strategy + ":" + value + "] on [" + enumType.getName() + "]"
			);
		}
	}

	/**
	 * Map the relation fields of the target instance from the resultset entry.
	 * <br>
	 * This is where it gets a bit tricky. For every @JoinTable field of the target element :
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
	 * @throws InstantiationException could not instantiate a target element
	 * @throws SQLException           error reading the resultset
	 * @throws YopMappingException    Incorrect mapping. Mostly a non Yopable/Collection of Yopable relationship.
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Yopable> void mapRelationFields(
		Results results,
		T element,
		String context,
		FirstLevelCache cache)
		throws SQLException, IllegalAccessException, InstantiationException {

		List<Field> fields = Reflection.getFields(element.getClass(), JoinTable.class, false);
		for (Field field : fields) {
			String newContext = context + SEPARATOR + field.getName() + SEPARATOR;
			Yopable target;
			if(Collection.class.isAssignableFrom(field.getType())) {
				Class<? extends Yopable> targetClass = getRelationFieldType(field);
				target = targetClass.newInstance();

				newContext += target.getClass().getSimpleName();
				if(noContext(results, newContext, targetClass)) continue;

				target = mapSimpleFields(results, target, newContext, cache);
				target = cycleBreaker(target, (Collection) field.get(element), cache);
				mapRelationFields(results, target, newContext, cache);
			} else if (Yopable.class.isAssignableFrom(field.getType())){
				target = (Yopable) ORMUtil.readField(field, element);
				if(target == null) {
					target = (Yopable) field.getType().newInstance();
				}

				newContext += target.getClass().getSimpleName();
				if(noContext(results, newContext, target.getClass())) continue;

				target = mapSimpleFields(results, target, newContext, cache);
				field.set(element, target);
				mapRelationFields(results, target, newContext, cache);
			} else {
				throw new YopMappingException(
					" Field type [" + field.getType().getName()
					+ "] @ [" + field.getDeclaringClass().getName() + "#" + field.getName()
					+ "] is unsupported. Sorry about that :-( "
				);
			}
		}
	}

	/**
	 * Check if there is some data for the given context or not.
	 * <b>⚠⚠⚠ This method DOES NOT iterate over the resultset ! ⚠⚠⚠</b>
	 * @param results     the resultset entry to check
	 * @param context     the context to check
	 * @param targetClass the target class
	 * @return true if there are no column AND no data for the given context
	 * @throws SQLException error reading the resultset
	 */
	static boolean noContext(
		Results results,
		String context,
		Class<? extends Yopable> targetClass)
		throws SQLException {

		String idColumn = results.getQuery().getShortened(context + SEPARATOR + ORMUtil.getIdColumn(targetClass));
		ResultSetMetaData rsmd = results.getResultSet().getMetaData();
		if(!hasColumn(rsmd, idColumn)) {
			return true;
		}

		if(results.getResultSet().getObject(idColumn) == null) {
			return true;
		}

		int columns = rsmd.getColumnCount();
		for (int x = 1; x <= columns; x++) {
			if (results.getQuery().getAlias(rsmd.getColumnLabel(x)).startsWith(context)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Check for an element in the given collection. Add to the collection if does not exist.
	 * @param element  the element to find
	 * @param elements the elements to check into
	 * @param <T> the target type
	 * @return the found element or the input element after it is added in the collection
	 */
	private static <T extends Yopable> T cycleBreaker(T element, Collection<T> elements, FirstLevelCache cache) {
		Optional<T> any = elements.stream().filter(element::equals).findFirst();
		if(any.isPresent()) {
			return any.get();
		}
		elements.add(cache.put(element));
		return element;
	}

	/**
	 * Read the target type of a Collection relationship.
	 * <br>
	 * Example : {@code ArrayList<Pojo> → Pojo.class}
	 * @param field the field to read
	 * @param <T> the target type
	 * @return the target class
	 */
	@SuppressWarnings("unchecked")
	static <T> Class<T> getRelationFieldType(Field field) {
		return (Class<T>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
	}

	/**
	 * Check if a given column is present in the resultset
	 * @param rsmd       the resultset metadata
	 * @param columnName the column name
	 * @return true if the column is present in the resultset
	 * @throws SQLException error reading the resultset
	 */
	private static boolean hasColumn(ResultSetMetaData rsmd, String columnName) throws SQLException {
		int columns = rsmd.getColumnCount();
		for (int x = 1; x <= columns; x++) {
			if (columnName.equals(rsmd.getColumnLabel(x))) {
				return true;
			}
		}
		return false;
	}
}
