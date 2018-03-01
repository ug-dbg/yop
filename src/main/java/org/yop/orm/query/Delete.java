package org.yop.orm.query;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import org.yop.orm.evaluation.Evaluation;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Delete : delete instances of T from the database.
 * <br>
 * Standard DELETE clauses are accessible :
 * <ul>
 *     <li>FROM</li>
 *     <li>JOIN</li>
 *     <li>WHERE</li>
 * </ul>
 * This API aims at writing inlined requests.
 * <br>
 * <b>
 *     ⚠⚠⚠ A delete request with join clauses deletes the whole data graph that matches ! ⚠⚠⚠
 * </b>
 * <br><br>
 * <b>
 *     ⚠⚠⚠ Some DB does not support delete with joins ! Please use selects then deletes :-( ⚠⚠⚠
 * </b>
 *
 * @param <T> the type to delete.
 */
public class Delete<T extends Yopable> {

	private Class<T> target;
	private Where<T> where;
	private Collection<IJoin<T, ? extends Yopable>> joins = new ArrayList<>();

	private Delete(Class<T> target) {
		this.target = target;
		this.where = new Where<>();
	}

	public static <T extends Yopable> Delete<T> from(Class<T> target) {
		return new Delete<>(target);
	}

	/**
	 * Add an evaluation to the where clause.
	 * @param evaluation the evaluation
	 * @return the current SELECT request, for chaining purposes
	 */
	public Delete<T> where(Evaluation evaluation) {
		this.where.and(evaluation);
		return this;
	}

	/**
	 * Add a join clause to the delete statement.
	 * <br>
	 * <b>⚠⚠⚠ Any data that matches the join clause will be deleted ⚠⚠⚠</b>
	 * @param join the join clause
	 * @param <To> the target type
	 * @return the current Delete query, for chaining purposes.
	 */
	public <To extends Yopable> Delete<T> join(IJoin<T, To> join) {
		this.joins.add(join);
		return this;
	}

	/**
	 * Delete the whole data graph. Stop on transient fields.
	 * <br>
	 * <b>⚠⚠⚠ There must be no cycle in the data graph model ! ⚠⚠⚠</b>
	 * <br><br>
	 * <b>⚠⚠⚠ Any join previously set is cleared ! Please add transient fetch clause after this ! ⚠⚠⚠</b>
	 * @return the current DELETE request, for chaining purpose
	 */
	public Delete<T> joinAll() {
		this.joins.clear();
		AbstractJoin.joinAll(this.target, this.joins);
		return this;
	}

	/**
	 * Execute the DELETE query on the given connection
	 * @param connection the connection to use
	 */
	public void executeQuery(Connection connection) {
		Parameters parameters = new Parameters();
		Executor.executeQuery(connection, new Query(this.toSQL(parameters), parameters));
	}

	/**
	 * Generate the SQL DELETE query
	 * @param parameters the SQL paramters that will be populated with actual query parameters
	 * @return the SQL DELETE query string
	 */
	private String toSQL(Parameters parameters) {
		Context<T> root = Context.root(this.target);
		Set<Context.SQLColumn> columns = this.columns();

		Set<String> tables = columns
			.stream()
			.map(c -> StringUtils.substringBeforeLast(c.getQualifiedId(), "."))
			.collect(Collectors.toSet());

		this.joins.forEach(j -> joinTables(j, root, tables));

		// 1 single table : omit the 'columns' clause
		String columnsClause =
			tables.size() == 1
			? ""
			: Joiner.on(", ").join(tables.stream().map(t -> t + ".*").collect(Collectors.toSet()));

		// 1 single table : omit the 'as' clause
		String asClause = tables.size() == 1 ? "" : " as " + root.getPath();

		return " DELETE " + columnsClause
			+ " FROM " + root.getTableName() + asClause
			+ this.toSQLJoin(parameters);
	}

	/**
	 * Find all the columns from the DELETE query (search in current target type and join clauses)
	 * @return all the columns this query should delete
	 */
	private Set<Context.SQLColumn> columns() {
		Context<T> root = Context.root(this.target);
		Set<Context.SQLColumn> columns = root.getColumns();

		for (IJoin<T, ? extends Yopable> join : this.joins) {
			columns.addAll(join.columns(root, true));
		}
		return columns;
	}

	/**
	 * Create the SQL join clause for the DELETE statement.
	 * @return the SQL join clause
	 */
	private String toSQLJoin(Parameters parameters) {
		StringBuilder join = new StringBuilder();
		this.joins.forEach(j -> join.append(j.toSQL(Context.root(this.target), parameters, false)));
		return join.toString();
	}

	/**
	 * Recursively read all the join tables this query will impact.
	 * @param join    the join clause to read
	 * @param context the current context
	 * @param tables  the found join tables. Init with an empty set.
	 */
	@SuppressWarnings("unchecked")
	private static void joinTables(IJoin join, Context context, Set<String> tables) {
		tables.add(join.joinTableAlias(context));
		for (Object subJoin : join.getJoins()) {
			joinTables(((IJoin)subJoin), join.to(context), tables);
		}
	}

	/**
	 * A custom Join clause. For now, very similar to the original join clause.
	 * @param <From> the source type
	 * @param <To>   the target type
	 */
	public static class Join<From extends Yopable, To extends Yopable> extends org.yop.orm.query.Join<From, To> {
		public static <From extends Yopable, To extends Yopable> Join<From, To> to(Function<From, To> getter) {
			Join<From, To> join = new Join<>();
			join.getter = getter;
			return join;
		}

		@Override
		protected ToSQL.JoinType joinType() {
			return ToSQL.JoinType.LEFT_JOIN;
		}
	}

	/**
	 * A custom Join clause for multi-valued relationships. For now, very similar to the original join clause.
	 * @param <From> the source type
	 * @param <To>   the target type
	 */
	public static class JoinSet<From extends Yopable, To extends Yopable> extends org.yop.orm.query.JoinSet<From, To> {
		public static <From extends Yopable, To extends Yopable> JoinSet<From, To> to(
			Function<From, ? extends Collection<To>> getter) {
			JoinSet<From, To> join = new JoinSet<>();
			join.getter = getter;
			return join;
		}

		@Override
		protected ToSQL.JoinType joinType() {
			return ToSQL.JoinType.LEFT_JOIN;
		}
	}
}
