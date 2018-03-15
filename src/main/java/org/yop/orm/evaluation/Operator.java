package org.yop.orm.evaluation;

public enum Operator {
	LIKE(" LIKE "),
	EQ(" = "),
	GT(" > "),
	GE(" >= "),
	LT(" < "),
	LE(" <= "),
	IS_NULL(" IS NULL "),
	IS_NOT_NULL(" IS NOT NULL ");

	String sql;

	Operator(String sql) {
		this.sql = sql;
	}

	public String toSQL() {
		return this.sql;
	}
}
