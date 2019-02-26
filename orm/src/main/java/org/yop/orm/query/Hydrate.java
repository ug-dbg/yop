package org.yop.orm.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.map.FirstLevelCache;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.reflection.Reflection;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Hydration is a simple tool that uses {@link Select} to fetch relations on Yopable objects whose IDs are set.
 * <br><br>
 * Example :
 * <pre>
 * {@code Hydrate.from(Pojo.class).onto(pojoInstance).fetchSet(Pojo::getJopos).execute(connection); }
 * </pre>
 * <br>
 * <b>About the {@link #recurse()} method : </b>
 * <br>
 * Sometimes there are cycles in the data graph and you want to fetch it all.
 * <br>
 * It cannot be done using the standard YOP queries ({@link Select}).
 * <br>
 * This method intends to enable recursive CRUD.
 * <br>
 * Any fetched element where a join clause is a applicable will trigger a sub-select query.
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
 *  <br><br>
 * <b>
 * ⚠⚠⚠
 * <br>
 * The 'recurse' option can lead to very long executions : for every 'root' class object retrieved,
 * the join directives are recursively applied.
 * <br>
 * If you don't pay attention to your data graph,
 * you can get involved into selecting much more data than you might think.
 * <br>
 * ⚠⚠⚠
 * </b>
 * <br><br>
 * Recursion usage example :
 * <br>
 * <pre>
 * {@code
 * Hydrate
 * .from(Employee.class)
 * .onto(jane)
 * .join(Employee::getReporters)
 * .join(Employee::getReportsTo)
 * .recurse()
 * .execute(connection);
 * }
 * </pre>
 * will fetch Jane's reporters and her manager.
 * Then it will recursively do the same onto her reporters and manager.
 * @param <T> the target type
 */
public class Hydrate<T extends Yopable> extends SQLRequest<Hydrate<T>, T>{

	private static final Logger logger = LoggerFactory.getLogger(Hydrate.class);

	/** Elements to hydrate */
	private final Collection<T> elements = new ArrayList<>() ;

	/** Recurse onto any T element fetched by this hydration. */
	private boolean recurse;

	/**
	 * Private constructor. Please use {@link #from(Class)} which does the same job.
	 * @param target the target class
	 */
	private Hydrate(Class<T> target){
		super(Context.root(target));
	}

	/**
	 * Create an Hydrate instance for a given target.
	 * @param target the target class
	 * @param <T> the target type
	 * @return a new Hydrate instance for the target type
	 */
	public static <T extends Yopable> Hydrate<T> from(Class<T> target) {
		return new Hydrate<>(target);
	}

	/**
	 * Add an element to the {@link #elements} to hydrate.
	 * @param element the element to hydrate
	 * @return the Hydrate instance, for chaining purposes
	 */
	public Hydrate<T> onto(T element) {
		if(element.getId() == null) {
			logger.warn("Element ID for [{}] is not set. Cannot hydrate !");
			return this;
		}
		this.elements.add(element);
		return this;
	}

	/**
	 * Add a collection of elements to the {@link #elements} to hydrate.
	 * @param elements the elements to hydrate
	 * @return the current Hydrate request, for chaining purposes
	 */
	public Hydrate<T> onto(Collection<T> elements) {
		for (T element : elements) {
			if(element.getId() == null) {
				logger.warn("Element ID for [{}] is not set. Cannot hydrate !");
				continue;
			}
			this.elements.add(element);
		}
		return this;
	}

	/**
	 * Activate the recurse option.
	 * <br>
	 * Every T object fetched from this request will be itself hydrated.
	 * <br>
	 * <b>Please read the disclaimer in {@link Hydrate} about this method !</b>
	 * @return the current Hydrate request, for chaining purposes
	 */
	public Hydrate<T> recurse() {
		this.recurse = true;
		return this;
	}

	/**
	 * Add joins to this request.
	 * @param joins the joins to use in this request
	 * @return the current request, for chaining purposes
	 */
	private Hydrate<T> join(IJoin.Joins<T> joins) {
		joins.forEach(this::join);
		return this;
	}

	/**
	 * Hydrate !
	 * A {@link Select} query restricted to {@link #elements} IDs is executed
	 * and every element returned is used to hydrate the {@link #elements}.
	 * @param connection the connection to use for the hydration query
	 */
	public void execute(IConnection connection) {
		this.execute(connection, Select.Strategy.EXISTS);
	}

	/**
	 * Hydrate !
	 * A {@link Select} query restricted to {@link #elements} IDs is executed
	 * and every element returned is used to hydrate the {@link #elements}.
	 * @param connection the connection to use for the hydration query
	 */
	public void execute(IConnection connection, Select.Strategy strategy) {
		if(this.elements.isEmpty()) {
			logger.warn("Hydrate on no element. Are you sure you did not forget using #onto() ?");
			return;
		}
		if(this.joins.isEmpty()) {
			logger.warn("Hydrate on no relation. Are you sure you did not forget using #join() ?");
			return;
		}
		this.recurse(connection, new FirstLevelCache(connection.config()), new ArrayList<>(), strategy);
	}

	/**
	 * Recursively fetch the given cyclic relation on the target {@link #elements}.
	 * <br>
	 * It means whenever new objects are fetched for the relation, try to fetch the relation on these new objects.
	 * @param connection the connection to use
	 * @param cache      the 1st level cache to use (will be shared across the different SELECT queries)
	 * @param done       the elements we have already recursed on (stop condition)
	 */
	@SuppressWarnings("unchecked")
	private void recurse(IConnection connection, FirstLevelCache cache, Collection<T> done, Select.Strategy strategy) {
		if(this.elements.isEmpty()) {
			logger.warn("Recurse on no element. Are you sure you did not forget using #onto() ?");
			return;
		}
		this.elements.forEach(cache::put);

		// Get the data using a SELECT query on the target elements and the join clauses.
		Map<Long, T> byID = this.elements.stream().collect(Collectors.toMap(Yopable::getId, Function.identity()));

		if (byID.size() > connection.config().maxParams()) {
			logger.warn(
				"Recursing over [{}] elements. This is more than the max params [{}]. Yop should do batches here :-(",
				byID.size(),
				connection.config().maxParams()
			);
		}

		Select<T> select = Select.from(this.getTarget()).setCache(cache).where(Where.id(byID.keySet()));
		this.joins.forEach(select::join);
		Set<T> fetched = select.execute(connection, strategy);

		Collection<T> next = new HashSet<>();
		for (IJoin<T, ?> join : this.joins) {
			// Assign the data, reading the field from the join directive and the fetched data
			Field field = join.getField(this.getTarget());
			fetched.forEach(from -> Reflection.setFrom(field, from, byID.get(from.getId())));

			if (this.recurse) {
				// Walk through the fetched data using the 'join' directive and grab any target type object → 'next'
				recurseCandidates(join, this.elements, next, this.getTarget());
			}
		}

		// Do not recurse on the source elements of this RECURSE query
		next.removeAll(this.elements);

		// Do not recurse on elements we have already been recursing on (stop condition)
		next.removeAll(done);

		// Recurse !
		// If 'recurse' is false, no element had been added to 'next'. And then there is nothing to recurse on.
		if (! next.isEmpty()) {
			done.addAll(next);
			Hydrate.from(this.getTarget()).join(this.joins).onto(next).recurse().recurse(
				connection, cache, done, strategy
			);
		}
	}

	/**
	 * Walk through the sources, using the join object and find any 'target' typed object.
	 * @param join       the join path
	 * @param sources    the source objects (will not be updated here at all)
	 * @param candidates the object of type 'T' found on the path (will be populated here)
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
