package org.yop.orm.query;

import com.google.common.base.Joiner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.exception.YopSerializableQueryException;
import org.yop.orm.map.FirstLevelCache;
import org.yop.orm.map.IdMap;
import org.yop.orm.model.JsonAble;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.*;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.MessageUtil;
import org.yop.orm.util.Reflection;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
public class Select<T extends Yopable> extends WithJoins<Select<T>, T> implements JsonAble {

	private static final Logger logger = LoggerFactory.getLogger(Select.class);

	public enum Strategy {IN, EXISTS}

	/** Select [what] FROM [table] [table_alias] [join clause] WHERE [where clause] [order by clause] [extra] */
	private static final String SELECT = " SELECT {0} FROM {1} {2} {3} WHERE {4} {5} {6}";

	/** Select distinct([what]) FROM [table] [table_alias] [join clause] WHERE [where clause] [extra] */
	private static final String SELECT_DISTINCT = " SELECT DISTINCT({0}) FROM {1} {2} {3} WHERE {4} {5}";

	/** COUNT(DISTINCT :idColumn) column selection */
	private static final String COUNT_DISTINCT = " COUNT(DISTINCT {0}) ";

	/** Order by clause. Defaults to no order.  */
	private OrderBy<T> orderBy = new OrderBy<>();

	/** A custom first level cache that can be specified in some very specific cases */
	private FirstLevelCache cache;

	/** Page : from offset x, limit to y rows */
	private Paging paging = new Paging(null, null);

	/**
	 * Private constructor. Please use {@link #from(Class)}
	 * @param from the target class (select from class)
	 */
	private Select(Class<T> from) {
		super(Context.root(from));
	}

