package org.yop.orm.exception;

/**
 * Exception in ORM request syntax exception.
 */
public class YopSyntaxException extends RuntimeException {
	public YopSyntaxException(String message) {
		super(message);
	}

	public YopSyntaxException(String message, Throwable cause) {
		super(message, cause);
	}
}