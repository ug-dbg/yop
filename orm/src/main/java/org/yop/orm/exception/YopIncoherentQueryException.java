package org.yop.orm.exception;

import org.yop.orm.sql.SQLPart;

/**
 * An exception for incoherent query, i.e. the query SQL does not match the parameters length.
 */
public class YopIncoherentQueryException extends YopRuntimeException {

	private SQLPart query;

	public YopIncoherentQueryException(SQLPart query) {
		super(
			"Incoherent query SQL and parameters. "
			+ "SQL : [" + query.toString() + "]. "
			+ "Number of parameters [" + query.getParameters().size() + "]"
		);
		this.query = query;
	}

	public SQLPart getQuery() {
		return this.query;
	}
}
