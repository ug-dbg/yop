package org.yop.rest.exception;

import org.yop.orm.exception.YopRuntimeException;

public class YopForbiddenException extends YopRuntimeException {
	public YopForbiddenException(String message) {
		super(message);
	}
}
