package org.yop.orm.exception;

import org.yop.orm.sql.Query;

import java.sql.SQLException;

/**
 * A runtime exception to encapsulate the checked {@link java.sql.SQLException}.
 */
public class YopSQLException extends RuntimeException {

	/** The query */
	private Query query;

	public YopSQLException(String message) {
		super(message);
	}

	public YopSQLException(String message, Throwable cause) {
		super(message, cause);
	}

	public YopSQLException(Query query, SQLException cause) {
		super(
			"Error executing query [" + query.getSql()
			+ "] with safe aliasing [" + query.getSafeSql()
			+ "] with parameters [" + query.getParameters() + "]",
			cause
		);
		this.query = query;
	}

	public Query getQuery() {
		return query;
	}
}
