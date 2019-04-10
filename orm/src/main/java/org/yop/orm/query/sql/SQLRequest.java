package org.yop.orm.query.sql;

import org.yop.orm.query.AbstractRequest;
import org.yop.orm.query.Context;
import org.yop.orm.query.join.IJoin;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.JoinClause;

import java.util.Set;

/**
 * A YOP request refined for SQL : it simply adds some protected methods, useful in an SQL context.
 * @param <T> the target type
 */
abstract class SQLRequest<Request extends AbstractRequest, T> extends AbstractRequest<Request, T> {

	/**
	 * Default constructor : final field {@link #context} must be initialized.
	 *
	 * @param context the context of the request
	 */
	SQLRequest(Context<T> context) {
		super(context);
	}

	/**
	 * Find all the columns to select (search in current target type and join clauses if required)
	 * @param addJoinClauseColumns true to add the columns from the join clauses
	 * @param config               the SQL config (sql separator, use batch inserts...)
	 * @return the columns to select
	 */
	protected Set<SQLColumn> columns(boolean addJoinClauseColumns, Config config) {
		Set<SQLColumn> columns = SQLColumn.columns(this.context, config);

		if (addJoinClauseColumns) {
			for (IJoin<T, ?> join : this.joins) {
				@SuppressWarnings("unchecked")
				SQLJoin<T, ?> sqlJoin = SQLJoin.toSQLJoin(join);
				columns.addAll(sqlJoin.columns(this.context, config));
			}
		}
		return columns;
	}

	/**
	 * Create the SQL join clause.
	 * @param evaluate true to add the where clauses to the join clauses
	 * @param config   the SQL config (sql separator, use batch inserts...)
	 * @return the SQL join clause
	 */
	protected JoinClause.JoinClauses toSQLJoin(boolean evaluate, Config config) {
		return SQLJoin.toSQLJoin(this.joins, this.context, evaluate, config);
	}
}
