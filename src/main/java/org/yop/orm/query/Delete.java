package org.yop.orm.query;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.yop.orm.evaluation.Evaluation;
import org.yop.orm.exception.YopSerializableQueryException;
import org.yop.orm.map.IdMap;
import org.yop.orm.model.JsonAble;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.*;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.Reflection;

import java.text.MessageFormat;
import java.util.*;
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
 * <br><br>
 * Example :
 * <pre>
 * {@code
 * Delete
 *  .from(Genre.class)
 *  .where(Where.compare(Genre::getName, Operator.LIKE, "%Vegetarian progressive grindcore%"))
 *  .join(JoinSet.to(Genre::getTracksOfGenre))
 *  .executeQueries(this.getConnection());
 * }
 * </pre>
 *
 * @param <T> the type to delete.
 */
public class Delete<T extends Yopable> implements JsonAble {

	private static final String DELETE = " DELETE {0} FROM {1} {2} WHERE {3} ";
	private static final String DEFAULT_WHERE = " 1=1 ";

	private final Class<T> target;
	private Where<T> where;
	private final IJoin.Joins<T> joins = new IJoin.Joins<>();

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
	 * Serialize the current Delete query into a Gson JSON object.
	 * @return the JSON representation of the query
	 */
	public JsonObject toJSON() {
		return this.toJSON(Context.root(this.target));
	}

	@Override
	public <U extends Yopable> JsonObject toJSON(Context<U> context) {
		JsonObject out = (JsonObject) JsonAble.super.toJSON(context);
		out.addProperty("target", this.target.getCanonicalName());
		return out;
	}

	/**
	 * Create a Delete query from the given json String representation.
	 * @param json         the Delete query JSON representation
	 * @param config       the SQL config (sql separator, use batch inserts...)
	 * @param classLoaders the class loaders to use to try to load the target resource
	 * @param <T> the target context type. This should match the one set in the JSON representation of the query !
	 * @return a new Delete query whose state is set from its JSON representation
	 */
	public static <T extends Yopable> Delete<T> fromJSON(String json, Config config, ClassLoader... classLoaders) {
		try {
			JsonParser parser = new JsonParser();
			JsonObject selectJSON = (JsonObject) parser.parse(json);
			String targetClassName = selectJSON.getAsJsonPrimitive("target").getAsString();
			Class<T> target = Reflection.forName(targetClassName, classLoaders);
			Delete<T> delete = Delete.from(target);
			delete.fromJSON(Context.root(delete.target), selectJSON, config);
			return delete;
		} catch (RuntimeException e) {
			throw new YopSerializableQueryException(
				"Could not create query from JSON [" + org.apache.commons.lang.StringUtils.abbreviate(json, 30) + "]", e
			);
		}
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
	 * What is the target Yopable of this Delete query ?
	 * @return {@link #target}
	 */
	public Class<T> getTarget() {
		return this.target;
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
		IJoin.joinAll(this.target, this.joins);
		return this;
	}

	/**
	 * <b>⚠⚠⚠ This method requires the DBMS to support delete from multiple tables ⚠⚠⚠</b>
	 * <br>
	 * <b>⚠⚠⚠ This method actually DOES NOT SEEM TO WORK AT ALL ! ⚠⚠⚠</b>
	 * <br>
	 * Execute the DELETE query on the given connection.
	 * @param connection the connection to use
	 */
	public void executeQuery(IConnection connection) {
		Parameters parameters = new Parameters();
		Executor.executeQuery(
			connection,
			new SimpleQuery(
				this.toSQL(parameters, connection.config()),
				Query.Type.DELETE,
				parameters,
				connection.config()
			)
		);
	}

	/**
	 * Execute the DELETE queries on the given connection, 1 query per table.
	 * <br>
	 * An {@link IdMap} is built using {@link Select#executeForIds(IConnection)}
	 * and then a delete query is applied for every table with IDs to delete.
	 * @param connection the connection to use
	 */
	public void executeQueries(IConnection connection) {
		Select<T> select = this.toSelect();
		IdMap idMap = select.executeForIds(connection);
		List<Query> queries = new ArrayList<>();

		for (Map.Entry<Class<? extends Yopable>, Set<Long>> entry : idMap.entries()) {

			// Create some 'delete by ID' batches, due to some DBMS limitations.
			List<List<Long>> batches = Lists.partition(
				new ArrayList<>(entry.getValue()),
				connection.config().maxParams()
			);
			for (List<Long> batch : batches) {
				Parameters parameters = new Parameters();
				String sql = Delete.from(entry.getKey()).where(Where.id(batch)).toSQL(parameters, connection.config());
				queries.add(new SimpleQuery(sql, Query.Type.DELETE, parameters, connection.config()));
			}
		}

		for (Query query : queries) {
			Executor.executeQuery(connection, query);
		}
	}

	/**
	 * Generate the SQL DELETE query
	 * @param parameters the SQL parameters that will be populated with actual query parameters
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the SQL DELETE query string
	 */
	private String toSQL(Parameters parameters, Config config) {
		Context<T> root = Context.root(this.target);
		Set<Context.SQLColumn> columns = this.columns(config);

		Set<String> tables = columns
			.stream()
			.map(c -> StringUtils.substringBeforeLast(c.getQualifiedId(), "."))
			.collect(Collectors.toSet());

		this.joins.forEach(j -> joinTables(j, root, tables, config));

		// 1 single table : omit the 'columns' clause, the 'as' clause and use a Fake context for the where clause
		// (This was to deal with SQLite. I am not very proud of this.)
		String columnsClause = "";
		String asClause = "";
		Context<T> context = new Context.FakeContext<>(root, root.getTableName());
		if (tables.size() > 1) {
			columnsClause = Joiner.on(", ").join(tables.stream().map(t -> t + ".*").collect(Collectors.toSet()));
			asClause = " as " + root.getPath(config);
			context = root;
		}

		String whereClause = this.where.toSQL(context, parameters, config);
		JoinClause.JoinClauses joinClauses = this.toSQLJoin(config);
		return MessageFormat.format(
			DELETE,
			columnsClause,
			root.getTableName() + asClause,
			joinClauses.toSQL(parameters),
			Where.toSQL(DEFAULT_WHERE, whereClause, joinClauses.toSQLWhere(parameters))
		);
	}

	/**
	 * Find all the columns from the DELETE query (search in current target type and join clauses)
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return all the columns this query should delete
	 */
	private Set<Context.SQLColumn> columns(Config config) {
		Context<T> root = Context.root(this.target);
		Set<Context.SQLColumn> columns = root.getColumns(config);

		for (IJoin<T, ? extends Yopable> join : this.joins) {
			columns.addAll(join.columns(root, true, config));
		}
		return columns;
	}

	/**
	 * Create the SQL join clause for the DELETE statement.
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the SQL join clause
	 */
	private JoinClause.JoinClauses toSQLJoin(Config config) {
		return AbstractJoin.toSQLJoin(this.joins, Context.root(this.target), false, config);
	}

	/**
	 * Recursively read all the join tables this query will impact.
	 * @param join    the join clause to read
	 * @param context the current context
	 * @param tables  the found join tables. Init with an empty set.
	 * @param config  the SQL config (sql separator, use batch inserts...)
	 */
	@SuppressWarnings("unchecked")
	private static void joinTables(IJoin join, Context context, Set<String> tables, Config config) {
		tables.add(join.joinTableAlias(context, config));
		for (Object subJoin : join.getJoins()) {
			joinTables(((IJoin)subJoin), join.to(context), tables, config);
		}
	}
}
