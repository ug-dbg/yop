package org.yop.orm.sql;

import org.yop.orm.query.Context;
import org.yop.orm.query.IJoin;
import org.yop.orm.query.Where;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * A join clause, as an SQL query portion with parameters, for a given context.
 * <br>
 * They are comparable because join clauses must be context ordered.
 */
public class JoinClause implements Comparable<JoinClause> {

	/** The join clause */
	private final String joinClause;

	/** The context of the join clause */
	private final Context<?> context;

	/** The join clause parameters (see : {@link IJoin#where()}) */
	private final Parameters parameters;

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
		return this.joinClause;
	}

	public Context<?> getContext() {
		return this.context;
	}

	public Parameters getParameters() {
		return this.parameters;
	}

	@Override
	public String toString() {
		return this.joinClause;
	}

	@Override
	public int compareTo(@Nonnull JoinClause o) {
		return this.context.getPath(Config.DEFAULT).compareTo(o.context.getPath(Config.DEFAULT));
	}

	/**
	 * Convenience class to hide a verbose Map and a Where clause.
	 */
	public static class JoinClauses extends HashMap<Context<?>, JoinClause> {
		/** The where clause aggregated from all the joins */
		private WhereClause whereClause = new WhereClause();

		/**
		 * Add a where clause (from a join)
		 * @param clause     the where clause
		 * @param parameters the where clause parameters
		 */
		public void addWhereClause(String clause, Parameters parameters) {
			this.whereClause.clause = Where.toSQL(this.whereClause.clause, clause);
			this.whereClause.parameters.addAll(parameters);
		}

		/**
		 * Generate the SQL portion for these join clauses.
		 * <br>
		 * Join clauses are appended from the closest to the farthest.
		 * <br>
		 * The where clause is not included !
		 * @param parameters the query parameters that will be populated with the join clauses parameters
		 * @return the join clauses SQL portion
		 */
		public String toSQL(Parameters parameters) {
			// Join clauses have to be ordered from the closest context to the farthest when building the output !
			Set<JoinClause> clauses = new TreeSet<>(this.values());

			StringBuilder sql = new StringBuilder();
			for (JoinClause joinClause : clauses) {
				sql.append(joinClause.getJoinClause());
				parameters.addAll(joinClause.getParameters());
			}

			return sql.toString();
		}

		/**
		 * Generate the SQL portion for these join clauses' where clause
		 * @param parameters the query parameters that will be populated with the join clauses' where clause parameters
		 * @return the join clauses' where clause SQL portion
		 */
		public String toSQLWhere(Parameters parameters) {
			parameters.addAll(this.whereClause.parameters);
			return this.whereClause.clause;
		}
	}

	/** The where clause for all the joins */
	private static class WhereClause {
		private String clause = "";
		private Parameters parameters = new Parameters();
	}
}
