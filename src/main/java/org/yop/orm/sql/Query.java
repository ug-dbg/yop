package org.yop.orm.sql;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IRequest;
import org.yop.orm.util.ORMUtil;

import java.util.*;

/**
 * An SQL query, and everything it needs to be executed on a Connection.
 * <br>
 * It also has a generated ID set, that should be filled by {@link IRequest#executeUpdate()}.
 */
public class Query {

	private static final Comparator<String> ALIAS_COMPARATOR = Comparator
		.comparing(String::length)
		.thenComparing(String::compareTo)
		.reversed();

	/** The SQL to execute */
	private String sql;

	/** The SQL with too long aliases replaced with generated UUIDs */
	private String safeAliasSQL;

	/** The SQL query parameters (i.e. for '?' in the query) */
	private Parameters parameters;

	/** True to ask the statement to return the generated Ids */
	private boolean askGeneratedKeys = false;

	/** The generated IDs */
	private Set<Long> generatedIds = new HashSet<>();

	/** Aliases map : short alias → original alias */
	private Map<String, String> tooLongAliases = new HashMap<>();

	/** A reference to the root target Yopable that this query was generated for. Only required for generated keys. */
	protected Class<? extends Yopable> target;

	/**
	 * Default constructor : SQL query and parameters.
	 * @param sql        the SQL query to execute
	 * @param parameters the query parameters
	 */
	public Query(String sql, Parameters parameters) {
		this.sql = sql;
		this.safeAliasSQL = sql;
		this.parameters = parameters;

		// Search table/column aliases that are too long for SQL : longest alias first !
		Set<String> tooLongAliases = new TreeSet<>(ALIAS_COMPARATOR);
		for (String word : StringUtils.split(sql, " ,;\"")) {
			// if the word is not too long, that's OK
			// if the word contains a "." this is not an alias
			if(word.length() <= Constants.SQL_ALIAS_MAX_LENGTH || word.contains(Constants.DOT)) {
				continue;
			}
			tooLongAliases.add(
				StringUtils.removeEnd(StringUtils.removeStart(word.trim(), "\""), "\"")
			);
		}

		for (String tooLongAlias : tooLongAliases) {
			String shortened = ORMUtil.uniqueShortened(tooLongAlias);
			this.tooLongAliases.put(tooLongAlias, shortened);
			this.safeAliasSQL = StringUtils.replace(this.safeAliasSQL, tooLongAlias, shortened);
		}
	}


	public Class<? extends Yopable> getTarget() {
		return target;
	}

	/**
	 * @return the original SQL query
	 */
	public String getSql() {
		return this.sql;
	}

	/**
	 * @return the query with long aliases replaced with UUIDs.
	 */
	public String getSafeSql() {
		return this.safeAliasSQL;
	}

	/** Get the original alias for a shortened one. Return the given parameter if no entry */
	public String getAlias(String shortened) {
		return this.tooLongAliases.entrySet()
			.stream()
			.filter(e -> StringUtils.equals(shortened, e.getValue()))
			.map(Map.Entry::getKey)
			.findFirst()
			.orElse(shortened);
	}

	/** Get the shortened version of an alias. Return the given parameter if no entry */
	public String getShortened(String alias) {
		return this.tooLongAliases.getOrDefault(alias, alias);
	}

	/**
	 * @return the query parameters
	 */
	public Parameters getParameters() {
		return parameters;
	}

	/**
	 * Ask for generated IDs or not.
	 * @param value  true to ask the statement to resturn the generated IDs
	 * @param target the target class for which there will be generated keys
	 * @return the current query, for chaining purposes
	 */
	public Query askGeneratedKeys(boolean value, Class<? extends Yopable> target) {
		this.askGeneratedKeys = value;
		this.target = target;
		return this;
	}

	public boolean askGeneratedKeys() {
		return this.askGeneratedKeys;
	}

	/**
	 * Get the ID column of this query's {@link #target}.
	 * @return an array of string that contains the target column ID at index 0, or an empty array if target is null.
	 */
	public String[] getIdColumn() {
		return this.target == null ? new String[0] : new String[] {ORMUtil.getIdColumn(this.target)};
	}

	/**
	 * @return the Ids generated when executing this query
	 */
	public Set<Long> getGeneratedIds() {
		return this.generatedIds;
	}

	@Override
	public String toString() {
		return "Query{" +
			"sql='" + sql + '\'' +
			", parameters=" + parameters +
			", askGeneratedKeys=" + askGeneratedKeys +
			", generatedIds=" + generatedIds +
			", tooLongAliases=" + tooLongAliases +
		'}';
	}
}
