package org.yop.orm.query;

import org.yop.orm.model.Yopable;
import org.yop.orm.evaluation.Comparaison;

import java.util.Collection;
import java.util.Set;

/**
 * A join clause.
 * [From class / From table] → [relation field / relation table] → [Target class / Target table]
 * @param <From> the source type
 * @param <To>   the target type
 */
public interface IJoin<From extends Yopable, To extends Yopable> {

	/**
	 * Create a context from the 'From' type context to the 'To' context.
	 * <br>
	 * It should use a getter or a reference to a field to build the target context.
	 * @param from the 'From' type context.
	 * @return the 'To' type context
	 */
	Context<To> to(Context<From> from);

	/**
	 * Get the sub-join clauses
	 * @return the sub join clauses (To → relation → Next)
	 */
	Collection<IJoin<To, ? extends Yopable>> getJoins();

	/**
	 * Add a sub join to the current join clause
	 * @param join   the next join clause
	 * @param <Next> the next target type
	 * @return the current join clause, for chaining purposes
	 */
	<Next extends Yopable> IJoin<From, To> join(IJoin<To, Next> join);

	/**
	 * Get the join clause where clause
	 * @return the current join clause where clause
	 */
	Where<To> where();

	/**
	 * Add a where clause to the current join clause.
	 * @param evaluation the comparison clause
	 * @return tje current join clause for chaining purposes
	 */
	IJoin<From, To> where(Comparaison evaluation);

	/**
	 * Create the SQL join clause.
	 * @param context            the context from which the SQL clause must be built.
	 * @param includeWhereClause true to include the where clauses evaluation
	 * @return the SQL join clause
	 */
	String toSQL(Context<From> context, boolean includeWhereClause);

	/**
	 * Find all the columns to select (search in current target type and sub-join clauses if required)
	 * @param addJoinClauseColumns true to add the columns from the sub-join clauses
	 * @return the columns to select
	 */
	default Set<Context.SQLColumn> columns(Context<From> context, boolean addJoinClauseColumns) {
		Context<To> to = this.to(context);
		Set<Context.SQLColumn> columns = to.getColumns();

		if (addJoinClauseColumns) {
			for (IJoin<To, ? extends Yopable> join : this.getJoins()) {
				columns.addAll(join.columns(to, true));
			}
		}
		return columns;
	}
}
