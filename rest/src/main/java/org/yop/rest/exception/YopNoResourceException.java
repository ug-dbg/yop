package org.yop.rest.exception;

import org.yop.orm.exception.YopRuntimeException;

public class YopNoResourceException extends YopRuntimeException {
	public YopNoResourceException(String message) {
		super(message);
	}
}
