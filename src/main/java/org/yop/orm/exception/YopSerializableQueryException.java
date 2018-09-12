package org.yop.orm.exception;

/**
 * An exception related to the serialization/deserialization of Yop Queries (Select/Upsert/Delete).
 */
public class YopSerializableQueryException extends YopRuntimeException {
	public YopSerializableQueryException(String message, Throwable cause) {
		super(message, cause);
	}
}
