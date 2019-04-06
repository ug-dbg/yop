package org.yop.rest.servlet;

/**
 * HTTP PUT implementation for the YOP servlet.
 * <br>
 * This is simply using the {@link Upsert} implementation.
 * <br>
 * <b>This means PUT is not always idempotent !</b>
 * <br>
 * If your need idempotence for the PUT method, please override this one.
 */
public class Put extends Upsert {
	static final HttpMethod INSTANCE = new Put();
}