	/**
	 * Complete constructor.
	 * @param from  root context
	 * @param where where clause
	 * @param joins joins clauses
	 */
	Select(Context<T> from, Where<T> where, Collection<IJoin<T, ? extends Yopable>> joins) {
		super(from);
		this.where = where;
		this.joins.addAll(joins);
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
	 * Serialize the current Select query into a Gson JSON object.
	 * @return the JSON representation of the query
	 */
	public JsonObject toJSON() {
		return this.toJSON(this.context);
	}

	@Override
	public <U extends Yopable> JsonObject toJSON(Context<U> context) {
		JsonObject out = (JsonObject) JsonAble.super.toJSON(context);
		out.addProperty("target", this.context.getTarget().getCanonicalName());
		return out;
	}

	/**
	 * Create a Select query from the given json String representation.
	 * @param json         the Select query JSON representation
	 * @param config       the SQL config (sql separator, use batch inserts...)
	 * @param classLoaders the class loaders to use to try to load the target resource
	 * @param <T> the target context type. This should match the one set in the JSON representation of the query !
	 * @return a new Select query whose state is set from its JSON representation
	 */
	public static <T extends Yopable> Select<T> fromJSON(String json, Config config, ClassLoader... classLoaders) {
		try {
			JsonParser parser = new JsonParser();
			JsonObject selectJSON = (JsonObject) parser.parse(json);
			String targetClassName = selectJSON.getAsJsonPrimitive("target").getAsString();
			Class<T> target = Reflection.forName(targetClassName, classLoaders);
			Select<T> select = Select.from(target);
			select.fromJSON(select.context, selectJSON, config);
			return select;
		} catch (RuntimeException e) {
			throw new YopSerializableQueryException(
				"Could not create query from JSON [" + StringUtils.abbreviate(json, 30) + "]", e
			);
		}
	}

	/**
	 * What is the target Yopable of this Select query ?
	 * @return {@link Context#getTarget()} of {@link #context}
	 */
	public Class<T> getTarget() {
		return this.context.getTarget();
	}

	/**
	 * Add a paging directive.
	 * <br>
	 * See {@link Paging} and {@link Config#getPagingMethod()}.
	 * @param offset  from which offset to start fetching. If null → start from first offset.
	 * @param results the number of results to fetch. If null → no limit.
	 * @return the current SELECT request, for chaining purpose
	 */
	public Select<T> page(Long offset, Long results) {
		this.paging = new Paging(offset, results);
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
	 *     <li>
	 *         If paging is activated and config is set to {@link org.yop.orm.query.Paging.Method#TWO_QUERIES} :
	 *         filter the matching ids.
	 *     </li>
	 *     <li>Fetch the data</li>
	 * </ul>
	 * @param connection the connection to use for the request
	 * @return the SELECT result, as a set of T
	 * @throws YopSQLException An SQL error occurred
	 * @throws org.yop.orm.exception.YopMapperException A ResultSet → Yopables mapping error occurred
	 */
	public Set<T> executeWithTwoQueries(IConnection connection) {
		List<Long> ids;

		Parameters parameters = new Parameters();
		String request = this.toSQLAnswerRequest(parameters, connection.config());
		Query query = new SimpleQuery(request, Query.Type.SELECT, parameters, connection.config());

		Set<T> elements = Executor.executeSelectQuery(
			connection,
			query,
			this.context.getTarget(),
			this.cache == null ? new FirstLevelCache(connection.config()) : this.cache
		);
		ids = elements.stream().map(Yopable::getId).distinct().collect(Collectors.toList());

		if (this.paging.isPaging()) {
			ids = this.paging.pageIds(ids);
		}

		if(ids.isEmpty()) {
			return new HashSet<>();
		}

		parameters = new Parameters();
		request = this.toSQLDataRequest(new HashSet<>(ids), parameters, connection.config());
		query = new SimpleQuery(request, Query.Type.SELECT, parameters, connection.config());
		return Executor.executeSelectQuery(
			connection,
			query,
			this.context.getTarget(),
			this.cache == null ? new FirstLevelCache(connection.config()) : this.cache
		);
	}

	/**
	 * Execute the SELECT request using 1 single query and a given strategy :
	 * <ul>
	 *     <li>{@link Strategy#EXISTS} : use a 'WHERE EXISTS' clause, unless paging is active. </li>
	 *     <li>{@link Strategy#IN} : use an 'IN' clause </li>
	 * </ul>
	 * ⚠⚠⚠ <b>
	 *     If {@link #paging} is set and {@link Config#getPagingMethod()} is {@link Paging.Method#TWO_QUERIES},
	 *     the strategy is ignored and we actually use {@link #executeWithTwoQueries(IConnection)}.
	 * </b> ⚠⚠⚠
	 * @param connection the connection to use for the request
	 * @param strategy the strategy to use for the select query
	 * @return the SELECT result, as a set of T
	 * @throws YopSQLException An SQL error occurred
	 * @throws org.yop.orm.exception.YopMapperException A ResultSet → Yopables mapping error occurred
	 */
	public Set<T> execute(IConnection connection, Strategy strategy) {
		if (this.paging.isPaging() && connection.config().getPagingMethod() == Paging.Method.TWO_QUERIES) {
			logger.warn(
				"Paging method is set to [{}] → we are going to use 2 queries and page on IDs",
				connection.config().getPagingMethod().name()
			);
			return this.executeWithTwoQueries(connection);
		}

		Parameters parameters = new Parameters();
		String request =
			(strategy == Strategy.IN || this.paging.isPaging())
			? this.toSQLDataRequestWithIN(parameters, connection.config())
			: this.toSQLDataRequestWithEXISTS(parameters, connection.config());

		return Executor.executeSelectQuery(
			connection,
			new SimpleQuery(request, Query.Type.SELECT, parameters, connection.config()),
			this.context.getTarget(),
			this.cache == null ? new FirstLevelCache(connection.config()) : this.cache
		);
	}

	/**
	 * Execute the SELECT request using the {@link Strategy#EXISTS} strategy.
	 * @param connection the connection to use for the request
	 * @return the SELECT result, as a set of T
	 * @throws YopSQLException An SQL error occurred
	 * @throws org.yop.orm.exception.YopMapperException A ResultSet → Yopables mapping error occurred
	 */
	public Set<T> execute(IConnection connection) {
		return this.execute(connection, Strategy.EXISTS);
	}

	/**
	 * Convenience method that returns the first element of the results or null.
	 * <br>
	 * See {@link #execute(IConnection)}
	 * <br>
	 * TODO : effectively limit the SQL query result
	 * @param connection the connection to use for the request
	 * @return the SELECT result, as an unique T
	 */
	public T uniqueResult(IConnection connection) {
		Set<T> results = this.execute(connection, Strategy.EXISTS);
		return results.isEmpty() ? null : results.iterator().next();
	}

	/**
	 * Count the elements that match the query.
	 * <br>
	 * We actually use {@link #toSQLIDsRequest(Parameters, boolean, Config)}
	 * to get distinct IDs and count the number of rows.
	 * @param connection the connection to use for the request
	 * @return the SELECT result, as an unique T
	 */
	public Long count(IConnection connection) {
		Parameters parameters = new Parameters();
		String request = this.toSQLIDsRequest(parameters, true, connection.config());

		return Executor.executeQuery(
			connection,
			new SimpleQuery(request, Query.Type.SELECT, parameters, connection.config()),
			results -> {results.getCursor().next(); return results.getCursor().getLong(1);}
		);
	}

	/**
	 * Execute the SELECT request using the {@link Strategy#EXISTS} strategy to fetch the IDs of every target class.
	 * @param connection the connection to use for the request
	 * @return an {@link IdMap} instance, with a set of Ids for every class.
	 * @throws YopSQLException An SQL error occurred
	 * @throws org.yop.orm.exception.YopMapperException A ResultSet → Yopables mapping error occurred
	 */
	public IdMap executeForIds(IConnection connection) {
		Parameters parameters = new Parameters();
		String request = this.toSQLIDsRequest(parameters, false, connection.config());
		Query query = new SimpleQuery(request, Query.Type.SELECT, parameters, connection.config());

		return Executor.executeQuery(
			connection,
			query,
			IdMap.populateAction(this.context.getTarget(), connection.config())
		);
	}

	/**
	 * Get the target type table name from the @Table annotation
	 * @return the target class (T) table name.
	 */
	private String getTableName() {
		return this.context.getTableName();
	}

	/**
	 * Find the target type ID alias
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the target type T id alias
	 */
	private String idAlias(Config config) {
		return this.idAlias(this.context.tableAlias(), config);
	}

	/**
	 * Find the target type ID alias
	 * @param prefix a context prefix
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the target type T id alias that will be added before the computed id alias
	 */
	private String idAlias(String prefix, Config config) {
		return this.context.idAlias(prefix, config);
	}

	/**
	 * Create the SQL columns clause
	 * @param addJoinClauseColumns true to fetch the columns from the join clauses
	 * @param config               the SQL config (sql separator, use batch inserts...)
	 * @return the SQL columns clause
	 */
	private String toSQLColumnsClause(boolean addJoinClauseColumns, Config config) {
		Set<Context.SQLColumn> columns = this.columns(addJoinClauseColumns, config);
		return
			columns.isEmpty()
			? "*"
			: Joiner.on(",").join(columns.stream().map(Context.SQLColumn::toSQL).collect(Collectors.toList()));
	}

	/**
	 * Create the SQL columns clause <b>only for ID columns </b>!
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the SQL columns clause for ID columns
	 */
	private String toSQLIdColumnsClause(Config config) {
		Set<Context.SQLColumn> columns = this
			.columns(true, config)
			.stream()
			.filter(Context.SQLColumn::isId)
			.collect(Collectors.toSet());

		return
			columns.isEmpty()
			? "*"
			: Joiner.on(",").join(columns.stream().map(Context.SQLColumn::toSQL).collect(Collectors.toList()));
	}

	/**
	 * Build the WHERE clause from {@link #where} and for this {@link #context}.
	 * <b>⚠ Does not prefix with the 'WHERE' keyword ! ⚠</b>
	 * @param parameters the query parameters that will be populated with the WHERE clause parameters
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the Where clause for {@link #where} and {@link #context}
	 */
	private String toSQLWhere(Parameters parameters, Config config) {
		return this.where.toSQL(this.context, parameters, config);
	}

	/**
	 * 2 query strategy : create the SQL 'answer' request : only select columns for the target type.
	 * <br>
	 * Joins are added to the query but none of their columns are selected.
	 * <br>
	 * Then, ids should be extracted and a second query should be used to fetch the whole data graph.
	 * <br>
	 * See {@link #executeWithTwoQueries(IConnection)} and {@link #toSQLDataRequest(Set, Parameters, Config)}.
	 * @param parameters the query parameters that will be populated with the WHERE clause parameters
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the SQL 'answer' request.
	 */
	private String toSQLAnswerRequest(Parameters parameters, Config config) {
		JoinClause.JoinClauses joinClauses = this.toSQLJoin(true, config);
		return this.select(
			this.toSQLColumnsClause(false, config),
			this.getTableName(),
			this.context.getPath(config),
			joinClauses.toSQL(parameters),
			Where.toSQL(this.toSQLWhere(parameters, config), joinClauses.toSQLWhere(parameters)),
			OrderBy.<T>orderById(true).toSQL(this.context.getTarget(), config)
		);
	}

	/**
	 * 2 query strategy : create the SQL 'data' request : fetch all data (including joins) for the given ids.
	 * <br>
	 * See {@link #toSQLAnswerRequest(Parameters, Config)}
	 * @param parameters the query parameters that will be populated with the WHERE clause parameters
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the SQL 'data' request.
	 */
	private String toSQLDataRequest(Set<Long> ids, Parameters parameters, Config config) {
		JoinClause.JoinClauses joinClauses = this.toSQLJoin(false, config);
		String whereClause = Where.toSQL(
			this.idAlias(config) + " IN (" + Joiner.on(",").join(ids) + ") ",
			joinClauses.toSQLWhere(parameters)
		);

		return this.select(
			this.toSQLColumnsClause(true, config),
			this.getTableName(),
			this.context.getPath(config),
			joinClauses.toSQL(parameters),
			whereClause,
			this.orderBy.toSQL(this.context.getTarget(), config)
		);
	}

	/**
	 * Single query strategy with EXISTS : create the SQL 'data' request.
	 * <br>
	 * It uses a subquery to find the target type results, attached to the main query with a 'WHERE EXISTS' clause.
	 * @param parameters the query parameters - will be populated with the actual request parameters
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the SQL 'data' request.
	 */
	private String toSQLDataRequestWithEXISTS(Parameters parameters, Config config) {
		// First we have to build a 'select ids' query for the EXISTS subquery
		// We copy the current 'Select' object to add a suffix to the context
		// We link the EXISTS subquery to the global one (id = subquery.id)
		// This is not very elegant, I must confess
		Select<T> copyForAlias = new Select<>(this.context.copy("_0"), this.where, this.joins);

		String whereClause = Where.toSQL(
			copyForAlias.toSQLWhere(parameters, config),
			this.idAlias(config) + " = " + copyForAlias.idAlias(config)
		);

		JoinClause.JoinClauses joinClauses = copyForAlias.toSQLJoin(true, config);
		String existsSubSelect = this.selectDistinct(
			copyForAlias.idAlias(config),
			copyForAlias.getTableName(),
			copyForAlias.context.getPath(config),
			joinClauses.toSQL(parameters),
			Where.toSQL(whereClause, joinClauses.toSQLWhere(parameters)),
			this.paging.toSQL(this.context, parameters, config)
		);

		// Now we can build the global query that fetches the data when the EXISTS clause matches
		joinClauses = this.toSQLJoin(false, config);
		whereClause = Where.toSQL(
			" EXISTS (" + existsSubSelect + ") ",
			joinClauses.toSQLWhere(parameters)
		);
		return this.select(
			this.toSQLColumnsClause(true, config),
			this.getTableName(),
			this.context.getPath(config),
			joinClauses.toSQL(parameters),
			whereClause,
			this.orderBy.toSQL(this.context.getTarget(), config)
		);
	}

	/**
	 * Create a SQL request that only returns ID columns.
	 * <br>
	 * This can be used as the subquery for an EXISTS clause or simply to count results.
	 * @param parameters the query parameters - will be populated with the actual request parameters
	 * @param count      if true, the request will be on 'COUNT(DISTINCT id_column)' instead of the actual columns
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the SQL 'data' request.
	 */
	private String toSQLIDsRequest(Parameters parameters, boolean count, Config config) {
		// First we have to build a 'select ids' query for the EXISTS subquery
		// We copy the current 'Select' object to add a suffix to the context
		// We link the EXISTS subquery to the global one (id = subquery.id)
		// This is not very elegant, I must confess
		Select<T> copyForAlias = new Select<>(this.context.copy("_0"), this.where, this.joins);

		String whereClause = Where.toSQL(
			copyForAlias.toSQLWhere(parameters, config),
			this.idAlias(config) + " = " + copyForAlias.idAlias(config)
		);

		JoinClause.JoinClauses joinClauses = copyForAlias.toSQLJoin(true, config);
		String existsSubSelect = this.selectDistinct(
			copyForAlias.idAlias(config),
			copyForAlias.getTableName(),
			copyForAlias.context.getPath(config),
			joinClauses.toSQL(parameters),
			Where.toSQL(whereClause, joinClauses.toSQLWhere(parameters)),
			this.paging.toSQL(this.context, parameters, config)
		);

		// Now we can build the global query that fetches the IDs for every type when the EXISTS clause matches
		joinClauses = this.toSQLJoin(false, config);
		return this.select(
			count ? MessageFormat.format(COUNT_DISTINCT, this.idAlias(config))  : this.toSQLIdColumnsClause(config),
			this.getTableName(),
			this.context.getPath(config),
			joinClauses.toSQL(parameters),
			Where.toSQL(" EXISTS (" + existsSubSelect + ") ", joinClauses.toSQLWhere(parameters)),
			this.orderBy.toSQL(this.context.getTarget(), config)
		);
	}

	/**
	 * Single query strategy with IN : create the SQL 'data' request.
	 * <br>
	 * It uses a subquery to find IDs of the target type, inside and 'id IN' clause.
	 * @return the SQL 'data' request.
	 */
	private String toSQLDataRequestWithIN(Parameters parameters, Config config) {
		String path = this.context.getPath(config);
		JoinClause.JoinClauses joinClauses = this.toSQLJoin(true, config);
		String inSubQuery = this.selectDistinct(
			this.idAlias(config),
			this.getTableName(),
			path,
			joinClauses.toSQL(parameters),
			Where.toSQL(this.toSQLWhere(parameters, config), joinClauses.toSQLWhere(parameters)),
			this.paging.toSQLOrderBy(this.context, config),
			this.paging.toSQL(this.context, parameters, config)
		);

		joinClauses = this.toSQLJoin(false, config);
		String whereClause = Where.toSQL(
			this.idAlias(config) + " IN (" + inSubQuery + ")",
			joinClauses.toSQLWhere(parameters)
		);
		return this.select(
			this.toSQLColumnsClause(true, config),
			this.getTableName(),
			path,
			joinClauses.toSQL(parameters),
			whereClause,
			this.orderBy.toSQL(this.context.getTarget(), config)
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
		String orderClause,
		String... extras) {
		String extra = MessageUtil.concat(extras);
		return MessageFormat.format(
			SELECT,
			what,
			from,
			as,
			joinClause,
			StringUtils.isBlank(whereClause) ? DEFAULT_WHERE : whereClause,
			orderClause,
			extra
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
		String whereClause,
		String... extras) {
		String extra = MessageUtil.concat(extras);
		return MessageFormat.format(
			SELECT_DISTINCT,
			what,
			from,
			as,
			joinClause,
			StringUtils.isBlank(whereClause) ? DEFAULT_WHERE : whereClause,
			extra
		);
	}
}
