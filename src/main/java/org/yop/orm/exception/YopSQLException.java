package org.yop.orm.exception;

/**
 * A runtime exception to encapsulate the checked {@link java.sql.SQLException}.
 */
public class YopSQLException extends RuntimeException {

	public YopSQLException(String message) {
		super(message);
	}

	public YopSQLException(String message, Throwable cause) {
		super(message, cause);
	}

}
