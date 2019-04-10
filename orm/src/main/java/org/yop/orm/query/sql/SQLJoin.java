package org.yop.orm.query.sql;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.JoinColumn;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.evaluation.Evaluation;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.query.Context;
import org.yop.orm.query.join.IJoin;
import org.yop.orm.query.join.Join;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.JoinClause;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.ORMUtil;
import org.yop.reflection.Reflection;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

/**
 * An IJoin implementation targeted for SQL.
 * <br>
 * This is a {@link Join} with an extra {@link #where()} clause and some SQL oriented methods.
 * @param <From> the source type
 * @param <To>   the target type
 */
public class SQLJoin<From, To> extends Join<From, To> {

	private static final Logger logger = LoggerFactory.getLogger(SQLJoin.class);

	protected SQLJoin() {
		super();
	}

	/**
	 * Copy constructor. See {@link Join#Join(Join)}.
	 * @param original the join to copy for this new instance
	 */
	private SQLJoin(Join<From, To> original) {
		super(original);
	}

	public enum JoinType {
		JOIN(" join "), INNER_JOIN(" inner join "), LEFT_JOIN(" left join ");

		JoinType(String sql) {
			this.sql = sql;
		}

		private final String sql;
	}

	/** The where clause on the target class */
	protected final Where<To> where = new Where<>();

	/**
	 * Get the join clause where clause
	 * @return the current join clause where clause
	 */
	public Where<To> where() {
		return this.where;
	}

	/**
	 * Add a where clause to the current join clause.
	 * @param evaluation the comparison clause
	 * @return tje current join clause for chaining purposes
	 */
	public IJoin<From, To> where(Evaluation evaluation) {
		this.where.and(evaluation);
		return this;
	}

	/**
	 * Append the SQL join clause(s) from the current instance into the target map.
	 * @param joinClauses        the target join clauses map
	 * @param parent             the context from which the SQL clause must be built.
	 * @param includeWhereClause true to include the where clauses evaluation
	 * @param config             the SQL config (sql separator, use batch inserts...)
	 */
	@SuppressWarnings("unchecked")
	private void toSQL(
		JoinClause.JoinClauses joinClauses,
		Context<From> parent,
		boolean includeWhereClause,
		Config config) {

		Class<From> from = parent.getTarget();
		Field field = this.getField(from);
		Context<To> to = this.to(parent);

		Parameters parameters = new Parameters();
		String joinClause = toSQLJoin(this.joinType(), parent, to, field, config);
		if(includeWhereClause) {
			joinClauses.addWhereClause(config, this.where.toSQL(to, config));
		}

		if(joinClauses.containsKey(to)) {
			logger.debug("Join clause for [{}], already exists !", to);
		} else {
			joinClauses.put(to, new JoinClause(joinClause, to, parameters));
		}

		this.joins
			.stream()
			.map(j -> SQLJoin.toSQLJoin((Join<To, ?>) j))
			.forEach(join -> join.toSQL(joinClauses, to, includeWhereClause, config));
	}

	/**
	 * Return the join table alias from the given context
	 * @param context the context from which the alias is built.
	 * @param config  the SQL config (sql separator, use batch inserts...)
	 * @return the join table alias for the given context
	 */
	String joinTableAlias(Context<From> context, Config config) {
		Field field = this.getField(context.getTarget());
		return context.getPath(config) + config.sqlSeparator() + field.getName();
	}

	/**
	 * Find all the columns to select (search in current target type and sub-join clauses if required)
	 * @param context              the context (columns are deduced using {@link SQLColumn#columns(Context, Config)}.
	 * @param config               the SQL config (sql separator, use batch inserts...)
	 * @return the columns to select
	 */
	Set<SQLColumn> columns(Context<From> context, Config config) {
		Context<To> to = this.to(context);
		Set<SQLColumn> columns = SQLColumn.columns(to, config);

		for (IJoin<To, ?> join : this.getJoins()) {
			@SuppressWarnings("unchecked")
			SQLJoin<To, ?> sqlJoin = SQLJoin.toSQLJoin(join);
			columns.addAll(sqlJoin.columns(to, config));
		}
		return columns;
	}

