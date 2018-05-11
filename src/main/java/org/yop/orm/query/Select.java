package org.yop.orm.query;

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.yop.orm.evaluation.Comparison;
import org.yop.orm.evaluation.Evaluation;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.map.FirstLevelCache;
import org.yop.orm.map.IdMap;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.*;
import org.yop.orm.sql.adapter.IConnection;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Select : select instances of T from the database.
 * <br>
 * Standard SELECT clauses are accessible :
 * <ul>
 *     <li>FROM</li>
 *     <li>JOIN</li>
 *     <li>WHERE</li>
 * </ul>
 * This API aims at writing inlined requests.
 * <br>
 * Example :
 * <pre>
 * {@code
 * Select
 *   .from(Pojo.class)
 *   .joinAll()
 *   .join(JoinSet.to(Pojo::getOthers).join(JoinSet.to(Other::getPojos)))
 *   .where(Where.naturalId(reference))
 *   .execute(connection);
 * }
 * </pre>
 *
 * @param <T> the type to search for.
 */
public class Select<T extends Yopable> {

	public enum Strategy {IN, EXISTS}

	/** Select [what] FROM [table] [table_alias] [join clause] WHERE [where clause] [order by clause]*/
	private static final String SELECT = " SELECT {0} FROM {1} {2} {3} WHERE {4} {5}";

	/** Select distinct([what]) FROM [table] [table_alias] [join clause] WHERE [where clause] */
	private static final String SELECT_DISTINCT = " SELECT DISTINCT({0}) FROM {1} {2} {3} WHERE {4}";

	/** Default where clause is always added. So I don't have to check if the 'WHERE' keyword is required ;-) */
	private static final String DEFAULT_WHERE = " 1=1 ";

	/** Select root context : target class and SQL path **/
	private final Context<T> context;

	/** Where clauses */
	private final Where<T> where;

	/** Order by clause. Defaults to no order.  */
	private OrderBy<T> orderBy = new OrderBy<>();

	/** Join clauses */
	private Collection<IJoin<T, ? extends Yopable>> joins = new ArrayList<>();

	/** A custom first level cache that can be specified in some very specific cases */
	private FirstLevelCache cache;

	/**
	 * Private constructor. Please use {@link #from(Class)}
	 * @param from the target class (select from class)
	 */
	private Select(Class<T> from) {
		this.context = Context.root(from);
		this.where = new Where<>();
	}

	/**
	 * Complete constructor.
	 * @param from  root context
	 * @param where where clause
	 * @param joins joins clauses
	 */
	Select(Context<T> from, Where<T> where, Collection<IJoin<T, ? extends Yopable>> joins) {
		this.context = from;
		this.where = where;
		this.joins = joins;
	}

	/**
	 * Init select request.
	 * @param clazz the target class
	 * @param <Y> the target type
	 * @return a SELECT request instance
	 */
	public static <Y extends Yopable> Select<Y> from(Class<Y> clazz) {
		return new Select<>(clazz);
	}

	/**
	 * The where clause of this SELECT request
	 * @return the Where clause
	 */
	public Where<T> where() {
		return this.where;
	}

	/**
	 * (Left) join to a new type.
	 * @param join the join clause
	 * @param <R> the target join type
	 * @return the current SELECT request, for chaining purpose
	 */
	public <R extends Yopable> Select<T> join(IJoin<T, R> join) {
		this.joins.add(join);
		return this;
	}

	/**
	 * Fetch the whole data graph. Stop on transient fields.
	 * <br>
	 * <b>⚠⚠⚠ There must be no cycle in the data graph model ! ⚠⚠⚠</b>
	 * <br><br>
	 * <b>⚠⚠⚠ Any join previously set is cleared ! Please add transient fetch clause after this ! ⚠⚠⚠</b>
	 * @return the current SELECT request, for chaining purpose
	 */
	public Select<T> joinAll() {
		this.joins.clear();
		IJoin.joinAll(this.context.getTarget(), this.joins);
		return this;
	}

