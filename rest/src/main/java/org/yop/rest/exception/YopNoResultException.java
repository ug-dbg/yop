package org.yop.rest.exception;

import org.yop.orm.exception.YopRuntimeException;

public class YopNoResultException extends YopRuntimeException {

	public YopNoResultException(String message) {
		super(message);
	}
}
