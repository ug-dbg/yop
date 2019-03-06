package org.yop.orm.exception;

import org.yop.orm.query.Context;

import java.util.function.Function;

/**
 * A runtime exception that is thrown when a join path is invalid.
 * <br>
 * e.g. [Library::getBooks, Author::getBooks, Book::getAuthor]
 * <br>
 * e.g. [Library::getBooks, Author::getBooks, String::length]
 * <br>
 * See for instance : {@link org.yop.orm.query.join.IJoin.Joins#join(Context, Function[])}
 */
public class YopInvalidJoinException extends YopRuntimeException {
	public YopInvalidJoinException(String message) {
		super(message);
	}

	public YopInvalidJoinException(String message, Throwable cause) {
		super(message, cause);
	}
}
