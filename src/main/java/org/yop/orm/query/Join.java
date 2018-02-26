package org.yop.orm.query;

import org.yop.orm.model.Yopable;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * A join clause from a {@link Yopable} to another.
 * @param <From> the source type
 * @param <To>   the target type
 */
public class Join<From extends Yopable, To extends Yopable> extends AbstractJoin<From, To> {

	/** The field getter (From → field & getter → To) */
	protected Function<From, To> getter;

	private Field field;

	@Override
	public Field getField(Class<From> from) {
		if (this.field == null) {
			this.field = Reflection.findField(from, this.getter);
		}
		return this.field;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<To> getTarget(Field field) {
		return (Class<To>) field.getType();
	}

	/**
	 * Create a new Join clause, context-less
	 * @param getter the getter which holds the relation
	 * @param <From> the source type
	 * @param <To>   the target type
	 * @return a new Join clause, that can be added to a SELECT clause or as a sub-join clause
	 */
	public static <From extends Yopable, To extends Yopable> IJoin<From, To> to(Function<From, To> getter) {
		Join<From, To> to = new Join<>();
		to.getter = getter;
		return to;
	}
}
