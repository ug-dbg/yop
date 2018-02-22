package org.yop.orm.query.join;

import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Parameters;

import java.util.Collection;
import java.util.Set;

/**
 * A join clause.
 * <br>
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
	Collection<org.yop.orm.query.join.where.IJoin<To, ? extends Yopable>> getJoins();

	/**
	 * Add a sub join to the current join clause
	 * @param join   the next join clause
	 * @param <Next> the next target type
	 * @return the current join clause, for chaining purposes
	 */
	<Next extends Yopable> org.yop.orm.query.join.where.IJoin<From, To> join(org.yop.orm.query.join.where.IJoin<To, Next> join);

	/**
	 * Create the SQL join clause.
	 * @param context    the context from which the SQL clause must be built.
	 * @param parameters the SQL parameters. Populate me with !
	 * @return the SQL join clause
	 */
	String toSQL(Context<From> context, Parameters parameters);

	/**
	 * Find all the columns to select (search in current target type and sub-join clauses if required)
	 * @param addJoinClauseColumns true to add the columns from the sub-join clauses
	 * @return the columns to select
	 */
	default Set<Context.SQLColumn> columns(Context<From> context, boolean addJoinClauseColumns) {
		Context<To> to = this.to(context);
		Set<Context.SQLColumn> columns = to.getColumns();

		if (addJoinClauseColumns) {
			for (org.yop.orm.query.join.where.IJoin<To, ? extends Yopable> join : this.getJoins()) {
				columns.addAll(join.columns(to, true));
			}
		}
		return columns;
	}
}
