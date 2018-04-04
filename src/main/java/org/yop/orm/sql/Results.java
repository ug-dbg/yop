package org.yop.orm.sql;

import org.yop.orm.sql.adapter.IResultCursor;

/**
 * The query results. Aggregates :
 * <ul>
 *     <li>query</li>
 *     <li>result cursor</li>
 * </ul>
 */
public class Results {

	/** The query cursor (JDBC result set). The result set closing is not handled in this class at all ! */
	private final IResultCursor cursor;

	/** The query that was executed */
	private final Query query;

	/**
	 * Default constructor : resultset and original query
	 * @param cursor the resultset from the query execution
	 * @param query     the executed query
	 */
	Results(IResultCursor cursor, Query query) {
		this.cursor = cursor;
		this.query = query;
	}

	/**
	 * @return the resultset from the query execution
	 */
	public IResultCursor getCursor() {
		return cursor;
	}

	/**
	 * @return the executed query
	 */
	public Query getQuery() {
		return query;
	}
}
