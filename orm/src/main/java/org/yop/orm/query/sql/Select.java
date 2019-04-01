package org.yop.orm.query.sql;

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
import org.yop.orm.query.Context;
import org.yop.orm.query.join.IJoin;
import org.yop.orm.sql.*;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.reflection.Reflection;

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
 *   .join(SQLJoin.toN(Pojo::getOthers).join(SQLJoin.toN(Other::getPojos)))
 *   .where(Where.naturalId(reference))
 *   .execute(connection);
 * }
 * </pre>
 *
 * @param <T> the type to search for.
 */
public class Select<T extends Yopable> extends WhereRequest<Select<T>, T> implements JsonAble {

	private static final Logger logger = LoggerFactory.getLogger(Select.class);

	public enum Strategy {IN, EXISTS}

	/** Order by clause. Defaults to no order.  */
	private OrderBy<T> orderBy = new OrderBy<>();

	/** A custom first level cache that can be specified in some very specific cases */
	private FirstLevelCache cache;

	/** Page : from offset x, limit to y rows */
	private Paging paging = new Paging(null, null);

	/** Lock the Select results (i.e. SELECT... FOR UPDATE) */
	private boolean lock = false;

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
	 * Lock the results of the current query, i.e. SELECT... FOR UPDATE. Use with caution.
	 * <br> <b>Some DBMS does not support locking.</b>
	 * @return the current SELECT query, for chaining purposes.
	 */
	public Select<T> lock() {
		this.lock = true;
		return this;
	}

