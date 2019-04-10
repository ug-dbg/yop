package org.yop.orm.query.relation;

import com.google.common.base.Joiner;
import org.yop.orm.util.ORMUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility methods for the 'toString' methods of {@link Relation} implementations.
 */
class RelationsToString {

	private static final String UNDEFINED = "?";

	/**
	 * An utility method to get a hint of the 'From' class of a relations map.
	 * @return the 'From' class name or '?' if the relations map has no entry.
	 */
	static <From, To> String from(Map<From, Collection<To>> relations) {
		if (relations.isEmpty()) {
			return "?";
		}
		return relations.keySet().iterator().next().getClass().getName();
	}

	/**
	 * An utility method to get a hint of the 'To' class of a relations map.
	 * @return the 'To' class name or '?' if the relations map has no entry.
	 */
	static <From, To> String to(Map<From, Collection<To>> relations) {
		Optional<To> sample = relations
			.values()
			.stream()
			.filter(c -> !c.isEmpty())
			.findAny()
			.orElse(new ArrayList<>(0))
			.stream()
			.findAny();
		return sample.map(to -> to.getClass().getName()).orElse(UNDEFINED);
	}

	/**
	 *  An utility method to turn a relation map into a human readable String, for logging purposes mostly.
	 * @return {source1ID→[target1ID, target2ID], source2ID[target1ID, target3ID]}
	 */
	static <From, To> String toString(Map<From, Collection<To>> relations) {
		return "{" + Joiner.on(",").join(
			relations
			.entrySet()
			.stream()
			.map(RelationsToString::entryToString)
			.collect(Collectors.toList()))
		+ "}";
	}

	/**
	 *  An utility method to turn a relation map entry into a human readable String, for logging purposes mostly.
	 * @return source1ID→[target1ID, target2ID]
	 */
	private static <From, To> String entryToString(Map.Entry<From, Collection<To>> entry) {
		return ORMUtil.readId(entry.getKey())
			+ "→"
			+ entry.getValue().stream().map(ORMUtil::readId).collect(Collectors.toList());
	}

}
