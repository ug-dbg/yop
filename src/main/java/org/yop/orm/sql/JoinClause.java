package org.yop.orm.sql;

import org.yop.orm.query.Context;
import org.yop.orm.query.IJoin;

import java.util.HashMap;

/**
 * A join clause, as an SQL query portion with parameters, for a given context.
 * <br>
 * They are comparable because join clauses must be context ordered.
 */
public class JoinClause implements Comparable<JoinClause> {

	/** The join clause */
	private String joinClause;

	/** The context of the join clause */
	private Context<?> context;

	/** The join clause parameters (see : {@link IJoin#where()}) */
	private Parameters parameters;

	/**
	 * JoinClause constructor. Please gimme all I need !
	 * @param joinClause the join clause (piece of SQL)
	 * @param context    the join clause context (makes join clauses comparable)
	 * @param parameters the join clause SQL parameters
	 */
	public JoinClause(String joinClause, Context<?> context, Parameters parameters) {
		this.joinClause = joinClause;
		this.context = context;
		this.parameters = parameters;
	}

	public String getJoinClause() {
		return joinClause;
	}

	public Context<?> getContext() {
		return context;
	}

	public Parameters getParameters() {
		return parameters;
	}

	@Override
	public String toString() {
		return this.joinClause;
	}

	@Override
	public int compareTo(JoinClause o) {
		return this.context.getPath().compareTo(o.context.getPath());
	}

	/**
	 * Convenience class to hide a verbose Map.
	 */
	public static class JoinClauses extends HashMap<Context<?>, JoinClause> {}
}
