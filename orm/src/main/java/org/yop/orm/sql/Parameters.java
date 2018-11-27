package org.yop.orm.sql;

import org.yop.orm.annotations.Column;
import org.yop.orm.exception.YopMapperException;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * SQL query parameters.
 * <br>
 * This is an array list, the index of the parameter in the SQL query is the parameter index in the list + 1.
 */
public class Parameters extends ArrayList<Parameters.Parameter> {

	/**
	 * Return the parameters names, using {@link Parameter#getName()}.
	 * @param includeIf an inclusion predicate : if the parameter does not match the predicate, it won't be returned
	 * @return the list of names of the parameters that matched the predicate
	 */
	public List<String> names(Predicate<Parameter> includeIf) {
		return this.stream().filter(includeIf).map(Parameters.Parameter::getName).collect(Collectors.toList());
	}

	/**
	 * Return the parameters values, using {@link Parameter#toSQLValue()}.
	 * <br>
	 * This is likely to return a collection of '?'.
	 * @param includeIf an inclusion predicate : if the parameter does not match the predicate, it won't be returned
	 * @return the list of values of the parameters that matched the predicate
	 */
	public List<String> values(Predicate<Parameter> includeIf) {
		return this.stream().filter(includeIf).map(Parameters.Parameter::toSQLValue).collect(Collectors.toList());
	}

	/**
	 * Return the parameters names and values, using {@link Parameter#toSQLValue()}.
	 * <br>
	 * This is likely to return something like [parameter1=?, parameter2=?, ...]
	 * @param includeIf an inclusion predicate : if the parameter does not match the predicate, it won't be returned
	 * @return the list of (name1=value1, name2=value2) of the parameters that matched the predicate
	 */
	public List<String> namesAndValues(Predicate<Parameter> includeIf) {
		return this.stream().filter(includeIf).map(p -> p.getName() + "=" + p.toSQLValue()).collect(Collectors.toList());
	}

	/**
	 * Move a parameter to the end of the list.
	 * <br>
	 * The first parameter whose name equals 'name' will be removed and added to the end of the list.
	 * @param name the parameter name
	 */
	public void moveLast(String name) {
		Optional<Parameter> columnParameter = this.stream().filter(p -> p.getName().equals(name)).findFirst();
		if (columnParameter.isPresent()) {
			this.remove(columnParameter.get());
			this.add(columnParameter.get());
		}
	}

	/**
	 * Add a new SQL parameter.
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
	 * @return the current Parameters object, for chaining purposes
	 */
	@SuppressWarnings("unchecked")
	public Parameters addParameter(String name, Object value, Field field) {
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
			parameterValue = ORMUtil.getTransformerFor(field).forSQL(parameterValue, column);

		}
		this.add(new Parameters.Parameter(name, parameterValue, false));
		return this;
	}

	/**
	 * Add a new SQL parameter that is a {@link DelayedValue}.
	 * @param name  the SQL parameter name (will be displayed in the logs if show_sql = true)
	 * @param value the SQL parameter delayed value
	 * @return the current Parameters object, for chaining purposes
	 */
	public Parameters addParameter(String name, DelayedValue value) {
		this.add(new Parameters.Parameter(name, value, false));
		return this;
	}

	/**
	 * Add a new SQL sequence parameter.
	 * A sequence parameter will not be added as a JDBC param but must be explicitly written in the SQL query.
	 * @param name  the SQL parameter name (will be displayed in the logs if show_sql = true)
	 * @param value the SQL parameter value
	 * @return the current Parameters object, for chaining purposes
	 */
	public Parameters addSequenceParameter(String name, Object value) {
		this.add(new Parameters.Parameter(name, value, true));
		return this;
	}

	/**
	 * An SQL parameter ('?' values in SQL queries)
	 */
	public static class Parameter {
		final String name;
		final Object value;
		final boolean sequence;

		Parameter(String name, Object value, boolean sequence) {
			this.name = name;
			this.value = value;
			this.sequence = sequence;
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

		/**
		 * Returns the parameter value that can be added to the SQL query.
		 * <ul>
		 *     <li>parameter is a sequence parameter → {@link #value}</li>
		 *     <li>else → '?'</li>
		 * </ul>
		 * @return the parameter SQL value
		 */
		public String toSQLValue() {
			return this.sequence ? String.valueOf(this.value) : "?";
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
	public interface DelayedValue {
		Object getDelayedValue();
	}
}
