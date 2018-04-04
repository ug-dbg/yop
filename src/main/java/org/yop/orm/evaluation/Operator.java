package org.yop.orm.evaluation;

/**
 * Comparison operator enum. Quite classic : equals, greater than, like, not null...
 */
public enum Operator {
	LIKE(" LIKE "),
	EQ(" = "),
	GT(" > "),
	GE(" >= "),
	LT(" < "),
	LE(" <= "),
	IS_NULL(" IS NULL "),
	IS_NOT_NULL(" IS NOT NULL ");

	private final String sql;

	Operator(String sql) {
		this.sql = sql;
	}

	/**
	 * Generate an SQL portion for this operator.
	 * @return the SQL portion for the enum value
	 */
	public String toSQL() {
		return this.sql;
	}
}
