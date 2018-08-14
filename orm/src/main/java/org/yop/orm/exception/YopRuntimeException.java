package org.yop.orm.exception;

/**
 * This is the base YOP Runtime Exception. <br>
 * YOP throws mostly Runtime Exceptions to keep the API simple. <br>
 *
 * Created by hugues on 10/02/15.
 * Ω≡{Ⓐ}
 */
public class YopRuntimeException extends RuntimeException {
	public YopRuntimeException(String message) {
		super(message);
	}

	public YopRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}
}
