package org.yop.orm.query;

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.evaluation.Comparaison;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.map.Mapper;
import org.yop.orm.model.Yopable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
 *
 * @param <T> the type to search for.
 */
public class Select<T extends Yopable> {

	private static final Logger logger = LoggerFactory.getLogger(Select.class);

	public enum STRATEGY {IN, EXISTS}

	/** Select root context : target class and SQL path **/
	private Context<T> context;

	/** Where clauses */
	private Where<T> where;

	/** Join clauses */
	private Collection<IJoin<T, ? extends Yopable>> joins = new ArrayList<>();

	/**
	 * Private constructor. Please use {@link #from(Class)}
	 * @param from the target class (select from class)
	 */
	private Select(Class<T> from) {
		this.context = Context.root(from);
		this.where = new Where<>();
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
	public Set<T> executeWithTwoQueries(Connection connection) {
		Set<Long> ids;

		String request = this.toSQLAnswerRequest();
		try(Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = this.executeQuery(statement, request)) {
				Set<T> elements = Mapper.map(resultSet, this.context.getTarget());
				ids = elements.stream().map(Yopable::getId).distinct().collect(Collectors.toSet());
			}
		} catch (SQLException e) {
			throw new YopSQLException("Error executing query [" + request + "]", e);
		}

		if(ids.isEmpty()) {
			return new HashSet<>();
		}

		request = this.toSQLDataRequest(ids);
		try(Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = this.executeQuery(statement, request)) {
				return Mapper.map(resultSet, this.context.getTarget());
			}
		} catch (SQLException e) {
			throw new YopSQLException("Error executing query [" + request + "]", e);
		}
	}

	/**
	 * Execute the SELECT request using 1 single query and a given strategy :
	 * <ul>
	 *     <li>{@link STRATEGY#EXISTS} : use a 'WHERE EXISTS' clause </li>
	 *     <li>{@link STRATEGY#IN} : use an 'IN' clause </li>
	 * </ul>
	 * @param connection the connection to use for the request
	 * @return the SELECT result, as a set of T
	 * @throws YopSQLException An SQL error occured
	 * @throws org.yop.orm.exception.YopMapperException A ResultSet → Yopables mapping error occured
	 */
	public Set<T> execute(Connection connection, STRATEGY strategy) {
		String request = strategy == STRATEGY.IN ? this.toSQLDataRequestWithIN() : this.toSQLDataRequest();
		try(Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = this.executeQuery(statement, request)) {
				return Mapper.map(resultSet, this.context.getTarget());
			}
		} catch (SQLException e) {
			throw new YopSQLException("Error executing query [" + request + "]", e);
		}
	}

	/**
	 * Execute the given SQL query.
	 * <br>
	 * If the <b>yop.show_sql</b> system property is set, the SQL request is logged.
	 * @param statement the statement to use to execute
	 * @param sql the SQL query
	 * @return the request execution ResultSet
	 * @throws SQLException an SQL error occured.
	 */
	private ResultSet executeQuery(Statement statement, String sql) throws SQLException {
		if(StringUtils.equals("true", System.getProperty("yop.show_sql"))) {
			logger.info("Executing SELECT SQL query [{}]", sql);
		}
		return statement.executeQuery(sql);
	}

	/**
	 * Add a comparison to the where clause.
	 * @param compare the comparison
	 * @return the current SELECT request, for chaining purposes
	 */
	public Select<T> where(Comparaison compare) {
		this.where.and(compare);
		return this;
	}

	/**
	 * Add several comparisons to the where clause, with an OR operator between them.
	 * @param compare the comparisons
	 * @return the current SELECT request, for chaining purposes
	 */
	public final Select<T> or(Comparaison... compare) {
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
		return this.idAlias(this.context.getTarget().getSimpleName());
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
	 * Create the SQL join clause.
	 * @param evaluate true to add the where clauses to the join clauses
	 * @return the SQL join clause
	 */
	private String toSQLJoin(boolean evaluate) {
		StringBuilder join = new StringBuilder();
		this.joins.forEach(j -> join.append(j.toSQL(this.context, evaluate)));
		return join.toString();
	}

	private String toSQLWhere() {
		String whereClause = this.where.toSQL(this.context);
		return StringUtils.isBlank(whereClause) ? "" : (" WHERE " + whereClause);
	}

	/**
	 * 2 query strategy : create the SQL 'answer' request : only find the target type that matches. Do not fetch the joins.
	 * @return the SQL 'answer' request.
	 */
	private String toSQLAnswerRequest() {
		String path = this.context.getPath();
		String sql = "SELECT " + this.toSQLColumnsClause(false) +  " FROM " + this.getTableName() + " as " + path;
		sql += this.toSQLJoin(true);
		sql += this.toSQLWhere();
		return sql;
	}

	/**
	 * 2 query strategy : create the SQL 'data' request : fetch all data (including joins) for the given ids.
	 * <br>
	 * See {@link #toSQLAnswerRequest()}
	 * @return the SQL 'data' request.
	 */
	private String toSQLDataRequest(Set<Long> ids) {
		String path = this.context.getPath();
		String sql = "SELECT " + this.toSQLColumnsClause(true) +  " FROM " + this.getTableName() + " as " + path;
		sql += this.toSQLJoin(false);
		sql += " WHERE " + this.idAlias() + " IN (" + Joiner.on(",").join(ids) + ") ";
		return sql;
	}

	/**
	 * Single query strategy with EXISTS : create the SQL 'data' request.
	 * @return the SQL 'data' request.
	 */
	private String toSQLDataRequest() {
		String path = this.context.getPath();
		String existsSubSelect = "SELECT DISTINCT(" + this.idAlias() +  ") FROM " + this.getTableName() + " as " + path;
		existsSubSelect += this.toSQLJoin(true);
		existsSubSelect += this.toSQLWhere();
		String andOrWhere = existsSubSelect.isEmpty() ? " WHERE " : " AND ";

		String subQueryDirtyAlias = path + "_0";
		existsSubSelect = existsSubSelect.replace(path, subQueryDirtyAlias);
		existsSubSelect += andOrWhere + this.idAlias() + " = " + idAlias(subQueryDirtyAlias);

		String sql = "SELECT " + this.toSQLColumnsClause(true) +  " FROM " + this.getTableName() + " as " + path;
		sql += this.toSQLJoin(false);
		sql += " WHERE EXISTS (" + existsSubSelect + ")";
		return sql;
	}

	/**
	 * Single query strategy with IN : create the SQL 'data' request.
	 * @return the SQL 'data' request.
	 */
	private String toSQLDataRequestWithIN() {
		String path = this.context.getPath();
		String existsSubSelect = "SELECT " + this.idAlias() +  " FROM " + this.getTableName() + " as " + path;
		existsSubSelect += this.toSQLJoin(true);
		existsSubSelect += this.toSQLWhere();

		String sql = "SELECT " + this.toSQLColumnsClause(true) +  " FROM " + this.getTableName() + " as " + path;
		sql += this.toSQLJoin(false);
		sql += " WHERE " + this.idAlias() + " IN (" + existsSubSelect + ")";
		return sql;
	}
}
