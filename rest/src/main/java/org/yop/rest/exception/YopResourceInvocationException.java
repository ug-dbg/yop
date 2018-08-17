package org.yop.rest.exception;

public class YopResourceInvocationException extends RuntimeException {

	public YopResourceInvocationException(String message, Throwable cause) {
		super(message, cause);
	}

	public YopResourceInvocationException(String message) {
		super(message);
	}
}
