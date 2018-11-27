package org.yop.orm.query;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.JoinColumn;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.evaluation.Evaluation;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.JoinClause;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Common API between the different IJoin implementations.
 * @param <From> the source type
 * @param <To>   the target type
 */
abstract class AbstractJoin<From extends Yopable, To extends Yopable> implements IJoin<From, To> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractJoin.class);

	public enum JoinType {
		JOIN(" join "), INNER_JOIN(" inner join "), LEFT_JOIN(" left join ");

		JoinType(String sql) {
			this.sql = sql;
		}

		private final String sql;
	}

	/** The where clause on the target class */
	protected final Where<To> where = new Where<>();

	/** Sub-join clauses */
	private final Joins<To> joins = new Joins<>();

	@Override
	public Joins<To> getJoins() {
		return this.joins;
	}

	@Override
	public Where<To> where() {
		return this.where;
	}

	@Override
	public IJoin<From, To> where(Evaluation evaluation) {
		this.where.and(evaluation);
		return this;
	}

	@Override
	public void toSQL(
		JoinClause.JoinClauses joinClauses,
		Context<From> parent,
		boolean includeWhereClause,
		Config config) {

		Class<From> from = parent.getTarget();
		Field field = this.getField(from);
		Context<To> to = this.to(parent, field);

		Parameters parameters = new Parameters();
		String joinClause = toSQLJoin(this.joinType(), parent, to, field, config);
		if(includeWhereClause) {
			Parameters whereParameters = new Parameters();
			String whereClause = this.where.toSQL(to, whereParameters, config);
			joinClauses.addWhereClause(whereClause, whereParameters);
		}

		if(joinClauses.containsKey(to)) {
			logger.debug("Join clause for [{}], already exists ! Trying to choose the most strict.", to);
			if(joinClauses.get(to).getParameters().size() < parameters.size()) {
				joinClauses.put(to, new JoinClause(joinClause, to, parameters));
			}
		} else {
			joinClauses.put(to, new JoinClause(joinClause, to, parameters));
		}

		this.joins.forEach(join -> join.toSQL(joinClauses, to, includeWhereClause, config));
	}

	@Override
	public String joinTableAlias(Context<From> context, Config config) {
		Field field = this.getField(context.getTarget());
		return context.getPath(config) + config.sqlSeparator() + field.getName();
	}

	@Override
	public <Next extends Yopable> IJoin<From, To> join(IJoin<To, Next> join) {
		this.joins.add(join);
		return this;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{From → To}";
	}

	@Override
	public Context<To> to(Context<From> from) {
		return this.to(from, this.getField(from.getTarget()));
	}

	protected Context<To> to(Context<From> from, Field field) {
		return from.to(this.getTarget(field), field);
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
	 * Create the SQL join clause (for SELECT or DELETE statement).
	 * @param joins      the join clauses
	 * @param context    the current context
	 * @param evaluate   true to add {@link IJoin#where()} clauses evaluations
	 * @param config     the SQL config (sql separator, use batch inserts...)
	 * @return the SQL join clauses
	 */
	static <T extends Yopable> JoinClause.JoinClauses toSQLJoin(
		Collection<IJoin<T, ? extends Yopable>> joins,
		Context<T> context,
		boolean evaluate,
		Config config) {

		JoinClause.JoinClauses joinClauses = new JoinClause.JoinClauses();
		joins.forEach(j -> j.toSQL(joinClauses, context, evaluate, config));
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
	private static <From extends Yopable, To extends Yopable> String toSQLJoin(
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
	private static <From extends Yopable, To extends Yopable> String toSQLJoin(
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
		String fromIdColumn = Reflection.newInstanceNoArgs(parent.getTarget()).getIdColumn();

		String targetTableAlias = to.getPath(config);
		String toIdColumn = Reflection.newInstanceNoArgs(to.getTarget()).getIdColumn();

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
	private static <From extends Yopable, To extends Yopable> String toSQLJoin(
		JoinType type,
		Context<From> parent,
		Context<To> to,
		JoinColumn joinColumnAnnotation,
		String relationName,
		Config config) {

		String sourceTableAlias = parent.getPath(config);
		String targetTableAlias = to.getPath(config);

		if (StringUtils.isNotBlank(joinColumnAnnotation.local())) {
			String toIdColumn = Reflection.newInstanceNoArgs(to.getTarget()).getIdColumn();

			return config.getDialect().join(
				type.sql,
				to.getTableName(),
				targetTableAlias,
				prefix(sourceTableAlias, joinColumnAnnotation.local(), config),
				prefix(targetTableAlias, toIdColumn, config)
			);
		} else if (StringUtils.isNotBlank(joinColumnAnnotation.remote())) {
			String idColumn = Reflection.newInstanceNoArgs(parent.getTarget()).getIdColumn();

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
