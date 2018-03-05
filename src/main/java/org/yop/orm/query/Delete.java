package org.yop.orm.query;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import org.yop.orm.evaluation.Evaluation;
import org.yop.orm.map.IdMap;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;

import java.sql.Connection;
import java.text.MessageFormat;
import java.util.*;
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

	private static final String DELETE = " DELETE {0} FROM {1} {2} WHERE {3} ";
	private static final String DEFAULT_WHERE = " 1=1 ";

	private Class<T> target;
	private Where<T> where;
	private Collection<IJoin<T, ? extends Yopable>> joins = new ArrayList<>();

	private Delete(Class<T> target) {
		this.target = target;
		this.where = new Where<>();
	}

	/**
	 * Complete constructor.
	 * Package protected so a {@link Select} query can be turned into a delete.
	 * @param target target class
	 * @param where  where clause
	 * @param joins  join clauses
	 */
	Delete(Class<T> target, Where<T> where, Collection<IJoin<T, ? extends Yopable>> joins) {
		this(target);
		this.where = where;
		this.joins.addAll(joins);
	}

	public static <T extends Yopable> Delete<T> from(Class<T> target) {
		return new Delete<>(target);
	}

	/**
	 * Turn this DELETE query into a SELECT query, with the same {@link #joins} and {@link #where}.
	 * <br>
	 * <b>The where and joins clauses are not duplicated when creating the SELECT query !</b>
	 * @return a {@link Select} query with this {@link Delete} parameters (context, where and joins)
	 */
	public Select<T> toSelect() {
		return new Select<>(Context.root(this.target), this.where, this.joins);
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
	 * Execute the DELETE queries on the given connection, 1 query per table.
	 * <br>
	 * This method can be useful if the target DBMS does not support deleting data from multiple tables in 1 query.
	 * <br>
	 * yes, SQLite, that's you I'm talking about.
	 * @param connection the connection to use
	 */
	public void executeQueries(Connection connection) {
		Select<T> select = this.toSelect();
		IdMap idMap = select.executeForIds(connection);
		List<Query> queries = new ArrayList<>();

		for (Map.Entry<Class<? extends Yopable>, Set<Long>> entry : idMap.entries()) {
			Parameters parameters = new Parameters();
			String sql = Delete.from(entry.getKey()).where(Where.id(entry.getValue())).toSQL(parameters);
			queries.add(new Query(sql, parameters));
		}

		for (Query query : queries) {
			Executor.executeQuery(connection, query);
		}
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

		// 1 single table : omit the 'columns' clause, the 'as' clause and use a Fake context for the where clause
		// (This was to deal with SQLite. I am not very proud of this.)
		String columnsClause = "";
		String asClause = "";
		Context<T> context = new Context.FakeContext<>(root, root.getTableName());
		if (tables.size() > 1) {
			columnsClause = Joiner.on(", ").join(tables.stream().map(t -> t + ".*").collect(Collectors.toSet()));
			asClause = " as " + root.getPath();
			context = root;
		}

		String whereClause = this.where.toSQL(context, parameters);
		return MessageFormat.format(
			DELETE,
			columnsClause,
			root.getTableName() + asClause,
			this.toSQLJoin(parameters),
			StringUtils.isBlank(whereClause) ? DEFAULT_WHERE : whereClause
		);
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
		return ToSQL.toSQLJoin(this.joins, Context.root(this.target), parameters, false);
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
