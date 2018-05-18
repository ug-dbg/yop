package org.yop.orm.query;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.JoinColumn;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.evaluation.Evaluation;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.JoinClause;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
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

	/** [table] [table alias] on [column name] = [value] */
	private static final String JOIN = " {0} {1} on {2} = {3} ";

	/** The where clause on the target class */
	protected final Where<To> where = new Where<>();

	/** Sub-join clauses */
	private final Collection<IJoin<To, ? extends Yopable>> joins = new ArrayList<>();

	@Override
	public Collection<IJoin<To, ? extends Yopable>> getJoins() {
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
	public void toSQL(JoinClause.JoinClauses joinClauses, Context<From> parent, boolean includeWhereClause) {
		Class<From> from = parent.getTarget();
		Field field = this.getField(from);
		Context<To> to = this.to(parent, field);

		Parameters parameters = new Parameters();
		String joinClause = toSQLJoin(this.joinType(), parent, to, field);
		if(includeWhereClause) {
			Parameters whereParameters = new Parameters();
			String whereClause = this.where.toSQL(to, whereParameters);
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

		this.joins.forEach(join -> join.toSQL(joinClauses, to, includeWhereClause));
	}

	@Override
	public String joinTableAlias(Context<From> context) {
		Field field = this.getField(context.getTarget());
		return context.getPath() + Context.SQL_SEPARATOR + field.getName();
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
		return this.to(from, getField(from.getTarget()));
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
	protected JoinType joinType() {
		return JoinType.LEFT_JOIN;
	}

	/**
	 * Create the SQL join clause (for SELECT or DELETE statement).
	 * @param joins      the join clauses
	 * @param context    the current context
	 * @param evaluate   true to add {@link IJoin#where()} clauses evaluations
	 * @return the SQL join clauses
	 */
	public static <T extends Yopable> JoinClause.JoinClauses toSQLJoin(
		Collection<IJoin<T, ? extends Yopable>> joins,
		Context<T> context,
		boolean evaluate) {

		JoinClause.JoinClauses joinClauses = new JoinClause.JoinClauses();
		joins.forEach(j -> j.toSQL(joinClauses, context, evaluate));
		return joinClauses;
	}

	/**
	 * Generate 'join' clauses from a parent context, a target context and a field linking them.
	 * @param parent the parent context
	 * @param to     the target context
	 * @param field  the field from 'parent' to 'to' (should have a @JoinTable/@JoinColumn annotation)
	 * @param <From> the source type
	 * @param <To>   the target type
	 * @return the SQL join table clauses
	 * @throws YopMappingException if there is no JoinColumn/JoinTable annotation or it is incorrectly set
	 */
	private static <From extends Yopable, To extends Yopable> String toSQLJoin(
		JoinType type,
		Context<From> parent,
		Context<To> to,
		Field field) {

		Class<From> from = parent.getTarget();
		JoinTable joinTableAnnotation   = field.getAnnotation(JoinTable.class);
		JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);

		if (joinTableAnnotation != null) {
			return toSQLJoin(type, parent, to, joinTableAnnotation, field.getName());
		} else if (joinColumnAnnotation != null) {
			return toSQLJoin(type, parent, to, joinColumnAnnotation, field.getName());
		} else {
			throw new YopMappingException(
				"Field [" + from.getName() + "#" + field.getName() + "] has no JoinTable/JoinColumn annotation !"
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
	 * @param <From> the source type
	 * @param <To>   the target type
	 * @return the SQL join table clauses (1 for the target table and 1 for the join table)
	 */
	private static <From extends Yopable, To extends Yopable> String toSQLJoin(
		JoinType type,
		Context<From> parent,
		Context<To> to,
		JoinTable joinTableAnnotation,
		String relationName) {

		String joinTable = joinTableAnnotation.table();
		String joinTableSourceColumn = joinTableAnnotation.sourceColumn();
		String joinTableTargetColumn = joinTableAnnotation.targetColumn();
		String relationAlias = parent.getPath() + Context.SQL_SEPARATOR + relationName;
		String fromIdColumn = Reflection.newInstanceNoArgs(parent.getTarget()).getIdColumn();

		String targetTableAlias = to.getPath();
		String toIdColumn = Reflection.newInstanceNoArgs(to.getTarget()).getIdColumn();

		return
			toSQLJoin(
				type,
				joinTable,
				relationAlias,
				prefix(relationAlias, joinTableSourceColumn),
				prefix(parent.getPath(), fromIdColumn)
			)
			+
			toSQLJoin(
				type,
				to.getTableName(),
				targetTableAlias,
				prefix(targetTableAlias, toIdColumn),
				prefix(relationAlias, joinTableTargetColumn)
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
		String relationName) {

		String sourceTableAlias = parent.getPath();
		String targetTableAlias = to.getPath();

		if (StringUtils.isNotBlank(joinColumnAnnotation.local())) {
			String toIdColumn = Reflection.newInstanceNoArgs(to.getTarget()).getIdColumn();

			return toSQLJoin(
				type,
				to.getTableName(),
				targetTableAlias,
				prefix(sourceTableAlias, joinColumnAnnotation.local()),
				prefix(targetTableAlias, toIdColumn)
			);
		} else if (StringUtils.isNotBlank(joinColumnAnnotation.remote())) {
			String idColumn = Reflection.newInstanceNoArgs(parent.getTarget()).getIdColumn();

			return toSQLJoin(
				type,
				to.getTableName(),
				targetTableAlias,
				prefix(sourceTableAlias, idColumn),
				prefix(targetTableAlias, joinColumnAnnotation.remote())
			);
		} else {
			throw new YopMappingException(
				"Incoherent JoinColumn mapping [" + parent.getTarget().getName() + "#" + relationName + "]"
			);
		}
	}

	/**
	 * Format JOIN clause using {@link #JOIN} and the parameters
	 * @param type       the join type to use (mostly {@link JoinType#LEFT_JOIN})
	 * @param table      the table name
	 * @param tableAlias the table alias
	 * @param left       the left side of the "on" clause
	 * @param right      the right side of the "on" clause
	 * @return the formatted SQL join clause
	 */
	private static String toSQLJoin(JoinType type, String table, String tableAlias, String left, String right) {
		return type.sql + MessageFormat.format(JOIN, table, tableAlias, left, right);
	}

	/**
	 * Concatenate the prefix and the value with {@link Context#DOT}.
	 * @param prefix the prefix
	 * @param what   the element to prefix
	 * @return the prefixed value
	 */
	private static String prefix(String prefix, String what) {
		return prefix + Context.DOT + what;
	}
}
