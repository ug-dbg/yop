package org.yop.orm.query;

import org.yop.orm.model.Yopable;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.function.Function;

/**
 * A join clause from a {@link Yopable} to several other ones.
 * @param <From> the source type
 * @param <To>   the target type
 */
public class JoinSet<From extends Yopable, To extends Yopable> extends AbstractJoin<From, To> {

	/** The field getter (From → field & getter → To) */
	protected Function<From, ? extends Collection<To>> getter;

	private Field field;

	@Override
	Field getField(Class<From> from) {
		if (this.field == null) {
			this.field = Reflection.findField(from, this.getter);
		}
		return this.field;
	}

	@Override
	Class<To> getTarget(Field field) {
		return Reflection.getCollectionTarget(field);
	}

	/**
	 * Create a new Join clause
	 * @param getter the getter which holds the relation
	 * @param <From> the source type
	 * @param <To>   the target type
	 * @return a new Join clause, that can be added to a SELECT clause or as a sub-join clause
	 */
	public static <From extends Yopable, To extends Yopable> JoinSet<From, To> to(
		Function<From, ? extends Collection<To>> getter) {

		JoinSet<From, To> to = new JoinSet<>();
		to.getter = getter;
		return to;
	}
}
