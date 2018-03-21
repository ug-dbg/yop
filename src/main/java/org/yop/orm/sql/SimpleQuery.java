package org.yop.orm.sql;

public class SimpleQuery extends Query {

	/** The SQL query parameters (i.e. for '?' in the query) */
	private Parameters parameters;

	private boolean next = true;

	/**
	 * Default constructor : SQL query and parameters.
	 * @param sql        the SQL query to execute
	 * @param parameters the query parameters
	 */
	public SimpleQuery(String sql, Parameters parameters) {
		super(sql);
		this.parameters = parameters;
	}

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
	public Parameters getParameters() {
		return parameters;
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