	/**
	 * Add an {@link OrderBy} clause.
	 * @param order the order by clause
	 * @return the current SELECT query, for chaining purposes.
	 */
	public Select<T> orderBy(OrderBy<T> order) {
		this.orderBy = order;
		return this;
	}

	/**
	 * Set a cache to use.
	 * <br>
	 * This was created for the {@link Recurse} queries where we would like to keep the same cache when recursing.
	 * @param cache the cache object to use
	 * @return the current SELECT request, for chaining purpose
	 */
	Select<T> setCache(FirstLevelCache cache) {
		this.cache = cache;
		return this;
	}

	/**
	 * Turn this SELECT query into a DELETE query, with the same {@link #joins} and {@link #where}.
	 * <br>
	 * <b>The where and joins clauses are not duplicated when creating the DELETE query !</b>
	 * @return a {@link Delete} query with this {@link Select} parameters (context, where and joins)
	 */
	public Delete<T> toDelete() {
		return new Delete<>(this.context.getTarget(), this.where, this.joins);
	}

	/**
	 * Execute the SELECT request using 2 queries :
	 * <ul>
	 *     <li>Find the matching T ids</li>
	 *     <li>Fetch the data</li>
	 * </ul>
	 * @param connection the connection to use for the request
	 * @return the SELECT result, as a set of T
	 * @throws YopSQLException An SQL error occured
	 * @throws org.yop.orm.exception.YopMapperException A ResultSet → Yopables mapping error occured
	 */
	public Set<T> executeWithTwoQueries(IConnection connection) {
		Set<Long> ids;

		Parameters parameters = new Parameters();
		String request = this.toSQLAnswerRequest(parameters);
		Query query = new SimpleQuery(request, Query.Type.SELECT, parameters);

		Set<T> elements = Executor.executeSelectQuery(
			connection,
			query,
			this.context.getTarget(),
			this.cache == null ? new FirstLevelCache() : this.cache
		);
		ids = elements.stream().map(Yopable::getId).distinct().collect(Collectors.toSet());

		if(ids.isEmpty()) {
			return new HashSet<>();
		}

		parameters = new Parameters();
		request = this.toSQLDataRequest(ids, parameters);
		query = new SimpleQuery(request, Query.Type.SELECT, parameters);
		return Executor.executeSelectQuery(
			connection,
			query,
			this.context.getTarget(),
			this.cache == null ? new FirstLevelCache() : this.cache
		);
	}

	/**
	 * Execute the SELECT request using 1 single query and a given strategy :
	 * <ul>
	 *     <li>{@link Strategy#EXISTS} : use a 'WHERE EXISTS' clause </li>
	 *     <li>{@link Strategy#IN} : use an 'IN' clause </li>
	 * </ul>
	 * @param connection the connection to use for the request
	 * @param strategy the strategy to use for the select query
	 * @return the SELECT result, as a set of T
	 * @throws YopSQLException An SQL error occured
	 * @throws org.yop.orm.exception.YopMapperException A ResultSet → Yopables mapping error occured
	 */
	public Set<T> execute(IConnection connection, Strategy strategy) {
		Parameters parameters = new Parameters();
		String request =
			strategy == Strategy.IN
			? this.toSQLDataRequestWithIN(parameters)
			: this.toSQLDataRequest(parameters);

		return Executor.executeSelectQuery(
			connection,
			new SimpleQuery(request, Query.Type.SELECT, parameters),
			this.context.getTarget(),
			this.cache == null ? new FirstLevelCache() : this.cache
		);
	}

	/**
	 * Execute the SELECT request using the {@link Strategy#EXISTS} strategy.
	 * @param connection the connection to use for the request
	 * @return the SELECT result, as a set of T
	 * @throws YopSQLException An SQL error occured
	 * @throws org.yop.orm.exception.YopMapperException A ResultSet → Yopables mapping error occured
	 */
	public Set<T> execute(IConnection connection) {
		return execute(connection, Strategy.EXISTS);
	}