	/**
	 * Set a cache to use.
	 * <br>
	 * This was created for {@link Hydrate#recurse()} where we would like to keep the same cache when recursing.
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
	 *         If paging is activated and config is set to {@link org.yop.orm.query.sql.Paging.Method#TWO_QUERIES} :
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
		List<Comparable> ids;

		SQLPart request = this.toSQLAnswerRequest(connection.config());
		Query query = new SimpleQuery(request, Query.Type.SELECT, connection.config());

		Set<T> elements = Executor.executeSelectQuery(
			connection,
			query,
			this.context.getTarget(),
			this.cache == null ? new FirstLevelCache() : this.cache
		);
		ids = elements.stream().map(Yopable::getId).distinct().collect(Collectors.toList());

		if (this.paging.isPaging()) {
			ids = this.paging.pageIds(ids);
		}

		if(ids.isEmpty()) {
			return new HashSet<>();
		}

		request = this.toSQLDataRequest(new HashSet<>(ids), connection.config());
		query = new SimpleQuery(request, Query.Type.SELECT, connection.config());
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

		SQLPart request =
			(strategy == Strategy.IN || this.paging.isPaging())
			? this.toSQLDataRequestWithIN(connection.config())
			: this.toSQLDataRequestWithEXISTS(connection.config());

		return Executor.executeSelectQuery(
			connection,
			new SimpleQuery(request, Query.Type.SELECT, connection.config()),
			this.context.getTarget(),
			this.cache == null ? new FirstLevelCache() : this.cache
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
	 * We actually use {@link #toSQLIDsRequest(boolean, Config)}
	 * to get distinct IDs and count the number of rows.
	 * @param connection the connection to use for the request
	 * @return the SELECT result, as an unique T
	 */
	public Long count(IConnection connection) {
		SQLPart request = this.toSQLIDsRequest(true, connection.config());

		return Executor.executeQuery(
			connection,
			new SimpleQuery(request, Query.Type.SELECT, connection.config()),
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
		SQLPart request = this.toSQLIDsRequest(false, connection.config());
		Query query = new SimpleQuery(request, Query.Type.SELECT, connection.config());

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
	 * Create the SQL columns clause
	 * @param addJoinClauseColumns true to fetch the columns from the join clauses
	 * @param config               the SQL config (sql separator, use batch inserts...)
	 * @return the SQL columns clause
	 */
	private CharSequence toSQLColumnsClause(boolean addJoinClauseColumns, Config config) {
		Set<SQLColumn> columns = this.columns(addJoinClauseColumns, config);
		return columns.isEmpty() ? "*" : SQLPart.join(",", columns);
	}

	/**
	 * Create the SQL columns clause <b>only for ID columns </b>!
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the SQL columns clause for ID columns
	 */
	private CharSequence toSQLIdColumnsClause(Config config) {
		Set<SQLColumn> columns = this.columns(true, config).stream().filter(SQLColumn::isId).collect(Collectors.toSet());
		return columns.isEmpty() ? "*" : SQLPart.join(",", columns);
	}

	/**
	 * Build the WHERE clause from {@link #where} and for this {@link #context}.
	 * <b>⚠ Does not prefix with the 'WHERE' keyword ! ⚠</b>
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the Where clause for {@link #where} and {@link #context}
	 */
	private SQLPart toSQLWhere(Config config) {
		return this.where.toSQL(this.context, config);
	}

	/**
	 * 2 query strategy : create the SQL 'answer' request : only select columns for the target type.
	 * <br>
	 * Joins are added to the query but none of their columns are selected.
	 * <br>
	 * Then, ids should be extracted and a second query should be used to fetch the whole data graph.
	 * <br>
	 * See {@link #executeWithTwoQueries(IConnection)} and {@link #toSQLDataRequest(Set, Config)}.
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the SQL 'answer' request.
	 */
	private SQLPart toSQLAnswerRequest(Config config) {
		JoinClause.JoinClauses joinClauses = this.toSQLJoin(true, config);
		return config.getDialect().select(
			false,
			this.toSQLColumnsClause(false, config),
			this.getTableName(),
			this.context.getPath(config),
			joinClauses.toSQL(config),
			Where.toSQL(config, this.toSQLWhere(config), joinClauses.toSQLWhere()),
			OrderBy.<T>orderById(true).toSQL(this.context.getTarget(), config)
		);
	}

	/**
	 * 2 query strategy : create the SQL 'data' request : fetch all data (including joins) for the given ids.
	 * <br>
	 * See {@link #toSQLAnswerRequest(Config)}
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the SQL 'data' request.
	 */
	private SQLPart toSQLDataRequest(Set<Comparable> ids, Config config) {
		JoinClause.JoinClauses joinClauses = this.toSQLJoin(false, config);
		String idColumn = SQLColumn.id(this.context, config).qualifiedName();
		SQLPart whereClause = Where.toSQL(
			config,
			config.getDialect().in(idColumn, ids.stream().map(String::valueOf).collect(Collectors.toList())) ,
			joinClauses.toSQLWhere()
		);

		return config.getDialect().select(
			this.lock,
			this.toSQLColumnsClause(true, config),
			this.getTableName(),
			this.context.getPath(config),
			joinClauses.toSQL(config),
			whereClause,
			this.orderBy.toSQL(this.context.getTarget(), config)
		);
	}

	/**
	 * Single query strategy with EXISTS : create the SQL 'data' request.
	 * <br>
	 * It uses a subquery to find the target type results, attached to the main query with a 'WHERE EXISTS' clause.
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the SQL 'data' request.
	 */
	private SQLPart toSQLDataRequestWithEXISTS(Config config) {
		return this.toSQLWithExists(config, false, false);
	}

	/**
	 * Create a SQL request that only returns ID columns.
	 * <br>
	 * This can be used as the subquery for an EXISTS clause or simply to count results.
	 * @param count      if true, the request will be on 'COUNT(DISTINCT id_column)' instead of the actual columns
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the SQL 'data' request.
	 */
	private SQLPart toSQLIDsRequest(boolean count, Config config) {
		return this.toSQLWithExists(config, count, true);
	}

	/**
	 * Create a 'SELECT WHERE EXISTS' query, using a subquery.
	 * <br>
	 * This relies on the dialect implementation.
	 * <br>
	 * @param config     the SQL config (sql separator, use batch inserts, dialect...)
	 * @param count      true if this query is for counting matches
	 * @param onlyIDs    true to only return ID columns. Useless if 'count' is set to 'true'
	 * @return the sqL 'SELECT WHERE EXISTS' query
	 */
	private SQLPart toSQLWithExists(Config config, boolean count, boolean onlyIDs) {
		// First we have to build a 'select ids' query for the EXISTS subquery
		// We copy the current 'Select' object to add a suffix to the context
		// We link the EXISTS subquery to the global one (id = subquery.id)
		// This is not very elegant, I must confess
		Select<T> subSelect = new Select<>(this.context.copy("_0"), this.where, this.joins);
		JoinClause.JoinClauses subSelectJoinClauses = subSelect.toSQLJoin(true, config);
		JoinClause.JoinClauses joinClauses = this.toSQLJoin(false, config);

		String idColumn = SQLColumn.id(this.context, config).qualifiedName();
		String subSelectIdColumn = SQLColumn.id(subSelect.context, config).qualifiedName();
		CharSequence columns = count
			? config.getDialect().toSQLCount(idColumn)
			: onlyIDs ? this.toSQLIdColumnsClause(config) : this.toSQLColumnsClause(true, config);

		return config.getDialect().selectWhereExists(
			this.lock,
			idColumn,
			columns,
			this.getTableName(),
			this.context.getPath(config),
			joinClauses.toSQL(config),
			joinClauses.toSQLWhere(),
			subSelectIdColumn,
			subSelect.context.getPath(config),
			subSelectJoinClauses.toSQL(config),
			Where.toSQL(config, subSelect.toSQLWhere(config), subSelectJoinClauses.toSQLWhere()),
			this.paging.toSQL(this.context, config),
			this.orderBy.toSQL(this.context.getTarget(), config)
		);
	}

	/**
	 * Single query strategy with IN : create the SQL 'data' request.
	 * <br>
	 * It uses a subquery to find IDs of the target type, inside and 'id IN' clause.
	 * @return the SQL 'data' request.
	 */
	private SQLPart toSQLDataRequestWithIN(Config config) {
		JoinClause.JoinClauses joinClauses = this.toSQLJoin(true, config);
		return config.getDialect().selectWhereIdIn(
			this.lock,
			SQLColumn.id(this.context, config).qualifiedName(),
			this.toSQLColumnsClause(true, config),
			this.getTableName(),
			this.context.getPath(config),
			joinClauses.toSQL(config),
			joinClauses.toSQLWhere(),
			this.toSQLWhere(config),
			this.paging.toSQLOrderBy(this.context, config),
			this.paging.toSQL(this.context, config),
			this.orderBy.toSQL(this.context.getTarget(), config)
		);
	}
}
