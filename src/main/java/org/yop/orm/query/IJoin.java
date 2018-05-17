package org.yop.orm.query;

import org.yop.orm.evaluation.Evaluation;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.JoinClause;
import org.yop.orm.util.ORMUtil;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A join clause.
 * [From class / From table] → [relation field / relation table] → [Target class / Target table]
 * <br>
 * The join clause is referenced by the getter method.
 * <br>
 * Because of cardinality management there are 2 join clause implementations :
 * <ul>
 *     <li>{@link Join}</li>
 *     <li>{@link JoinSet}</li>
 * </ul>
 * <br>
 * You can concatenate join clauses :
 * <pre>
 * {@code JoinSet.to(Genre::getTracksOfGenre).join(Join.to(Track::getAlbum).join(Join.to(Album::getArtist)))}
 * </pre>
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
	IJoin<From, To> where(Evaluation evaluation);

	/**
	 * Create the SQL join clause.
	 * @param joinClauses        the join clauses map
	 * @param context            the context from which the SQL clause must be built.
	 * @param includeWhereClause true to include the where clauses evaluation
	 */
	void toSQL(JoinClause.JoinClauses joinClauses, Context<From> context, boolean includeWhereClause);

	/**
	 * Return the join table alias from the given context
	 * @param context the context from which the alias is built.
	 * @return the join table alias for the given context
	 */
	String joinTableAlias(Context<From> context);

	/**
	 * Get the field this join is related to, given the source class.
	 * @param from the source
	 * @return the found field
	 */
	Field getField(Class<From> from);

	/**
	 * Return the target type of this join, given the field
	 * @param field the field of this join
	 * @return the target class
	 */
	Class<To> getTarget(Field field);

	/**
	 * Get the objects related to the source object, using the current join clause
	 * @param from the source object
	 * @return the related targets
	 */
	Collection<To> getTarget(From from);

	/**
	 * Find all the columns to select (search in current target type and sub-join clauses if required)
	 * @param context              the context (columns are deduced using {@link Context#getColumns()}.
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

	/**
	 * Join all relation fields from the source class.
	 * @param source the source class, where fields will be searched
	 * @param joins  the target joins collection
	 * @param <T> the source type
	 */
	static <T extends Yopable> void joinAll(Class<T> source, Collection<IJoin<T, ?  extends Yopable>> joins) {
		List<Field> fields = ORMUtil.nonTransientJoinedFields(source);
		for (Field field : fields) {
			IJoin<T, Yopable> join = AbstractJoin.create(field);
			joins.add(join);

			Class<Yopable> newTarget = join.getTarget(field);
			joinAll(newTarget, join.getJoins());
		}
	}
}