	/**
	 * What is the join type to use for this join directive ?
	 * <br><br>
	 * For now we only use left join, for simplicity purposes.
	 * <br>
	 * We want to ensure all the data the user needs gets retrieved.
	 * <br>
	 * The select directive then uses EXISTS with a sub-select.
	 * <br>
	 * We should certainly try to use {@link JoinType#INNER_JOIN} when {@link #where} contains restrictions.
	 * @return {@link JoinType#LEFT_JOIN}
	 */
	private JoinType joinType() {
		return JoinType.LEFT_JOIN;
	}

	/**
	 * Create a new Join clause to a collection.
	 * @param getter the getter which holds the relation
	 * @param <From> the source type
	 * @param <To>   the target type
	 * @return a new Join clause to From → (N) To
	 */
	public static <From, To> SQLJoin<From, To> toN(Function<From, ? extends Collection<To>> getter) {
		SQLJoin<From, To> to = new SQLJoin<>();
		to.getter = getter;
		return to;
	}

	/**
	 * Create a new Join clause to a single other Yopable.
	 * @param getter the getter which holds the relation
	 * @param <From> the source type
	 * @param <To>   the target type
	 * @return a new Join clause to From → (1) To
	 */
	public static <From, To> SQLJoin<From, To> to(Function<From, To> getter) {
		SQLJoin<From, To> to = new SQLJoin<>();
		to.getter = getter;
		return to;
	}

	@SuppressWarnings("unchecked")
	static <From, To> SQLJoin<From, To> toSQLJoin(IJoin<From, To> join) {
		if (join == null) {
			return null;
		}
		if (join instanceof SQLJoin) {
			return (SQLJoin<From, To>) join;
		}
		if (join instanceof Join) {
			return new SQLJoin<>((Join) join);
		} else {
			throw new IllegalArgumentException("Unsupported IJoin implementation [" + join.getClass() + "]");
		}
	}

	/**
	 * Create the SQL join clause (for SELECT or DELETE statement).
	 * @param joins      the join clauses
	 * @param context    the current context
	 * @param evaluate   true to add {@link SQLJoin#where()} clauses evaluations
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the SQL join clauses
	 */
	@SuppressWarnings("unchecked")
	static <T> JoinClause.JoinClauses toSQLJoin(
		Collection<IJoin<T, ?>> joins,
		Context<T> context,
		boolean evaluate,
		Config config) {

		JoinClause.JoinClauses joinClauses = new JoinClause.JoinClauses();
		joins
			.stream()
			.map(j -> SQLJoin.toSQLJoin((Join<T, ?>) j))
			.forEach(j -> j.toSQL(joinClauses, context, evaluate, config));
		return joinClauses;
	}

	/**
	 * Generate 'join' clauses from a parent context, a target context and a field linking them.
	 * @param parent the parent context
	 * @param to     the target context
	 * @param field  the field from 'parent' to 'to' (should have a @JoinTable/@JoinColumn annotation)
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @param <From> the source type
	 * @param <To>   the target type
	 * @return the SQL join table clauses
	 * @throws YopMappingException if there is no JoinColumn/JoinTable annotation or it is incorrectly set
	 */
	private static <From, To> String toSQLJoin(
		JoinType type,
		Context<From> parent,
		Context<To> to,
		Field field,
		Config config) {

		JoinTable joinTableAnnotation   = field.getAnnotation(JoinTable.class);
		JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);

