package org.yop.orm.exception;

import org.yop.orm.sql.SQLExpression;

/**
 * An exception for incoherent query, i.e. the query SQL does not match the parameters length.
 */
public class YopIncoherentQueryException extends YopRuntimeException {

	private SQLExpression query;

	public YopIncoherentQueryException(SQLExpression query) {
		super(
			"Incoherent query SQL and parameters. "
			+ "SQL : [" + query.toString() + "]. "
			+ "Number of parameters [" + query.getParameters().size() + "]"
		);
		this.query = query;
	}

	public SQLExpression getQuery() {
		return this.query;
	}
}
