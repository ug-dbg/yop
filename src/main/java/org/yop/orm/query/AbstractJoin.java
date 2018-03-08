package org.yop.orm.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.evaluation.Evaluation;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.JoinClause;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Common API between the different IJoin implementations.
 * @param <From> the source type
 * @param <To>   the target type
 */
abstract class AbstractJoin<From extends Yopable, To extends Yopable> implements IJoin<From, To> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractJoin.class);

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
		String joinClause = ToSQL.toSQLJoin(this.joinType(), parent, to, field);
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

	protected ToSQL.JoinType joinType() {
		return ToSQL.JoinType.LEFT_JOIN;
	}

	/**
	 * Create a Join clause when the field is known.
	 * @param field  the field to use for the join
	 * @param <From> the source type
	 * @param <To>   the target type
	 * @return a new instance of {@link AbstractJoinImpl}
	 */
	public static <From extends Yopable, To extends Yopable> IJoin<From, To> create(Field field) {
		return new AbstractJoinImpl<>(field);
	}

	/**
	 * Join all relation fields from the source class.
	 * @param source the source class, where fields will be searched
	 * @param joins  the target joins collection
	 * @param <T> the source type
	 */
	@SuppressWarnings("unchecked")
	static <T extends Yopable> void joinAll(Class<T> source, Collection<IJoin<T, ?  extends Yopable>> joins) {
		List<Field> fields = Reflection.getFields(source, JoinTable.class);
		for (Field field : fields) {
			IJoin<T, Yopable> join = AbstractJoin.create(field);
			joins.add(join);

			Class<Yopable> newTarget = join.getTarget(field);
			joinAll(newTarget, join.getJoins());
		}
	}

	/**
	 * IJoin implementation when the Field is known.
	 * <br>
	 * This can save some reflection ;-)
	 * @param <From> the source type
	 * @param <To>   the target type
	 */
	private static class AbstractJoinImpl<From extends Yopable, To extends Yopable> extends AbstractJoin<From, To> {
		private Field field;

		private AbstractJoinImpl(Field field) {
			this.field = field;
		}

		@Override
		public Field getField(Class<From> from) {
			return this.field;
		}

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "{"
				+ this.field.getDeclaringClass().getName()
				+ "#"
				+ this.field.getName()
				+ " → "
				+ this.getTarget(field).getName()
			+ "}";
		}


		@Override
		@SuppressWarnings("unchecked")
		public Class<To> getTarget(Field field) {
			if(Collection.class.isAssignableFrom(field.getType())) {
				return Reflection.getCollectionTarget(field);
			}
			return (Class<To>) field.getType();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Collection<To> getTarget(From from) {
			try {
				// Here we are asked to return a collection of objects, whatever the cardinality.
				Object target = this.field.get(from);

				// target is null → empty list
				// target is collection → target
				// target is Yopable → target, as a singleton list
				return
					target == null ? new ArrayList<>(0)  : (
						target instanceof Collection
						? (Collection<To>) target
						: Collections.singletonList((To) target)
					);
			} catch (IllegalAccessException e) {
				throw new YopRuntimeException(
					"Could not read"
					+ " field [" + this.field.getDeclaringClass() + "#" + this.field.getName() + "]"
					+ " of type [" + this.field.getType() + "]"
					+ " on [" + from + "]"
				);
			}
		}
	}
}
