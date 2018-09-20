package org.yop.rest.exception;

import org.yop.orm.exception.YopRuntimeException;

public class YopBadContentException extends YopRuntimeException {

	public YopBadContentException(String message, Throwable cause) {
		super(message, cause);
	}
}