	/**
	 * Convenience method that returns the first element of the results or null.
	 * <br>
	 * See {@link #execute(IConnection)}
	 * <br>
	 * TODO : effectively limit the SQL query result
	 */
	public T uniqueResult(IConnection connection) {
		Set<T> results = execute(connection, Strategy.EXISTS);
		return results.isEmpty() ? null : results.iterator().next();
	}

	/**
	 * Execute the SELECT request using the {@link Strategy#EXISTS} strategy to fetch the IDs of every target class.
	 * @param connection the connection to use for the request
	 * @return an {@link IdMap} instance, with a set of Ids for every class.
	 * @throws YopSQLException An SQL error occured
	 * @throws org.yop.orm.exception.YopMapperException A ResultSet → Yopables mapping error occured
	 */
	public IdMap executeForIds(IConnection connection) {
		Parameters parameters = new Parameters();
		String request = this.toSQLIDsRequest(parameters);
		Query query = new SimpleQuery(request, Query.Type.SELECT, parameters);

		return (IdMap) Executor.executeQuery(connection, query, IdMap.populateAction(this.context.getTarget()));
	}

	/**
	 * Add an evaluation to the where clause.
	 * @param evaluation the evaluation
	 * @return the current SELECT request, for chaining purposes
	 */
	public Select<T> where(Evaluation evaluation) {
		this.where.and(evaluation);
		return this;
	}

	/**
	 * Add several comparisons to the where clause, with an OR operator between them.
	 * @param compare the comparisons
	 * @return the current SELECT request, for chaining purposes
	 */
	public final Select<T> or(Comparison... compare) {
		this.where.or(compare);
		return this;
	}

	/**
	 * Get the target type table name from the @Table annotation
	 * @return the target class (T) table name.
	 */
	private String getTableName() {
		return this.context.getTableName();
	}

	/**
	 * Find all the columns to select (search in current target type and join clauses if required)
	 * @param addJoinClauseColumns true to add the columns from the join clauses
	 * @return the columns to select
	 */
	private Set<Context.SQLColumn> columns(boolean addJoinClauseColumns) {
		Set<Context.SQLColumn> columns = this.context.getColumns();

		if (addJoinClauseColumns) {
			for (IJoin<T, ? extends Yopable> join : this.joins) {
				columns.addAll(join.columns(this.context, true));
			}
		}
		return columns;
	}

	/**
	 * Find the target type ID alias
	 * @return the target type T id alias
	 */
	private String idAlias() {
		return this.idAlias(this.context.tableAlias());
	}

	/**
	 * Find the target type ID alias
	 * @param prefix a context prefxi
	 * @return the target type T id alias that will be added before the computed id alias
	 */
	private String idAlias(String prefix) {
		return this.context.idAlias(prefix);
	}

	/**
	 * Create the SQL columns clause
	 * @param addJoinClauseColumns true to fetch the columns from the join clauses
	 * @return the SQL columns clause
	 */
	private String toSQLColumnsClause(boolean addJoinClauseColumns) {
		Set<Context.SQLColumn> columns = this.columns(addJoinClauseColumns);
		return
			columns.isEmpty()
			? "*"
			: Joiner.on(",").join(columns.stream().map(Context.SQLColumn::toSQL).collect(Collectors.toList()));
	}

	/**
	 * Create the SQL columns clause <b>only for ID columns </b>!
	 * @return the SQL columns clause for ID columns
	 */
	private String toSQLIdColumnsClause() {
		Set<Context.SQLColumn> columns = this
			.columns(true)
			.stream()
			.filter(Context.SQLColumn::isId)
			.collect(Collectors.toSet());

		return
			columns.isEmpty()
			? "*"
			: Joiner.on(",").join(columns.stream().map(Context.SQLColumn::toSQL).collect(Collectors.toList()));
	}

