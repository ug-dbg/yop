package org.yop.orm.sql;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.ORMUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * An SQL query, and everything it needs to be executed on a Connection.
 */
public class Query {

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

		// Search table/column aliases that are too long for SQL
		Set<String> tooLongAliases = new TreeSet<>(Comparator.comparing(String::length).reversed());
		for (String word : sql.split(" ")) {
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
			String shortened = uniqueShortened(tooLongAlias);
			this.tooLongAliases.put(tooLongAlias, shortened);
			this.safeAliasSQL = safeAliasSQL.replace("\"" + tooLongAlias + "\"", "\"" + shortened + "\"");
			this.safeAliasSQL = safeAliasSQL.replace(tooLongAlias + " ", shortened + " ");
			this.safeAliasSQL = safeAliasSQL.replace(tooLongAlias + ".", shortened + ".");
		}
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
		this.parameters.add(new Parameters.Parameter(name, value));
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

	/**
	 * @return if {@link #askGeneratedKeys} → {@link Statement#RETURN_GENERATED_KEYS}
	 *         else → {@link Statement#NO_GENERATED_KEYS}
	 */
	public int generatedKeyCommand() {
		return this.askGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS;
	}

	/**
	 * @return the Ids generated when executing this query
	 */
	public Set<Long> getGeneratedIds() {
		return this.generatedIds;
	}

	/**
	 * Read the generated keys from the executed statement.
	 * <br>
	 * We rely on {@link Statement#getGeneratedKeys()} and don't specify the generated key column.
	 * (I surely should check this out first)
	 * <br>
	 * <br>
	 * <b>
	 *     Generally, the generated key is in the column #1.
	 *     <br>
	 *     But, Postgres for instance, put the whole row in the generated key ResultSet.
	 *     <br>
	 *     So we need either :
	 *     <ul>
	 *         <li>Put the ID column first ALWAYS</li>
	 *         <li>Keep a reference to the {@link #target} class to read the right column </li>
	 *     </ul>
	 *
	 * </b>
	 * @param statement the statement that was executed
	 * @throws SQLException an SQL error occured reading the resultset
	 */
	public void readGeneratedKey(Statement statement) throws SQLException {
		if(this.askGeneratedKeys) {
			ResultSet generatedKeys = statement.getGeneratedKeys();
			int idIndex = 1;

			if(this.target != null) {
				String idColumn = ORMUtil.getIdColumn(this.target);
				for (int i = 1; i <= generatedKeys.getMetaData().getColumnCount(); i++) {
					if (StringUtils.equals(idColumn, generatedKeys.getMetaData().getColumnLabel(i))) {
						idIndex = i;
						break;
					}
				}
			}

			while (generatedKeys.next()) {
				this.generatedIds.add(generatedKeys.getLong(idIndex));
			}
		}
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

	/**
	 * Generate an unique shortened alias for the given one
	 * @param alias the alias that is too long
	 * @return a unique alias, generated from the shortened parameter + genererated UUID
	 */
	private static String uniqueShortened(String alias) {
		String shortened = StringUtils.substringAfterLast(alias, Constants.SQL_SEPARATOR);
		shortened = StringUtils.substring(shortened, 0, Constants.SQL_ALIAS_MAX_LENGTH - 37);
		return shortened + UUID.randomUUID().toString().replace("-", "_");
	}
}
