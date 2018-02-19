package org.yop.orm.exception;

/**
 * Any exception that can occur during the ResultSet → Yopables objects mapping.
 */
public class YopMapperException extends RuntimeException {
	public YopMapperException(String message) {
		super(message);
	}

	public YopMapperException(String message, Throwable cause) {
		super(message, cause);
	}
}
