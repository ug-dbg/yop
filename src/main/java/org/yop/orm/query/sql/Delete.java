package org.yop.orm.query.sql;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.yop.orm.exception.YopSerializableQueryException;
import org.yop.orm.map.IdMap;
import org.yop.orm.model.JsonAble;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.query.join.IJoin;
import org.yop.orm.sql.*;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.Reflection;

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
 *  .join(SQLJoin.toN(Genre::getTracksOfGenre))
 *  .executeQueries(this.getConnection());
 * }
 * </pre>
 *
 * @param <T> the type to delete.
 */
public class Delete<T extends Yopable> extends WhereRequest<Delete<T>, T> implements JsonAble {

	private Delete(Class<T> target) {
		super(Context.root(target));
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
		return this.toJSON(this.context);
	}

	@Override
	public <U extends Yopable> JsonObject toJSON(Context<U> context) {
		JsonObject out = (JsonObject) JsonAble.super.toJSON(context);
		out.addProperty("target", this.getTarget().getCanonicalName());
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
			delete.fromJSON(delete.context, selectJSON, config);
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
	@SuppressWarnings("WeakerAccess")
	public Select<T> toSelect() {
		return new Select<>(this.context, this.where, this.joins);
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
		Executor.executeQuery(
			connection,
			new SimpleQuery(
				this.toSQL(connection.config()),
				Query.Type.DELETE,
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

		for (Map.Entry<Class<? extends Yopable>, Set<Comparable>> entry : idMap.entries()) {

			// Create some 'delete by ID' batches, due to some DBMS limitations.
			List<List<Comparable>> batches = Lists.partition(
				new ArrayList<>(entry.getValue()),
				connection.config().maxParams()
			);
			for (List<Comparable> batch : batches) {
				SQLPart sql = Delete.from(entry.getKey()).where(Where.id(batch)).toSQL(connection.config());
				queries.add(new SimpleQuery(sql, Query.Type.DELETE, connection.config()));
			}
		}

		for (Query query : queries) {
			Executor.executeQuery(connection, query);
		}
	}

	/**
	 * Generate the SQL DELETE query
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the SQL DELETE query string
	 */
	private SQLPart toSQL(Config config) {
		Context<T> root = this.context;
		Set<Context.SQLColumn> columns = this.columns(true, config);

		Set<String> tables = columns
			.stream()
			.map(c -> StringUtils.substringBeforeLast(c.getQualifiedId(), "."))
			.collect(Collectors.toSet());

		this.joins.forEach(j -> joinTables(j, root, tables, config));

		// 1 single table : omit the 'columns' clause, the 'as' clause and use a Fake context for the where clause
		// (This was to deal with SQLite. I am not very proud of this.)
		String columnsClause = "";
		String asClause = "";
		Context<T> context = new FakeContext<>(root, root.getTableName());
		if (tables.size() > 1) {
			columnsClause = Joiner.on(", ").join(tables.stream().map(t -> t + ".*").collect(Collectors.toSet()));
			asClause = " as " + root.getPath(config);
			context = root;
		}

		SQLPart whereClause = this.where.toSQL(context, config);
		JoinClause.JoinClauses joinClauses = this.toSQLJoin(false, config);
		return config.getDialect().delete(
			columnsClause,
			root.getTableName() + asClause,
			joinClauses.toSQL(config),
			Where.toSQL(config, whereClause, joinClauses.toSQLWhere())
		);
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
		tables.add(SQLJoin.toSQLJoin(join).joinTableAlias(context, config));
		for (Object subJoin : join.getJoins()) {
			joinTables(((IJoin) subJoin), join.to(context), tables, config);
		}
	}
}
