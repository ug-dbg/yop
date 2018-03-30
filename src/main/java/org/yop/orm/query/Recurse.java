package org.yop.orm.query;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

	private static final Logger logger = LoggerFactory.getLogger(Recurse.class);

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

	/**
	 * Recursively fetch the given cyclic relation on the target {@link #elements}.
	 * <br>
	 * It means whenever new objects are fetched for the relation, try to fetch the relation on these new objects.
	 * @param join       the relation thread to follow recursively
	 * @param connection the connection to use
	 */
	@SuppressWarnings("unchecked")
	public <To extends Yopable> void fetch(IJoin<T, To> join, IConnection connection) {
		if(this.elements.isEmpty()) {
			logger.warn("Recurse on no element. Are you sure you did not forget using #onto() ?");
			return;
		}

		// Get the data using a select on the target elements
		Map<Long, T> byID = this.elements.stream().collect(Collectors.toMap(Yopable::getId, Function.identity()));
		Set<T> fetched = Select
			.from(this.target)
			.where(Where.id(this.elements.stream().map(Yopable::getId).collect(Collectors.toList())))
			.join(join)
			.execute(connection);

		// Assign the data
		Field field = join.getField(this.target);
		fetched.forEach(t -> {
			T onto = byID.get(t.getId());
			try {
				field.set(onto, field.get(t));
			} catch (IllegalAccessException | RuntimeException e) {
				throw new YopRuntimeException(
					"Unable to set field [" + field.getDeclaringClass() + "#" + field.getName() + "]"
					+ " from [" + t + "] onto [" + onto + "]"
				);
			}
		});

		// Walk through the fetched data using the 'join' and grab any target type object
		Collection<T> next = new ArrayList<>();
		recurseCandidates(join, this.elements, next, this.target);

		// Recurse !
		next.removeAll(this.elements);
		if (! next.isEmpty()) {
			Recurse.from(this.target).onto(next).fetch(join, connection);
		}
	}

	/**
	 * Walk through the sources, using the join object and find any 'target' typed object.
	 * @param join       the join path
	 * @param sources    the source objects
	 * @param candidates the object of type 'T' found on the path
	 * @param target     the target class
	 * @param <T>        the target type
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Yopable> void recurseCandidates(
		IJoin join,
		Collection sources,
		Collection<T> candidates,
		Class<T> target) {

		Collection elements = new ArrayList();
		sources.forEach(source -> elements.addAll(join.getTarget((Yopable) source)));

		if (elements.isEmpty() || candidates.containsAll(elements)) {
			return;
		}

		if (target.isAssignableFrom(elements.iterator().next().getClass())) {
			candidates.addAll(elements);
		}

		join.getJoins().forEach(j -> recurseCandidates((IJoin) j, elements, candidates, target));
	}
}
