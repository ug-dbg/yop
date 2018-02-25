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

	/** The query that was executed */
	private Query query;

	/**
	 * Default constructor : resultset and original query
	 * @param resultSet the resultset from the query execution
	 * @param query     the executed query
	 */
	Results(ResultSet resultSet, Query query) {
		this.resultSet = resultSet;
		this.query = query;
	}

	/**
	 * @return the resultset from the query execution
	 */
	public ResultSet getResultSet() {
		return resultSet;
	}

	/**
	 * @return the executed query parameters
	 */
	public Parameters getParameters() {
		return this.query.getParameters();
	}

	/**
	 * @return the executed query
	 */
	public Query getQuery() {
		return query;
	}

	/**
	 * @return the executed query original SQL
	 */
	public String getSQL() {
		return this.query.getSql();
	}

	/**
	 * @return the SQL that cas actually executed (long aliases → safe aliases)
	 */
	public String getSafeAliasSQL() {
		return this.query.getSafeSql();
	}
}
