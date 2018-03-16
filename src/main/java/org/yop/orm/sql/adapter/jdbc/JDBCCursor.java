package org.yop.orm.sql.adapter.jdbc;

import org.yop.orm.exception.YopSQLException;
import org.yop.orm.sql.Query;
import org.yop.orm.sql.adapter.IRequest;
import org.yop.orm.sql.adapter.IResultCursor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * JDBC cursor, returned from {@link IRequest#execute()} when using JDBC.
 * This is basically :
 * <ul>
 *     <li>a JDBC result set</li>
 *     <li>the original query</li>
 *     <li>the original JDBC prepared statement </li>
 * </ul>
 */
public class JDBCCursor implements IResultCursor {

	/** The JDBC resultset */
	private final ResultSet results;

	/** The original query */
	private final Query query;

	/** The original statement */
	private final PreparedStatement statement;

	/**
	 * Default constructor. Give me the query, the statement and the result set !
	 * <br>
	 * Of course, the result set and the statement should come from the given query.
	 * @param results   the JDBC result set
	 * @param statement the JDBC prepared statement
	 * @param query     the original query
	 */
	JDBCCursor(ResultSet results, PreparedStatement statement, Query query) {
		this.results = results;
		this.statement = statement;
		this.query = query;
	}

	@Override
	public void close() throws SQLException {
		this.results.close();
		this.statement.close();
	}

	@Override
	public boolean hasColumn(String columnName) {
		try {
			ResultSetMetaData metaData = this.results.getMetaData();
			int columns = metaData.getColumnCount();
			for (int x = 1; x <= columns; x++) {
				if (columnName.equals(metaData.getColumnLabel(x))) {
					return true;
				}
			}
			return false;
		} catch (SQLException e) {
			throw new YopSQLException(
				"Error reading metadata for column [" + columnName + "] after query [" + this.query + "]", this.query, e
			);
		}
	}

	@Override
	public int getColumnCount() {
		try {
			return this.results.getMetaData().getColumnCount();
		} catch (SQLException e) {
			throw new YopSQLException("Error reading column count after query [" + this.query + "]", this.query, e);
		}
	}

	@Override
	public String getColumnName(int columnIndex) {
		try {
			return this.results.getMetaData().getColumnLabel(columnIndex);
		} catch (SQLException e) {
			throw new YopSQLException(
				"Error reading column [" + columnIndex + "] after query [" + this.query + "]", this.query, e
			);
		}
	}

	@Override
	public Long getLong(String columnName) {
		try {
			return this.results.getLong(columnName);
		} catch (SQLException e) {
			throw new YopSQLException(
				"Error reading for long in column [" + columnName + "] after query [" + this.query + "]", this.query, e
			);
		}
	}

	@Override
	public Object getObject(String columnName) {
		try {
			return this.results.getObject(columnName);
		} catch (SQLException e) {
			throw new YopSQLException(
				"Error reading column [" + columnName + "] after query [" + this.query + "]", this.query, e
			);
		}
	}

	@Override
	public Object getObject(String columnName, Class<?> type) {
		try {
			return this.results.getObject(columnName, type);
		} catch (SQLException e) {
			throw new YopSQLException(
				"Error reading column [" + columnName + "] after query [" + this.query + "]", this.query, e
			);
		}
	}

	@Override
	public Long getLong(int columnIndex) {
		try {
			return this.results.getLong(columnIndex);
		} catch (SQLException e) {
			throw new YopSQLException(
				"Error reading long value in column #[" + columnIndex+ "] for query [" + this.query + "]", this.query, e
			);
		}
	}

	@Override
	public boolean next() {
		try {
			return this.results.next();
		} catch (SQLException e) {
			throw new YopSQLException(
				"Could not move to next row on the resultset of [" + this.query + "]",
				e
			);
		}
	}
}
