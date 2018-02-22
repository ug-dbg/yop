package org.yop.orm.query.join.where;

import org.yop.orm.evaluation.Comparaison;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.query.Where;
import org.yop.orm.sql.Parameters;

/**
 * A join clause that can have a where clause.
 * <br>
 * {@inheritDoc}
 */
public interface IJoin<From extends Yopable, To extends Yopable> extends org.yop.orm.query.join.IJoin<From, To> {

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
	 * @param parameters         the SQL parameters. Populate me with !
	 * @param includeWhereClause true to include the where clauses evaluation
	 * @return the SQL join clause
	 */
	String toSQL(Context<From> context, Parameters parameters, boolean includeWhereClause);
}
