package org.yop.orm.sql;

import java.util.*;

/**
 * SQL query parameters.
 * <br>
 * This is an array list, the index of the parameter in the SQL query is the parameter index in the list + 1.
 */
public class Parameters extends ArrayList<Parameters.Parameter> {

	/**
	 * Add a new SQL parameter
	 * @param name  the SQL parameter name (will be displayed in the logs if show_sql = true)
	 * @param value the SQL parameter value
	 * @return the current Parameters object, for chaining purposes
	 */
	public Parameters addParameter(String name, Object value) {
		this.add(new Parameters.Parameter(name, value, false));
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
			return name;
		}

		public Object getValue() {
			return this.value instanceof DelayedValue ? ((DelayedValue) this.value).getDelayedValue() : this.value;
		}

		public boolean isSequence() {
			return sequence;
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
			return sequence ? String.valueOf(this.value) : "?";
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