	/**
	 * Create the SQL join clause.
	 * @param evaluate true to add the where clauses to the join clauses
	 * @return the SQL join clause
	 */
	private JoinClause.JoinClauses toSQLJoin(boolean evaluate) {
		return ToSQL.toSQLJoin(this.joins, this.context, evaluate);
	}

	/**
	 * Build the WHERE clause from {@link #where} and for this {@link #context}.
	 * <b>⚠ Does not prefix with the 'WHERE' keyword ! ⚠</b>
	 * @param parameters the query parameters that will be populated with the WHERE clause parameters
	 * @return the Where clause for {@link #where} and {@link #context}
	 */
	private String toSQLWhere(Parameters parameters) {
		return this.where.toSQL(this.context, parameters);
	}

	/**
	 * 2 query strategy : create the SQL 'answer' request : only find the target type that matches.
	 * @return the SQL 'answer' request.
	 */
	private String toSQLAnswerRequest(Parameters parameters) {
		JoinClause.JoinClauses joinClauses = this.toSQLJoin(true);
		return select(
			this.toSQLColumnsClause(false),
			this.getTableName(),
			this.context.getPath(),
			joinClauses.toSQL(parameters),
			Where.toSQL(this.toSQLWhere(parameters), joinClauses.toSQLWhere(parameters)),
			this.orderBy.toSQL(this.context.getTarget())
		);
	}

	/**
	 * 2 query strategy : create the SQL 'data' request : fetch all data (including joins) for the given ids.
	 * <br>
	 * See {@link #toSQLAnswerRequest(Parameters)}
	 * @return the SQL 'data' request.
	 */
	private String toSQLDataRequest(Set<Long> ids, Parameters parameters) {
		JoinClause.JoinClauses joinClauses = this.toSQLJoin(false);
		return select(
			this.toSQLColumnsClause(true),
			this.getTableName(),
			this.context.getPath(),
			joinClauses.toSQL(parameters),
			Where.toSQL(this.idAlias() + " IN (" + Joiner.on(",").join(ids) + ") ", joinClauses.toSQLWhere(parameters)),
			this.orderBy.toSQL(this.context.getTarget())
		);
	}

	/**
	 * Single query strategy with EXISTS : create the SQL 'data' request.
	 * @return the SQL 'data' request.
	 */
	private String toSQLDataRequest(Parameters parameters) {
		// First we have to build a 'select ids' query for the EXISTS subquery
		// We copy the current 'Select' object to add a suffix to the context
		// We link the EXISTS subquery to the global one (id = subquery.id)
		// This is not very elegant, I must confess
		Select<T> copyForAlias = new Select<>(this.context.copy("_0"), this.where, this.joins);

		String whereClause = Where.toSQL(
			copyForAlias.toSQLWhere(parameters),
			this.idAlias() + " = " + copyForAlias.idAlias()
		);

		JoinClause.JoinClauses joinClauses = copyForAlias.toSQLJoin(true);
		String existsSubSelect = selectDistinct(
			copyForAlias.idAlias(),
			copyForAlias.getTableName(),
			copyForAlias.context.getPath(),
			joinClauses.toSQL(parameters),
			Where.toSQL(whereClause, joinClauses.toSQLWhere(parameters))
		);

		// Now we can build the global query that fetches the data when the EXISTS clause matches
		joinClauses = this.toSQLJoin(false);
		return select(
			this.toSQLColumnsClause(true),
			this.getTableName(),
			this.context.getPath(),
			joinClauses.toSQL(parameters),
			Where.toSQL(" EXISTS (" + existsSubSelect + ") ", joinClauses.toSQLWhere(parameters)),
			this.orderBy.toSQL(this.context.getTarget())
		);
	}

