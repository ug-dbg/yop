package org.yop.orm.exception;

import org.yop.orm.sql.Parameters;

/**
 * A runtime exception to encapsulate the checked {@link java.sql.SQLException}.
 */
public class YopSQLException extends RuntimeException {

	/** The original query */
	private String query;

	/** The query with safe aliases replacing too long aliases */
	private String safeAliasQuery;

	/** The query parameters */
	private Parameters parameters;

	public YopSQLException(String message) {
		super(message);
	}

	public YopSQLException(String message, Throwable cause) {
		super(message, cause);
	}

	public YopSQLException(String query, String safeAliasQuery, Parameters parameters, Throwable cause) {
		super(
			"Error executing query [" + query
			+ "] with safe aliasing [" + safeAliasQuery
			+ "] with parameters [" + parameters + "]",
			cause
		);

		this.query = query;
		this.safeAliasQuery = safeAliasQuery;
		this.parameters = parameters;
	}

	public String getQuery() {
		return query;
	}

	public String getSafeAliasQuery() {
		return safeAliasQuery;
	}

	public Parameters getParameters() {
		return parameters;
	}
}
