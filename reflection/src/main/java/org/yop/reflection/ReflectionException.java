package org.yop.reflection;

/**
 * An exception that occurred when using {@link org.yop.reflection.Reflection} and/or the {@link java.lang.reflect} API.
 */
public class ReflectionException extends RuntimeException {

	public ReflectionException(String message) {
		super(message);
	}

	public ReflectionException(String message, Throwable cause) {
		super(message, cause);
	}

}
