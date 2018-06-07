package org.yop.orm.query.json;

import org.yop.orm.model.Yopable;
import org.yop.orm.query.IJoin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A wrapper class for a {@link Yopable} that should be serialized to Json using {@link JSON}.
 * <br>
 * It aggregates the {@link #subject} but also the join and joinID instructions from the subject.
 */
class YopableForJSON {

	/** The object that will be serialized. */
	private final Yopable subject;

	/** Which fields (and sub-fields) should be serialized */
	private final Collection<IJoin> joins;

	/** Which fields (and sub-fields) should be serialized */
	private final Collection<IJoin> joinIDs;

	/** join → Field cache (Finding a field from a method reference can be expensive) */
	private final Map<IJoin, Field> fieldCache;

	/**
	 * Private complete constructor. Please use {@link #create(Yopable, JSON)}.
	 * @param subject    the object that will be serialized
	 * @param joins      which fields (and sub-fields) should be serialized
	 * @param joinIDs    which fields (and sub-fields) should be serialized
	 * @param fieldCache join → Field cache (Finding a field from a method reference can be expensive)
	 */
	private YopableForJSON(
		Yopable subject,
		Collection<IJoin> joins,
		Collection<IJoin> joinIDs,
		Map<IJoin, Field> fieldCache) {

		this.subject = subject;
		this.joins = joins == null ? Collections.EMPTY_LIST : joins;
		this.joinIDs = joinIDs == null ? Collections.EMPTY_LIST : joinIDs;
		this.fieldCache = fieldCache == null ? new HashMap<>() : fieldCache;
	}

	/**
	 * Get the underlying subject (i.e. the object that will be serialized to Json)
	 * @return {@link #subject}
	 */
	Yopable getSubject() {
		return this.subject;
	}

	/**
	 * Get the join directives (i.e. the paths to follow to serialize the whole subject)
	 * @return {@link #joins}
	 */
	Collection<IJoin> getJoins() {
		return this.joins;
	}

	/**
	 * Get the join IDs directives (i.e. the paths on which related elements will be serialized into 'ID' properties)
	 * <br>
	 * See the {@link JSON} directive documentation.
	 * @return {@link #joinIDs}
	 */
	Collection<IJoin> getJoinIDs() {
		return this.joinIDs;
	}

	/**
	 * Create a new {@link YopableForJSON} wrapping a {@link Yopable}.
	 * @param on    the object to wrap
	 * @param using where to find the join/joinID paths and a field cache
	 * @return a new wrapper for the object to serialize
	 */
	@SuppressWarnings("unchecked")
	static YopableForJSON create(Yopable on, JSON using) {
		return new YopableForJSON(
			on,
			new ArrayList<IJoin>(using.joins),
			new ArrayList<IJoin>(using.joinIDs),
			using.fieldCache
		);
	}

	/**
	 * Get the "#id" property to serialize for the given join.
	 * <ul>
	 *     <li>Find the target field value for the join, on {@link #subject}</li>
	 *     <ul>
	 *         <li>if the field is a collection of Yopable → return the related objects IDs</li>
	 *         <li>if the field is a Yopable → return the related object ID, or null</li>
	 *     </ul>
	 * </ul>
	 * @param join the join directive
	 * @return what should be serialized - as "#id" property - from the {@link #subject} and for the join directive.
	 */
	@SuppressWarnings("unchecked")
	Object nextIds(IJoin join) {
		Field field = this.getField(join);
		Collection<Yopable> next = join.getTarget(this.subject);
		if (Collection.class.isAssignableFrom(field.getType())) {
			return next.stream().map(Yopable::getId).collect(Collectors.toSet());
		}
		if (next.isEmpty()) {
			return null;
		}
		return next.iterator().next().getId();
	}

	/**
	 * Get the "#id" property to serialize for the given join.
	 * <ul>
	 *     <li>Find the target field value for the join, on {@link #subject}</li>
	 *     <ul>
	 *         <li>if the field is a collection of Yopable → return the related objects IDs</li>
	 *         <li>if the field is a Yopable → return the related object ID, or null</li>
	 *     </ul>
	 * </ul>
	 * @param join        the join directive
	 * @param nextJoinIDs the "join IDs" join directive for the returned
	 * @return a new {@link YopableForJSON} or a collection of {@link YopableForJSON}
	 */
	@SuppressWarnings("unchecked")
	Object next(IJoin join, Collection<IJoin> nextJoinIDs) {
		Field field = this.getField(join);
		Collection<Yopable> next = join.getTarget(this.subject);
		if (Collection.class.isAssignableFrom(field.getType())) {
			return next
				.stream()
				.map(y -> new YopableForJSON(y, join.getJoins(), nextJoinIDs, this.fieldCache))
				.collect(Collectors.toList());
		}
		if (next.isEmpty()) {
			return null;
		}
		return new YopableForJSON(next.iterator().next(), join.getJoins(), nextJoinIDs, this.fieldCache);
	}

	/**
	 * Get the field of {@link #subject} for the given join
	 * @param join the join directive
	 * @return the field that is used by the join directive
	 */
	@SuppressWarnings("unchecked")
	Field getField(IJoin join) {
		Field field = this.fieldCache.get(join);
		field = field == null ? join.getField(this.subject.getClass()) : field;
		this.fieldCache.putIfAbsent(join, field);
		return field;
	}

}
