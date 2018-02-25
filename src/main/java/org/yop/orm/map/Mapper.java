package org.yop.orm.map;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.exception.YopMapperException;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Results;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Maps results of an SQL query to a target class.
 */
public class Mapper {

	private static final String SEPARATOR = Context.SQL_SEPARATOR;

	public static <T extends Yopable> Set<T> map(Results results, Class<T> clazz) {
		try {
			return map(results, clazz, clazz.getSimpleName());
		} catch (IllegalAccessException | InstantiationException e) {
			throw new YopMapperException("Error mapping resultset to [" + clazz.getName() + "]", e);
		} catch (SQLException e) {
			throw new YopSQLException("An SQL error occured mapping resultset to [" + clazz.getName() + "]", e);
		}
	}

	private static <T extends Yopable> Set<T> map(
		Results results,
		Class<T> clazz,
		String context)
		throws IllegalAccessException, InstantiationException, SQLException {

		Set<T> out = new HashSet<>();
		while (results.getResultSet().next()) {
			T element = clazz.newInstance();
			element = mapSimpleFields(results, element, context);
			element = cycleBreaker(element, out);
			element = mapRelationFields(results, element, context);
			out.add(element);
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static <T> T mapSimpleFields(
		Results results,
		T element,
		String context)
		throws SQLException, IllegalAccessException {

		List<Field> fields = Reflection.getFields(element.getClass(), Column.class);
		for (Field field : fields) {
			String columnName = field.getAnnotation(Column.class).name();
			columnName = context + SEPARATOR + columnName;
			columnName = results.getQuery().getAlias(columnName);

			if(field.getType().isEnum()) {
				setEnumValue(results.getResultSet(), field, columnName, element);
			} else {
				field.set(element, results.getResultSet().getObject(columnName, field.getType()));
			}
		}
		return element;
	}

	/**
	 * Map an enum value onto a field of an element
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
					enumField.set(element, Enum.valueOf(enumType, String.valueOf(value))
					);
					break;
				case ORDINAL:
					// Just in case an ordinal is stored as a string... A bit preposterous !
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

	@SuppressWarnings("unchecked")
	private static <T> T mapRelationFields(
		Results results,
		T element,
		String context)
		throws SQLException, IllegalAccessException, InstantiationException {

		List<Field> fields = Reflection.getFields(element.getClass(), JoinTable.class, false);
		for (Field field : fields) {
			String newContext = context + SEPARATOR + field.getName() + SEPARATOR;
			Yopable target;
			if(Collection.class.isAssignableFrom(field.getType())) {
				Class<?> targetClass = getRelationFieldType(field);
				target = (Yopable) targetClass.newInstance();
				newContext += target.getClass().getSimpleName();

				if(noContext(results, newContext)) continue;

				mapSimpleFields(results, target, newContext);
				target = cycleBreaker(target, (Set) field.get(element));
				mapRelationFields(results, target, newContext);
			} else if (Yopable.class.isAssignableFrom(field.getType())){
				target = (Yopable) field.get(element);
				if(target == null) {
					target = (Yopable) field.getType().newInstance();
				}

				newContext += target.getClass().getSimpleName();

				if(noContext(results, newContext)) continue;
				field.set(element, target);

				mapSimpleFields(results, target, newContext);
				mapRelationFields(results, target, newContext);
			} else {
				throw new YopMappingException(
					" Field type [" + field.getType().getName()
					+ "] @ [" + field.getDeclaringClass().getName() + "#" + field.getName()
					+ "] is unsupported. Sorry about that :-( "
				);
			}
		}
		return element;
	}

	private static boolean noContext(Results results, String context) throws SQLException {
		ResultSetMetaData rsmd = results.getResultSet().getMetaData();
		int columns = rsmd.getColumnCount();
		for (int x = 1; x <= columns; x++) {
			if (results.getQuery().getAlias(rsmd.getColumnLabel(x)).startsWith(context)) {
				return false;
			}
		}
		return true;
	}

	private static <T extends Yopable> T cycleBreaker(T element, Set<T> elements) {
		T checked = elements.stream().filter(element::equals).findAny().orElse(element);
		elements.add(checked);
		return checked;
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> getRelationFieldType(Field field) {
		return (Class<T>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
	}
}