		if (joinTableAnnotation != null) {
			return toSQLJoin(type, parent, to, joinTableAnnotation, field.getName(), config);
		} else if (joinColumnAnnotation != null) {
			return toSQLJoin(type, parent, to, joinColumnAnnotation, field.getName(), config);
		} else {
			throw new YopMappingException(
				"Field [" + Reflection.fieldToString(field) + "] has no JoinTable/JoinColumn annotation !"
			);
		}
	}

	/**
	 * Generate 'join' clauses from a parent context, a target context and a field linking them
	 * <b>when the join clause uses a {@link JoinTable} annotation</b>.
	 * @param type                the join type to use (mostly {@link JoinType#LEFT_JOIN})
	 * @param parent              the parent context
	 * @param to                  the target context
	 * @param joinTableAnnotation the field join table annotation
	 * @param relationName        the relation name from 'parent' to 'to' ({@link Field#getName()})
	 * @param config              the SQL config (sql separator, use batch inserts...)
	 * @param <From> the source type
	 * @param <To>   the target type
	 * @return the SQL join table clauses (1 for the target table and 1 for the join table)
	 */
	private static <From, To> String toSQLJoin(
		JoinType type,
		Context<From> parent,
		Context<To> to,
		JoinTable joinTableAnnotation,
		String relationName,
		Config config) {

		String joinTable = ORMUtil.getJoinTableQualifiedName(joinTableAnnotation);
		String joinTableSourceColumn = joinTableAnnotation.sourceColumn();
		String joinTableTargetColumn = joinTableAnnotation.targetColumn();
		String relationAlias = parent.getPath(config) + config.sqlSeparator() + relationName;
		String fromIdColumn = ORMUtil.getIdColumn(parent.getTarget());

		String targetTableAlias = to.getPath(config);
		String toIdColumn = ORMUtil.getIdColumn(to.getTarget());

		return
			config.getDialect().join(
				type.sql,
				joinTable,
				relationAlias,
				prefix(relationAlias, joinTableSourceColumn, config),
				prefix(parent.getPath(config), fromIdColumn, config)
			)
			+
			config.getDialect().join(
				type.sql,
				to.getTableName(),
				targetTableAlias,
				prefix(targetTableAlias, toIdColumn, config),
				prefix(relationAlias, joinTableTargetColumn, config)
			);
	}

	/**
	 * Generate 'join' clause from a parent context, a target context and a field linking them
	 * <b>when the join clause uses a {@link JoinColumn} annotation</b>.
	 * @param type                 the join type to use (mostly {@link JoinType#LEFT_JOIN})
	 * @param parent               the parent context
	 * @param to                   the target context
	 * @param joinColumnAnnotation the field join column annotation
	 * @param relationName         the relation name from 'parent' to 'to' ({@link Field#getName()})
	 * @param config               the SQL config (sql separator, use batch inserts...)
	 * @param <From> the source type
	 * @param <To>   the target type
	 * @return the SQL join table clauses
	 * @throws YopMappingException if the JoinColumn is incorrectly set (no local or remote column name set)
	 */
	private static <From, To> String toSQLJoin(
		JoinType type,
		Context<From> parent,
		Context<To> to,
		JoinColumn joinColumnAnnotation,
		String relationName,
		Config config) {

		String sourceTableAlias = parent.getPath(config);
		String targetTableAlias = to.getPath(config);

		if (StringUtils.isNotBlank(joinColumnAnnotation.local())) {
			String toIdColumn = ORMUtil.getIdColumn(to.getTarget());

			return config.getDialect().join(
				type.sql,
				to.getTableName(),
				targetTableAlias,
				prefix(sourceTableAlias, joinColumnAnnotation.local(), config),
				prefix(targetTableAlias, toIdColumn, config)
			);
		} else if (StringUtils.isNotBlank(joinColumnAnnotation.remote())) {
			String idColumn = ORMUtil.getIdColumn(parent.getTarget());

			return config.getDialect().join(
				type.sql,
				to.getTableName(),
				targetTableAlias,
				prefix(sourceTableAlias, idColumn, config),
				prefix(targetTableAlias, joinColumnAnnotation.remote(), config)
			);
		} else {
			throw new YopMappingException(
				"Incoherent JoinColumn mapping [" + parent.getTarget().getName() + "#" + relationName + "]"
			);
		}
	}

	/**
	 * Concatenate the prefix and the value with {@link Config#dot()}.
	 * @param prefix the prefix
	 * @param what   the element to prefix
	 * @param config the SQL config (yes, it has a 'DOT' parameter :-D)
	 * @return the prefixed value
	 */
	private static String prefix(String prefix, String what, Config config) {
		return prefix + config.dot() + what;
	}
}
