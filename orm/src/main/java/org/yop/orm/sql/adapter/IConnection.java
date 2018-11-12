package org.yop.orm.sql.adapter;

import org.yop.orm.sql.Config;
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
 * <br><br>
 * This connection abstraction also provides the configuration to use.
 * <br>
 * The default implementation returns a {@link Config} initialized from system properties or default values.
 * <br>
 * See {@link Config#initFromSystemProperties()}
 * <br>
 * The JDBC implementation provides a method to override default configuration :
 * {@link org.yop.orm.sql.adapter.jdbc.JDBCConnection#withConfig(Config)}.
 */
public interface IConnection extends AutoCloseable {

	/**
	 * Get the config for this connection (SQL separator, show sql...).
	 * <br>
	 * Override me to manage a custom configuration in your own implementation.
	 * <br>
	 * See {@link org.yop.orm.sql.adapter.jdbc.JDBCConnection#config()}
	 * @return the default config, initialized from system properties.
	 */
	default Config config() {
		return Config.DEFAULT;
	}

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
	 * Get this connection's auto-commit mode.
	 * @return the current auto-commit state
	 * @throws SQLException an error occurred getting the auto-commit mode
	 */
	boolean getAutoCommit() throws SQLException;

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

	/**
	 * Undoes all changes made in the current transaction and releases any database locks currently held
	 * by this Connection object.
	 * <br>
	 * This method should be used only when auto-commit mode has been disabled.
	 *
	 * @throws SQLException if a database access error occurs,
	 * this method is called while participating in a distributed transaction,
	 * this method is called on a closed connection or this Connection object is in auto-commit mode
	 */
	void rollback() throws SQLException;
}
