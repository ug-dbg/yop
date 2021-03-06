package org.yop.orm.sql;

import org.yop.orm.query.Context;

import javax.annotation.Nonnull;
import java.util.*;

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

	/** The join clause parameters (see : {@link org.yop.orm.query.sql.SQLJoin#where()}) */
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
		private SQLExpression whereClause = new SQLExpression("");

		/**
		 * Add a where clause (from a join).
		 * @param config the SQL config (dialect, sql separator, use batch inserts...)
		 * @param sql    the where clause to append
		 */
		public void addWhereClause(Config config, SQLExpression sql) {
			this.whereClause = config.getDialect().where(this.whereClause, sql);
		}

		/**
		 * Generate the SQL portion for these join clauses.
		 * <br>
		 * Join clauses are appended from the closest to the farthest.
		 * <br>
		 * The where clause is not included !
		 * @return the join clauses SQL portion
		 */
		public SQLExpression toSQL(Config config) {
			// Join clauses have to be ordered from the closest context to the farthest when building the output !
			Set<JoinClause> clauses = new TreeSet<>(this.values());
			List<Parameters.Parameter> parameters = new ArrayList<>();
			StringBuilder sql = new StringBuilder();
			for (JoinClause joinClause : clauses) {
				sql.append(joinClause.joinClause);
				parameters.addAll(joinClause.getParameters());
			}

			return new SQLExpression(sql.toString(), parameters);
		}

		/**
		 * Generate the SQL portion for these join clauses' where clause
		 * @return the join clauses' where clause SQL part
		 */
		public SQLExpression toSQLWhere() {
			return this.whereClause;
		}
	}
}
