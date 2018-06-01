package org.yop.orm.sql.adapter;

import java.sql.SQLException;

/**
 * SQL result set <b>abstraction</b>.
 * <br>
 * (e.g. you have a grip onto your SQL database other than JDBC)
 * <br><br>
 * The reason for this is making a <i>very small</i> step toward Android.
 * <br><br>
 * Of course, you can easily get an IResultCursor for JDBC : {@link org.yop.orm.sql.adapter.jdbc.JDBCCursor}.
 */
public interface IResultCursor extends AutoCloseable {

	/**
	 * Check if a given column is present in the cursor
	 * @param columnName the column name
	 * @return true if the column is present in the cursor
	 */
	boolean hasColumn(String columnName);

	/**
	 * Get the column name for the given column index.
	 * <br>
	 * No range control should be done here, since this interface also declares {@link #getColumnCount()}.
	 * @param columnIndex the column index
	 * @return the column name for the given index.
	 */
	String getColumnName(int columnIndex);

	/**
	 * Get the number of available columns on this cursor
	 * @return the column count of this cursor
	 */
	int getColumnCount();

	/**
	 * Read a column as a Long.
	 * <br>
	 * No control should be performed over the column type.
	 * @param columnName the column name
	 * @return the long value in the given column
	 */
	Long getLong(String columnName);

	/**
	 * Read a column as a Long.
	 * <br>
	 * No control should be performed over the column type.
	 * @param columnIndex the column index
	 * @return the long value in the given column
	 */
	Long getLong(int columnIndex);

	/**
	 * Read a column.
	 * @param columnName the column to read
	 * @return the column raw value
	 */
	Object getObject(String columnName);

	/**
	 * Read a column as an expected type.
	 * @param columnName the column to read
	 * @param type       the expected type
	 * @return the column value as the expected type
	 */
	Object getObject(String columnName, Class<?> type);

	/**
	 * Move the cursor to the next row.
	 * @return true if the new current row is valid; false if there are no more rows
	 * @throws org.yop.orm.exception.YopSQLException a database access error occurred or the cursor is closed
	 */
	boolean next();

	/**
	 * Release all the resources this cursor holds.
	 * <br>
	 * Calling the method close on an object that is already closed is a no-op.
	 * @throws SQLException database access error occurred
	 */
	void close() throws SQLException;
}