	/**
	 * Single query strategy with EXISTS : create the SQL 'data' request that only returns ID columns.
	 * @return the SQL 'data' request.
	 */
	private String toSQLIDsRequest(Parameters parameters) {
		// First we have to build a 'select ids' query for the EXISTS subquery
		// We copy the current 'Select' object to add a suffix to the context
		// We link the EXISTS subquery to the global one (id = subquery.id)
		// This is not very elegant, I must confess
		Select<T> copyForAlias = new Select<>(this.context.copy("_0"), this.where, this.joins);

		String whereClause = Where.toSQL(
			copyForAlias.toSQLWhere(parameters),
			this.idAlias() + " = " + copyForAlias.idAlias()
		);

		JoinClause.JoinClauses joinClauses = copyForAlias.toSQLJoin(true);
		String existsSubSelect = selectDistinct(
			copyForAlias.idAlias(),
			copyForAlias.getTableName(),
			copyForAlias.context.getPath(),
			joinClauses.toSQL(parameters),
			Where.toSQL(whereClause, joinClauses.toSQLWhere(parameters))
		);

		// Now we can build the global query that fetches the IDs for every type when the EXISTS clause matches
		joinClauses = this.toSQLJoin(false);
		return select(
			this.toSQLIdColumnsClause(),
			this.getTableName(),
			this.context.getPath(),
			joinClauses.toSQL(parameters),
			Where.toSQL(" EXISTS (" + existsSubSelect + ") ", joinClauses.toSQLWhere(parameters)),
			this.orderBy.toSQL(this.context.getTarget())
		);
	}

	/**
	 * Single query strategy with IN : create the SQL 'data' request.
	 * @return the SQL 'data' request.
	 */
	private String toSQLDataRequestWithIN(Parameters parameters) {
		String path = this.context.getPath();
		JoinClause.JoinClauses joinClauses = this.toSQLJoin(true);
		String inSubQuery = select(
			this.idAlias(),
			this.getTableName(),
			path,
			joinClauses.toSQL(parameters),
			Where.toSQL(this.toSQLWhere(parameters), joinClauses.toSQLWhere(parameters)),
			""
		);

		joinClauses = this.toSQLJoin(false);
		return select(
			this.toSQLColumnsClause(true),
			this.getTableName(),
			path,
			joinClauses.toSQL(parameters),
			Where.toSQL(this.idAlias() + " IN (" + inSubQuery + ")", joinClauses.toSQLWhere(parameters)),
			this.orderBy.toSQL(this.context.getTarget())
		);
	}

	/**
	 * Build the Select query from component clauses
	 * @param what        Mandatory. Columns clause.
	 * @param from        Mandatory. Target table.
	 * @param as          Mandatory. Target table alias.
	 * @param joinClause  Optional. Join clause.
	 * @param whereClause Optional. Where clause.
	 * @return the SQL select query.
	 */
	private String select(
		String what,
		String from,
		String as,
		String joinClause,
		String whereClause,
		String orderClause) {
		return MessageFormat.format(
			SELECT,
			what,
			from,
			as,
			joinClause,
			StringUtils.isBlank(whereClause) ? DEFAULT_WHERE : whereClause,
			orderClause
		);
	}

	/**
	 * Build the 'distinct' Select query from component clauses.
	 * @param what        Mandatory. Column clause that is to be distinct.
	 * @param from        Mandatory. Target table.
	 * @param as          Mandatory. Target table alias.
	 * @param joinClause  Optional. Join clause.
	 * @param whereClause Optional. Where clause.
	 * @return the SQL 'distinct' select query.
	 */
	private String selectDistinct(
		String what,
		String from,
		String as,
		String joinClause,
		String whereClause) {
		return MessageFormat.format(
			SELECT_DISTINCT,
			what,
			from,
			as,
			joinClause,
			StringUtils.isBlank(whereClause) ? DEFAULT_WHERE : whereClause
		);
	}
}
