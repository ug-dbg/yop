package org.yop.orm.sql;

import java.util.*;

/**
 * SQL query parameters + an other map of too long aliases.
 * <br>
 * This is an array list, the index of the parameter in the SQL query is the parameter index in the list + 1.
 */
public class Parameters extends ArrayList<Parameters.Parameter> {

	private boolean askGeneratedKeys = false;

	private Set<Long> generatedIds = new HashSet<>();

	/** Aliases map : short alias → original alias */
	private Map<String, String> tooLongAliases = new HashMap<>();

	/** Add an alias replacement */
	public void addTooLongAlias(String alias, String shortened) {
		this.tooLongAliases.put(shortened, alias);
	}

	/** Get the original alias for a shortened one. Return the given parameter if no alias entry */
	public String getAlias(String shortened) {
		return this.tooLongAliases.getOrDefault(shortened, shortened);
	}

	/**
	 * Add a new SQL parameter
	 * @param name  the SQL parameter name (will be displayed in the logs if show_sql = true)
	 * @param value the SQL parameter value
	 */
	public void addParameter(String name, Object value) {
		this.add(new Parameter(name, value));
	}

	public Parameters askGeneratedKeys(boolean value) {
		this.askGeneratedKeys = value;
		return this;
	}

	public boolean askGeneratedKeys() {
		return this.askGeneratedKeys;
	}

	public Set<Long> getGeneratedIds() {
		return this.generatedIds;
	}

	@Override
	public String toString() {
		return "Parameters{" +
			super.toString() +
			", tooLongAliases=" + tooLongAliases +
		'}';
	}

	public void addGeneratedKey(long id) {
		this.generatedIds.add(id);
	}

	/**
	 * An SQL parameter ('?' values in SQL queries)
	 */
	public static class Parameter {
		String name;
		Object value;

		private Parameter(String name, Object value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public Object getValue() {
			return value;
		}

		@Override
		public String toString() {
			return "Parameter{" + "name='" + this.name + '\'' + ", value=" + this.value + '}';
		}
	}
}
