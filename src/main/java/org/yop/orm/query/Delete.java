package org.yop.orm.query;

import org.yop.orm.evaluation.Evaluation;
import org.yop.orm.model.Yopable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

/**
 * Delete : delete instances of T from the database.
 *
 * @param <T> the type to delete.
 */
public class Delete<T extends Yopable> {

	private Class<T> target;
	private Where<T> where;
	private Collection<IJoin<T, ? extends Yopable>> joins = new ArrayList<>();

	private Delete(Class<T> target) {
		this.target = target;
		this.where = new Where<>();
	}

	public static <T extends Yopable> Delete<T> from(Class<T> target) {
		return new Delete<>(target);
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

	public <To extends Yopable> Delete<T> join(IJoin<T, To> join) {
		this.joins.add(join);
		return this;
	}

	private String toSQL() {
		Context<T> root = Context.root(this.target);
		Set<Context.SQLColumn> columns = root.getColumns();
		return "";
	}

	private interface IJoin<From extends Yopable, To extends Yopable> {
		IJoin<From, To> join(IJoin<To, ? extends Yopable> join);
		String toSQL(Context<From> context, Collection<String> tablesAliases);
	}

	public static class Join<From extends Yopable, To extends Yopable> implements IJoin<From, To> {
		private Function<From, To> getter;
		private Collection<Join<To, ? extends Yopable>> next = new ArrayList<>();

		public static <From extends Yopable, To extends Yopable> Join<From, To> to(Function<From, To> getter) {
			Join<From, To> join = new Join<>();
			join.getter = getter;
			return join;
		}

		@Override
		@SuppressWarnings("unchecked")
		public IJoin<From, To> join(IJoin<To, ? extends Yopable> join) {
			this.next.add((Join<To, ? extends Yopable>) join);
			return this;
		}

		@Override
		public String toSQL(Context<From> context, Collection<String> tablesAliases) {

			return null;
		}
	}

	public static class JoinSet<From extends Yopable, To extends Yopable> implements IJoin<From, To> {
		private Function<From, ? extends Collection<To>> getter;
		private Collection<Join<To, ? extends Yopable>> next = new ArrayList<>();

		public static <From extends Yopable, To extends Yopable> JoinSet<From, To> to(
			Function<From, ? extends Collection<To>> getter) {

			JoinSet<From, To> join = new JoinSet<>();
			join.getter = getter;
			return join;
		}

		@Override
		@SuppressWarnings("unchecked")
		public IJoin<From, To> join(IJoin<To, ? extends Yopable> join) {
			this.next.add((Join<To, ? extends Yopable>) join);
			return this;
		}

		@Override
		public String toSQL(Context<From> context, Collection<String> tablesAliases) {
			return null;
		}
	}
}
