package org.yop.rest.exception;

import org.yop.orm.exception.YopRuntimeException;

public class YopResourceInvocationException extends YopRuntimeException {

	public YopResourceInvocationException(String message, Throwable cause) {
		super(message, cause);
	}

	public YopResourceInvocationException(String message) {
		super(message);
	}
}
