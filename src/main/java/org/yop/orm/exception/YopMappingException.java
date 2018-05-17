package org.yop.orm.exception;

/**
 * Any exception for any mapping related problem.
 */
public class YopMappingException extends RuntimeException {
	public YopMappingException(String message) {
		super(message);
	}
}
