package org.yop.orm.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.map.FirstLevelCache;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Constants;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.Reflection;

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
 * The API is very similar to {@link Select}.
 * <br>
 * But any fetched element where a join clause is a applicable will trigger a sub-select query.
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
 *  ⚠⚠⚠
 *  <br>
 *  The 'Recurse' API can lead to very long executions : for every 'root' class object retrieved,
 *  the join directives are recursively applied.
 *  <br>
 *  If you don't pay attention to your data graph,
 *  you can get involved into selecting much more data than you might think.
 *  <br>
 *  ⚠⚠⚠
 *  </b>
 *  <br><br>
 *  Example :
 *  <br>
 *  <pre>
 *  {@code
 *  Recurse
 *   .from(Employee.class)
 *   .onto(jane)
 *   .join(JoinSet.to(Employee::getReporters))
 *   .join(Join.to(Employee::getReportsTo))
 *   .execute(this.getConnection());
 *  }
 *  </pre>
 * @param <T> the target type.
 */
public class Recurse<T extends Yopable> {

	private static final Logger logger = LoggerFactory.getLogger(Recurse.class);

	/** Target class */
	protected final Class<T> target;

	/** Elements on which to recurse */
	protected final Collection<T> elements = new ArrayList<>();

	/** Join clauses */
	private final Collection<IJoin<T, ? extends Yopable>> joins = new ArrayList<>();

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
	 * (Left) join to a new type.
	 * @param join the join clause
	 * @param <R> the target join type
	 * @return the current SELECT request, for chaining purpose
	 */
	public <R extends Yopable> Recurse<T> join(IJoin<T, R> join) {
		this.joins.add(join);
		return this;
	}

	/**
	 * Fetch the whole data graph. Stop on transient fields.
	 * <br>
	 * <b>
	 *     ⚠⚠⚠
	 *     <br>
	 *     There must be no cycle in the data graph model !
	 *     <br>
	 *     In a recurse query, any found element where a join clause is a applicable
	 *     will trigger a sub-select query, using this join clause.
	 *     <br>
	 *     ⚠⚠⚠
	 * </b>
	 * <br><br>
	 * <b>⚠⚠⚠ Any join previously set is cleared ! Please add transient fetch clause after this ! ⚠⚠⚠</b>
	 * @return the current RECURSE request, for chaining purpose
	 */
	public Recurse<T> joinAll() {
		this.joins.clear();
		IJoin.joinAll(this.target, this.joins);
		return this;
	}

	/**
	 * Recursively the join relations on the target {@link #elements}.
	 * <br>
	 * It means whenever new objects are fetched for the relation,
	 * try to fetch the relation on these new objects, if applicable.
	 * <br><br>
	 * This method will use a shared {@link FirstLevelCache} across all the {@link Select} queries it will execute.
	 * <br>
	 * Thus, there <i>should</i> be no duplicates in memory (1 DB object ↔ 1 single object in memory).
	 * @param connection the connection to use.
	 */
	public void execute(IConnection connection) {
		this.recurse(connection, new FirstLevelCache(), new ArrayList<>());
	}

	/**
	 * Add join clauses explicitly. Useful when creating a sub-recurse.
	 * @param joins the join clause
	 * @return the current RECURSE request, for chaining purpose
	 */
	private Recurse<T> join(Collection<IJoin<T, ?>> joins) {
		this.joins.addAll(joins);
		return this;
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
	private void recurse(IConnection connection, FirstLevelCache cache, Collection<T> done) {
		if(this.elements.isEmpty()) {
			logger.warn("Recurse on no element. Are you sure you did not forget using #onto() ?");
			return;
		}
		this.elements.forEach(cache::put);

		// Get the data using a SELECT query on the target elements and the join clauses.
		Map<Long, T> byID = this.elements.stream().collect(Collectors.toMap(Yopable::getId, Function.identity()));

		if (byID.size() > Constants.MAX_PARAMS) {
			logger.warn(
				"Recursing over [{}] elements. This is more than the max params [{}]. Yop should do batches here :-(",
				byID.size(),
				Constants.MAX_PARAMS
			);
		}

		Select<T> select = Select.from(this.target).setCache(cache).where(Where.id(byID.keySet()));
		this.joins.forEach(select::join);
		Set<T> fetched = select.execute(connection, Select.Strategy.EXISTS);

		Collection<T> next = new HashSet<>();
		for (IJoin<T, ?> join : this.joins) {
			// Assign the data, reading the field from the join directive and the fetched data
			Field field = join.getField(this.target);
			fetched.forEach(from -> Reflection.setFrom(field, from, byID.get(from.getId())));

			// Walk through the fetched data using the 'join' directive and grab any target type object
			recurseCandidates(join, this.elements, next, this.target);
		}

		// Do not recurse on the source elements of this RECURSE query
		next.removeAll(this.elements);

		// Do not recurse on elements we have already been recursing on (stop condition)
		next.removeAll(done);

		// Recurse !
		if (! next.isEmpty()) {
			done.addAll(next);
			Recurse.from(this.target).join(this.joins).onto(next).recurse(connection, cache, done);
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
