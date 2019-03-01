package org.yop.orm.query.join;

import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

/**
 * Simple IJoin implementation, not SQL coupled.
 * <br>
 * This can be <ul>
 *     <li>From → (1) To relation : {@link #to(Function)}</li>
 *     <li>From → (N) To relation : {@link #toN(Function)}</li>
 * </ul>
 * @param <From> the source type
 * @param <To>   the target type
 */
public class Join<From extends Yopable, To extends Yopable> implements IJoin<From, To> {

	/** The field getter (From → field and getter → To) */
	protected Function<From, ?> getter;

	/** The relation field. Will be set from the getter. See {@link #getField(Class)}. */
	protected Field field;

	/** Sub-join clauses */
	protected final Joins<To> joins = new Joins<>();

	/** No-arg constructor. Please use copy-constructor or static methods */
	protected Join() {}

	/**
	 * Copy constructor.
	 * @param original the Join to copy for this new instance
	 */
	protected Join(Join<From, To> original) {
		this();
		this.field = original.field;
		this.getter = original.getter == null ? from -> Reflection.readField(this.field, from) : original.getter;
		this.joins.addAll(original.joins);
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
		Field field = this.getField(from.getTarget());
		return from.to(this.getTarget(field), field);
	}

	@Override
	public Joins<To> getJoins() {
		return this.joins;
	}

	@Override
	public Field getField(Class<From> from) {
		if (this.field == null) {
			this.field = Reflection.findField(from, this.getter);
		}
		return this.field;
	}

	@Override
	public Class<To> getTarget(Field field) {
		return Reflection.getTarget(field);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<To> getTarget(From from) {
		Object value = this.getter.apply(from);
		if (value == null) {
			return new ArrayList<>(0);
		}
		if (value instanceof Collection) {
			return (Collection<To>) value;
		}
		return Collections.singletonList((To) value);
	}

	/**
	 * Create a new Join clause to a collection.
	 * @param getter the getter which holds the relation
	 * @param <From> the source type
	 * @param <To>   the target type
	 * @return a new Join clause to From → (N) To
	 */
	public static <From extends Yopable, To extends Yopable> Join<From, To> toN(
		Function<From, ? extends Collection<To>> getter) {

		Join<From, To> to = new Join<>();
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
	public static <From extends Yopable, To extends Yopable> IJoin<From, To> to(Function<From, To> getter) {
		Join<From, To> to = new Join<>();
		to.getter = getter;
		return to;
	}
}
