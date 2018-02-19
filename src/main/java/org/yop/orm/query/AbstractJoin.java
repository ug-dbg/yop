package org.yop.orm.query;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.model.Yopable;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.evaluation.Comparaison;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Common API between the different IJoin implementations.
 * @param <From> the source type
 * @param <To>   the target type
 */
abstract class AbstractJoin<From extends Yopable, To extends Yopable> implements IJoin<From, To> {

	/** The where clause on the target class */
	protected Where<To> where = new Where<>();

	/** Sub-join clauses */
	private Collection<IJoin<To, ? extends Yopable>> joins = new ArrayList<>();

	@Override
	public Collection<IJoin<To, ? extends Yopable>> getJoins() {
		return this.joins;
	}

	@Override
	public Where<To> where() {
		return this.where;
	}

	@Override
	public IJoin<From, To> where(Comparaison evaluation) {
		this.where.and(evaluation);
		return this;
	}

	@Override
	public String toSQL(Context<From> parent, boolean includeWhereClause) {
		Class<From> from = parent.getTarget();
		Field field = this.getField(from);
		JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);
		if(joinTableAnnotation == null) {
			throw new YopMappingException(
				"Field [" + from.getName() + "#" + field.getName() + "] has no JoinTable annotation !"
			);
		}

		String joinTable = joinTableAnnotation.table();
		String joinTableSourceColumn = joinTableAnnotation.sourceColumn();
		String joinTableTargetColumn = joinTableAnnotation.targetColumn();
		String relationAlias = parent.getPath() + Context.SQL_SEPARATOR + field.getName();
		String fromIdColumn = Reflection.newInstanceNoArgs(parent.getTarget()).getIdColumn();
		
		Context<To> to = this.to(parent, field);
		String targetTableAlias = to.getPath();
		String toIdColumn = Reflection.newInstanceNoArgs(to.getTarget()).getIdColumn();

		StringBuilder out = new StringBuilder();
		out.append(toSQLJoinTable(
			joinTable, relationAlias, joinTableSourceColumn, parent.getPath() + Context.DOT + fromIdColumn
		));
		out.append(toSQLTargetTable(
			to.getTableName(), targetTableAlias, toIdColumn, relationAlias + Context.DOT + joinTableTargetColumn
		));

		String whereClause = this.where.toSQL(to);
		if(includeWhereClause) {
			out.append(StringUtils.isNotBlank(whereClause) ? " AND " + whereClause : "");
		}

		this.joins.forEach(join -> out.append(join.toSQL(to, includeWhereClause)));

		return out.toString();
	}

	@Override
	public <Next extends Yopable> IJoin<From, To> join(IJoin<To, Next> join) {
		this.joins.add(join);
		return this;
	}

	@Override
	public String toString() {
		Field where = Reflection.get(this.getClass(), "where");
		return this.getClass().getSimpleName()
			+ " { To → " +
				(where == null ? "N/A" : Reflection.get1ArgParameter(where))
			+ "}";
	}

	@Override
	public Context<To> to(Context<From> from) {
		return this.to(from, getField(from.getTarget()));
	}

	protected Context<To> to(Context<From> from, Field field) {
		return from.to(this.getTarget(field), field);
	}

	private String toSQLJoinTable(String table, String alias, String column, String referencedIDColumnAlias) {
		String sql = " left join " + table + " as " + alias;
		sql += " on " + referencedIDColumnAlias + " = " + alias + Context.DOT + column;
		return sql;
	}

	private String toSQLTargetTable(String table, String alias, String column, String referencedJoinColumnAlias) {
		String sql = " join " + table + " as " + alias;
		sql += " on " + referencedJoinColumnAlias + " = " + alias + Context.DOT + column;
		return sql;
	}

	abstract Field getField(Class<From> from);

	abstract Class<To> getTarget(Field field);
}
