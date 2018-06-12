package org.yop.orm.sql;

/**
 * A query implementation with no batch mechanism : {@link #nextBatch()} returns true once, then false.
 */
public class SimpleQuery extends Query {

	/** The SQL query parameters (i.e. for '?' in the query) */
	private final Parameters parameters;

	/** Fake single entry batch cursor. Default to true. Set to false on {@link #next} first call */
	private boolean next = true;

	/**
	 * Default constructor : SQL query and parameters.
	 * @param sql        the SQL query to execute
	 * @param type       the query type
	 * @param parameters the query parameters
	 */
	public SimpleQuery(String sql, Type type, Parameters parameters) {
		super(sql, type);
		this.parameters = parameters;
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
