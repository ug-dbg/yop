package org.yop.orm.sql.adapter;

import org.yop.orm.sql.Query;

import java.sql.SQLException;

/**
 * SQL Connection <b>abstraction</b>.
 * <br>
 * (e.g. you have a grip onto your SQL database other than JDBC)
 * <br><br>
 * The reason for this is making a <i>very small</i> step toward Android.
 * <br><br>
 * Of course, you can easily get an IConnection for JDBC : {@link org.yop.orm.sql.adapter.jdbc.JDBCConnection}.
 */
public interface IConnection extends AutoCloseable {

	/**
	 * Prepare the request (e.g. SQL prepared statement) to be executed using the query.
	 * <br>
	 * The safe alias SQL query should be used and parameters set.
	 * <br>
	 * @param query the query object to use to prepare the request
	 * @return the prepared request with the parameters set
	 */
	IRequest prepare(Query query);

	/**
	 * Close the underlying connection
	 * @throws SQLException an error occurred closing the connection
	 */
	void close() throws SQLException;

	/**
	 * Sets this connection's auto-commit mode to the given state.
	 * @param autocommit the auto-commit state
	 * @throws SQLException an error occurred setting the auto-commit mode
	 */
	void setAutoCommit(boolean autocommit) throws SQLException;

	/**
	 * Makes all changes made since the previous commit/rollback permanent
	 * and releases any database locks currently held by this Connection object.
	 * This method should be used only when auto-commit mode has been disabled.
	 * @throws SQLException an error occurred committing the current state
	 */
	void commit() throws SQLException;
}
