package org.yop.rest.exception;

import org.yop.orm.exception.YopRuntimeException;

public class YopNoAuthException extends YopRuntimeException {
	public YopNoAuthException(String message) {
		super(message);
	}
}
