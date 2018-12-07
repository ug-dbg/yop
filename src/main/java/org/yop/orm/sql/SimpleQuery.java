package org.yop.orm.sql;

import org.yop.orm.exception.YopIncoherentQueryException;

/**
 * A query implementation with no batch mechanism : {@link #nextBatch()} returns true once, then false.
 */
public class SimpleQuery extends Query {

	/** The SQL query parameters (i.e. for '?' in the query) */
	private final Parameters parameters;

	/** Fake single entry batch cursor. Default to true. Set to false on {@link #nextBatch()} first call */
	private boolean next = true;

	/**
	 * Default constructor : SQL query WITH NO PARAMETER.
	 * @param sql        the SQL query to execute (query string + parameters)
	 * @param type       the query type
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 */
	public SimpleQuery(String sql, Type type, Config config) {
		this(new SQLPart(sql), type, config);
	}

	/**
	 * Default constructor : SQL query part (with its parameters).
	 * @param sql        the SQL query to execute (query string + parameters)
	 * @param type       the query type
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 */
	public SimpleQuery(SQLPart sql, Type type, Config config) {
		super(sql.toString(), type, config);
		if (! sql.isCoherent()) {
			throw new YopIncoherentQueryException(sql);
		}
		this.parameters = sql.getParameters();
	}

	/**
	 * Fake single entry batch : return true once then false.
	 * See {@link #next}.
	 * @return true on first call, then false.
	 */
	@Override
	public boolean nextBatch() {
		if(this.next) {
			this.next = false;
			return true;
		}
		return false;
	}

	/**
	 * @return the query parameters ({@link #parameters} whatever the result of {@link #nextBatch()}.
	 */
	@Override
	public Parameters getParameters() {
		return this.parameters;
	}

	@Override
	public String parametersToString() {
		return String.valueOf(this.parameters);
	}

	@Override
	public String toString() {
		return "Query{" +
			"sql='" + this.sql + '\'' +
			", parameters=" + this.parameters +
			", askGeneratedKeys=" + this.askGeneratedKeys +
			", generatedIds=" + this.generatedIds +
			", tooLongAliases=" + this.tooLongAliases +
		'}';
	}
}
