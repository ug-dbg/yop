package org.yop.orm.sql.adapter;

import org.yop.orm.sql.Query;

import java.sql.SQLException;

/**
 * SQL statement <b>abstraction</b>.
 * <br>
 * (e.g. you have a grip onto your SQL database other than JDBC)
 * <br><br>
 * The reason for this is making a <i>very small</i> step toward Android.
 * <br><br>
 * Of course, you can easily get an IRequest for JDBC : {@link org.yop.orm.sql.adapter.jdbc.JDBCRequest}.
 */
public interface IRequest extends AutoCloseable {

	/**
	 * Return the underlying query object
	 * @return the query
	 */
	Query getQuery();

	/**
	 * Execute the query and get a cursor (e.g. JDBC resultset).
	 * <br>
	 * Typically for 'SELECT' queries.
	 * @return the query results cursor
	 */
	IResultCursor execute();

	/**
	 * Execute the request that does not return any result.
	 * <br><br>
	 * Typically for 'DELETE', 'UPDATE' or 'INSERT' queries.
	 * <br><br>
	 * Any generated ID should be pushed to {@link #getQuery()} using {@link Query#pushGeneratedIds()}.
	 * <br>
	 * This mechanism is not very easy to read. I will try to do something better :)
	 */
	void executeUpdate();

	/**
	 * Release the resources this request holds.
	 * @throws SQLException an error occurred closing the request
	 */
	void close() throws SQLException;
}
