package org.yop.orm.sql;

import org.yop.orm.annotations.Column;
import org.yop.orm.exception.YopMapperException;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * SQL query parameters.
 * <br>
 * This is an array list, the index of the parameter in the SQL query is the parameter index in the list + 1.
 */
public class Parameters extends ArrayList<Parameters.Parameter> {

	/**
	 * Add a new SQL parameter that is not a sequence.
	 * <br>
	 * See {@link #parameterForField(String, Object, Field, boolean, Config)}.
	 * @param name  the SQL parameter name (will be displayed in the logs if show_sql = true)
	 * @param value the SQL parameter value
	 * @param field the field associated to the value. Required to check enum/transformer strategies. Might be null.
	 * @param seq   is this parameter a sequence parameter ?
	 * @param config  the SQL config. Might be required to get the default column length.
	 * @return the current Parameters object, for chaining purposes
	 */
	public Parameters addParameter(String name, Object value, Field field, boolean seq, Config config) {
		this.add(parameterForField(name, value, field, seq, config));
		return this;
	}

	/**
	 * Add a new SQL parameter that is a {@link DelayedValue}.
	 * @param name  the SQL parameter name (will be displayed in the logs if show_sql = true)
	 * @param value the SQL parameter delayed value
	 * @return the current Parameters object, for chaining purposes
	 */
	public Parameters addParameter(String name, DelayedValue value) {
		this.add(new Parameters.Parameter(name, value, null, false));
		return this;
	}

	/**
	 * Create a new SQL parameter.
	 * <br>
	 * If the field associated to the value is not null, its column configuration is read and configurations :
	 * <ul>
	 *     <li>{@link Column#enum_strategy()}</li>
	 *     <li>{@link Column#transformer()}</li>
	 *     <li>{@link Column#not_null()}</li>
	 * </ul>
	 * will be used when applicable.
	 * @param name  the SQL parameter name (will be displayed in the logs if show_sql = true)
	 * @param value the SQL parameter value
	 * @param field the field associated to the value. Required to check enum/transformer strategies. Might be null.
	 * @param seq   is this parameter a sequence parameter ?
	 * @param config  the SQL config. Might be required to get the default column length.
	 * @return the new Parameter instance
	 */
	@SuppressWarnings("unchecked")
	private static Parameter parameterForField(String name, Object value, Field field, boolean seq, Config config) {
		Object parameterValue = value;

		if (field != null && field.isAnnotationPresent(Column.class)) {
			Column column = field.getAnnotation(Column.class);

			if (column.not_null() && value == null) {
				throw new YopMapperException(
					"Field [" + Reflection.fieldToString(field) + "] is marked with @Column not null. "
					+ "Parameter [" + name + "] value is null !"
				);
			}

			if (value instanceof Enum) {
				Column.EnumStrategy enumStrategy = column.enum_strategy();
				switch (enumStrategy) {
					case ORDINAL: parameterValue = ((Enum) value).ordinal(); break;
					case NAME:    parameterValue = ((Enum) value).name();    break;
					default: throw new YopMapperException("Unknown enum strategy [" + enumStrategy.name() + "] !");
				}
			}
			parameterValue = ORMUtil.getTransformerFor(field).forSQL(parameterValue, column, config);

		}
		return new Parameters.Parameter(name, parameterValue, field, seq);
	}

	/**
	 * An SQL parameter ('?' values in SQL queries)
	 */
	public static class Parameter {
		final String name;
		final Object value;
		final boolean sequence;
		Field field;

		private Parameter(String name, Object value, Field field, boolean sequence) {
			this.name = name;
			this.value = value;
			this.sequence = sequence;
			this.field = field;
		}

		public String getName() {
			return this.name;
		}

		public Object getValue() {
			return this.value instanceof DelayedValue ? ((DelayedValue) this.value).getDelayedValue() : this.value;
		}

		public boolean isSequence() {
			return this.sequence;
		}

		public Field getField() {
			return this.field;
		}

		@Override
		public String toString() {
			return "Parameter{" + "name='" + this.name + '\'' + ", value=" + this.value + '}';
		}
	}

	/**
	 * A delayed value can be set as a {@link Parameter#value}.
	 * <br>
	 * When {@link Parameter#getValue()} is called, the delayed value method is used.
	 * <br>
	 * This is useful for queries where parameters values are IDs that are not yet generated.
	 */
	@FunctionalInterface
	public interface DelayedValue {
		Object getDelayedValue();
	}
}
