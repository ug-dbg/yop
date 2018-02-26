package org.yop.orm.query;

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.yop.orm.evaluation.Comparaison;
import org.yop.orm.evaluation.Evaluation;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;
import org.yop.orm.util.MessageUtil;

import java.sql.Connection;
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
 *
 * @param <T> the type to search for.
 */
public class Select<T extends Yopable> {

	public enum STRATEGY {IN, EXISTS}

	private static final String SELECT = " SELECT {0} FROM {1} AS {2} {3} WHERE {4} ";
	private static final String SELECT_DISTINCT = " SELECT DISTINCT({0}) FROM {1} AS {2} {3} WHERE {4} ";
	private static final String DEFAULT_WHERE = " 1=1 ";

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
	 * Complete constructor.
	 * @param from  root context
	 * @param where where clause
	 * @param joins joins clauses
	 */
	private Select(Context<T> from, Where<T> where, Collection<IJoin<T, ? extends Yopable>> joins) {
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
	 * @return the current SELECT request, for chaining purpose
	 */
	public Select<T> joinAll() {
		throw new UnsupportedOperationException("Not implemented yet !");
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

		Parameters parameters = new Parameters();
		String request = this.toSQLAnswerRequest(parameters);
		Query query = new Query(request, parameters);

		Set<T> elements = Executor.executeSelectQuery(connection, query, this.context.getTarget());
		ids = elements.stream().map(Yopable::getId).distinct().collect(Collectors.toSet());

		if(ids.isEmpty()) {
			return new HashSet<>();
		}

		parameters = new Parameters();
		request = this.toSQLDataRequest(ids, parameters);
		query = new Query(request, parameters);
		return Executor.executeSelectQuery(connection, query, this.context.getTarget());
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
		Parameters parameters = new Parameters();
		String request =
			strategy == STRATEGY.IN
			? this.toSQLDataRequestWithIN(parameters)
			: this.toSQLDataRequest(parameters);

		return Executor.executeSelectQuery(connection, new Query(request, parameters), this.context.getTarget());
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
	 * Create the SQL join clause.
	 * @param evaluate true to add the where clauses to the join clauses
	 * @return the SQL join clause
	 */
	private String toSQLJoin(Parameters parameters, boolean evaluate) {
		StringBuilder join = new StringBuilder();
		this.joins.forEach(j -> join.append(j.toSQL(this.context, parameters, evaluate)));
		return join.toString();
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
		return select(
			this.toSQLColumnsClause(false),
			this.getTableName(),
			this.context.getPath(),
			this.toSQLJoin(parameters, true),
			this.toSQLWhere(parameters)
		);
	}

	/**
	 * 2 query strategy : create the SQL 'data' request : fetch all data (including joins) for the given ids.
	 * <br>
	 * See {@link #toSQLAnswerRequest(Parameters)}
	 * @return the SQL 'data' request.
	 */
	private String toSQLDataRequest(Set<Long> ids, Parameters parameters) {
		return select(
			this.toSQLColumnsClause(true),
			this.getTableName(),
			this.context.getPath(),
			this.toSQLJoin(parameters, false),
			this.idAlias() + " IN (" + Joiner.on(",").join(ids) + ") "
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

		String whereClause = MessageUtil.join(
			" AND ",
			copyForAlias.toSQLWhere(parameters),
			this.idAlias() + " = " + copyForAlias.idAlias()
		);

		String existsSubSelect = selectdistinct(
			copyForAlias.idAlias(),
			copyForAlias.getTableName(),
			copyForAlias.context.getPath(),
			copyForAlias.toSQLJoin(parameters, true),
			whereClause
		);

		// Now we can build the global query that fetches the data when the EXISTS clause matches
		return select(
			this.toSQLColumnsClause(true),
			this.getTableName(),
			this.context.getPath(),
			this.toSQLJoin(parameters, false),
			" EXISTS (" + existsSubSelect + ") "
		);
	}

	/**
	 * Single query strategy with IN : create the SQL 'data' request.
	 * @return the SQL 'data' request.
	 */
	private String toSQLDataRequestWithIN(Parameters parameters) {
		String path = this.context.getPath();
		String inSubQuery = select(
			this.idAlias(),
			this.getTableName(),
			path,
			this.toSQLJoin(parameters, true),
			this.toSQLWhere(parameters)
		);

		return select(
			this.toSQLColumnsClause(true),
			this.getTableName(),
			path,
			this.toSQLJoin(parameters, false),
			this.idAlias() + " IN (" + inSubQuery + ")"
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
	private String select(String what, String from, String as, String joinClause, String whereClause) {
		return MessageFormat.format(
			SELECT,
			what,
			from,
			as,
			joinClause,
			StringUtils.isBlank(whereClause) ? DEFAULT_WHERE : whereClause
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
	private String selectdistinct(String what, String from, String as, String joinClause, String whereClause) {
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
