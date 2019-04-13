package org.yop.orm.query.serialize;

import org.yop.orm.query.join.IJoin;

import java.util.Collection;

/**
 * Serialization interface.
 * <ul>
 *     <li>join</li>
 *     <li>set target elements</li>
 *     <li>execute</li>
 * </ul>
 * See {@link org.yop.orm.query.serialize.json.JSON}
 * @param <Request> the request type (e.g. JSON, XML...)
 * @param <T> the request target type
 */
public interface Serialize<Request extends Serialize, T> {

	/**
	 * Add an element to be serialized
	 * @param element the element to be serialized
	 * @return the current serialize directive, for chaining purposes
	 */
	Request onto(T element);

	/**
	 * Add several elements to be serialized
	 * @param elements the elements to be serialized
	 * @return the current directive, for chaining purposes
	 */
	Request onto(Collection<T> elements);

	/**
	 * Add a relation - to another Yopable type - to be serialized.
	 * @param join the join clause
	 * @param <R> the target join type
	 * @return the current directive, for chaining purpose
	 */
	<R> Request join(IJoin<T, R> join);

	/**
	 * Add relations - to others Yopable types - to be serialized.
	 * @param joins the join clauses
	 * @return the current directive, for chaining purpose
	 */
	Request join(Collection<IJoin<T, ?>> joins);

	/**
	 * Serialize the whole data graph. Stop on transient fields and 'cycling' fields.
	 * @return the current directive, for chaining purpose
	 */
	Request joinAll();

	/**
	 * Add the joins which are targeted by profiles, using {@link org.yop.orm.annotations.JoinProfile} on fields.
	 * @return the current request, for chaining purpose
	 */
	@SuppressWarnings({"unused"})
	Request joinProfiles(String... profiles);

	/**
	 * Get the current joins of this serialize request.
	 * @return the current joins. Might be a copy of these joins.
	 */
	Collection<IJoin<T, ?>> getJoins();

	/**
	 * Execute the current serialization directive.
	 * @return the target object(s), serialized into string
	 */
	String execute();
}
