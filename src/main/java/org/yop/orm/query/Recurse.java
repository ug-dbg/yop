package org.yop.orm.query;

import org.apache.commons.collections4.CollectionUtils;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;

/**
 * Sometimes there are cycles in the data graph and you want to fetch it all.
 * <br>
 * It cannot be done using the standard YOP queries ({@link Select}, {@link Hydrate}).
 * <br>
 * This class intends to enable recursive CRUD. This is just the beginning for now !
 * <br><br>
 * <b>
 *  ⚠⚠⚠
 *  <br>
 *  YOP does not hold any session, it just hits the DB and runs away.
 *  <br>
 *  You cannot be assured that 1 DB object ↔ 1 single object in memory !
 *  <br>
 *  A Yopable can have a reference to itself (same ID) that is actually another object in memory.
 *  <br>
 *  Even though - when recursing - YOP will try to hook on the Yopable objects it knows.
 *  <br>
 *  ⚠⚠⚠
 *  </b>
 * @param <T> the target type.
 */
public class Recurse<T extends Yopable> {

	/** Target class */
	protected Class<T> target;

	/** Elements on which to recurse */
	protected Collection<T> elements = new ArrayList<>();

	/**
	 * Protected constructor, please use {@link #from(Class)}
	 * @param target the target class
	 */
	private Recurse(Class<T> target) {
		this.target = target;
	}

	/**
	 * Init recurse request.
	 * @param clazz the target class
	 * @param <Y> the target type
	 * @return an RECURSE request instance
	 */
	public static <Y extends Yopable> Recurse<Y> from(Class<Y> clazz) {
		return new Recurse<>(clazz);
	}

	/**
	 * Add an element to be recursed on.
	 * @param element the element to be recursed on
	 * @return the current RECURSE, for chaining purposes
	 */
	public Recurse<T> onto(T element) {
		this.elements.add(element);
		return this;
	}

	/**
	 * Add several elements to be recursed on.
	 * @param elements the elements to be recursed on
	 * @return the current RECURSE, for chaining purposes
	 */
	public Recurse<T> onto(Collection<T> elements) {
		this.elements.addAll(elements);
		return this;
	}

	/**
	 * Recursively fetch the given cyclic relation on the target {@link #elements}.
	 * <br>
	 * It means whenever a new object is fetched for the relation, try to fetch the relation on this new object.
	 * @param getter     the relation getter
	 * @param connection the connection to use
	 */
	public void fetch(Function<T, T> getter, IConnection connection) {
		Hydrate.from(this.target).onto(this.elements).fetch(getter).execute(connection);

		Collection<T> nexts = new HashSet<>();
		for (T element : this.elements) {
			T next = getter.apply(element);
			if (next != null && getter.apply(next) == null) {
				nexts.add(next);
			}
		}

		nexts.removeAll(this.elements);
		if (! nexts.isEmpty()) {
			Recurse.from(this.target).onto(nexts).fetch(getter, connection);
		}
	}

	/**
	 * Recursively fetch the given cyclic relation on the target {@link #elements}.
	 * <br>
	 * It means whenever new objects are fetched for the relation, try to fetch the relation on these new objects.
	 * @param getter     the relation getter
	 * @param connection the connection to use
	 */
	public void fetchSet(Function<T, Collection<T>> getter, IConnection connection) {
		Hydrate.from(this.target).onto(this.elements).fetchSet(getter).execute(connection);

		Collection<T> nexts = new HashSet<>();
		for (T element : this.elements) {
			Collection<T> next = getter.apply(element);

			for (T nextElement : next) {
				if (nextElement != null && CollectionUtils.isNotEmpty(getter.apply(nextElement))) {
					nexts.add(nextElement);
				}
			}
		}

		nexts.removeAll(this.elements);
		if (! nexts.isEmpty()) {
			Recurse.from(this.target).onto(nexts).fetchSet(getter, connection);
		}
	}
}
