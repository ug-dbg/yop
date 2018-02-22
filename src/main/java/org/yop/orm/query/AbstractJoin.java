package org.yop.orm.query;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.evaluation.Comparaison;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Parameters;
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
	public String toSQL(Context<From> parent, Parameters parameters, boolean includeWhereClause) {
		Class<From> from = parent.getTarget();
		Field field = this.getField(from);
		Context<To> to = this.to(parent, field);

		StringBuilder out = new StringBuilder();
		out.append(ToSQL.toSQLJoin(this.joinType(), parent, to, field));
		if(includeWhereClause) {
			String whereClause = this.where.toSQL(to, parameters);
			out.append(StringUtils.isNotBlank(whereClause) ? " AND " + whereClause : "");
		}

		this.joins.forEach(join -> out.append(join.toSQL(to, parameters, includeWhereClause)));
		return out.toString();
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

	protected ToSQL.JoinType joinType() {
		return ToSQL.JoinType.LEFT_JOIN;
	}

	abstract Field getField(Class<From> from);

	abstract Class<To> getTarget(Field field);
}
