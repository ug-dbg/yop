package org.yop.rest.exception;

import org.yop.orm.exception.YopRuntimeException;

public class YopOpenAPIException extends YopRuntimeException {
	public YopOpenAPIException(String message, Throwable cause) {
		super(message, cause);
	}
}
