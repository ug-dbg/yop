package org.yop.orm.sql;

import java.sql.ResultSet;

/**
 * An SQL result set with references to :
 * <ul>
 *     <li>query</li>
 *     <li>query with safe aliases</li>
 *     <li>query parameters</li>
 * </ul>
 */
public class Results {

	/** The query result set. The result set closing is not handled in this class at all ! */
	private final ResultSet resultSet;

	/** The query parameters and long aliases replacements */
	private final Parameters parameters;

	/** The SQL query */
	private final String query;

	/** The SQL query with long aliases replaced */
	private final String safeAliasQuery;

	public Results(ResultSet resultSet, Parameters parameters, String query, String safeAliasQuery) {
		this.resultSet = resultSet;
		this.parameters = parameters;
		this.query = query;
		this.safeAliasQuery = safeAliasQuery;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public Parameters getParameters() {
		return parameters;
	}

	public String getQuery() {
		return query;
	}

	public String getSafeAliasQuery() {
		return safeAliasQuery;
	}
}
