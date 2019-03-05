package org.yop.orm.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.JoinColumn;
import org.yop.orm.annotations.JoinProfile;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.exception.YopJoinCycleException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.join.IJoin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A collection of utility methods to deal with joins !
 * <br>
 * This was originally into {@link ORMUtil} or {@link IJoin}.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class JoinUtil {

	private static final Logger logger = LoggerFactory.getLogger(JoinUtil.class);

	/**
	 * Get all the fields with either a {@link JoinTable} or {@link JoinColumn} annotation - be they transient or not.
	 * @param clazz the clazz to inspect for @JoinTable/@JoinColumn fields
	 * @return the matching fields
	 */
	public static List<Field> joinedFields(Class clazz) {
		return ListUtils.union(joinTableFields(clazz), joinColumnFields(clazz));
	}

	/**
	 * Get all the fields with either a {@link JoinTable} or {@link JoinColumn} annotation that are not transient.
	 * @param clazz the clazz to inspect for @JoinTable/@JoinColumn fields
	 * @return the matching fields
	 */
	public static List<Field> nonTransientJoinedFields(Class clazz) {
		return ListUtils.union(nonTransientJoinTableFields(clazz), nonTransientJoinColumnFields(clazz));
	}

	/**
	 * Get all the fields with a {@link JoinTable} annotation - be they transient or not.
	 * @param clazz the clazz to inspect for @JoinTable fields
	 * @return the matching fields
	 */
	public static List<Field> joinTableFields(Class clazz) {
		return ORMUtil.getFields(clazz, JoinTable.class, false);
	}

	/**
	 * Get all the fields with a {@link JoinTable} annotation that are not transient.
	 * @param clazz the clazz to inspect for @JoinTable fields
	 * @return the matching fields
	 */
	public static List<Field> nonTransientJoinTableFields(Class clazz) {
		return ORMUtil.getFields(clazz, JoinTable.class, true);
	}

	/**
	 * Get all the fields with a {@link JoinColumn} annotation - be they transient or not.
	 * @param clazz the clazz to inspect for @JoinColumn fields
	 * @return the matching fields
	 */
	public static List<Field> joinColumnFields(Class clazz) {
		return ORMUtil.getFields(clazz, JoinColumn.class, false);
	}

	/**
	 * Get all the fields with a {@link JoinColumn} annotation which target a Yopable - be they transient or not.
	 * @param clazz the clazz to inspect for @JoinColumn fields
	 * @return the matching fields
	 */
	public static List<Field> joinColumnYopableFields(Class clazz) {
		return ORMUtil.getFields(clazz, JoinColumn.class, false)
			.stream()
			.filter(f -> Yopable.class.isAssignableFrom(f.getType()))
			.collect(Collectors.toList());
	}

	/**
	 * Get all the fields with a {@link JoinColumn} annotation that are not transient.
	 * @param clazz the clazz to inspect for @JoinColumn fields
	 * @return the matching fields
	 */
	public static List<Field> nonTransientJoinColumnFields(Class clazz) {
		return ORMUtil.getFields(clazz, JoinColumn.class, true);
	}

	/**
	 * Read all the {@link JoinProfile} profiles annotated on this field.
	 * @param field the field to read (can be null)
	 * @return the join profiles annotated for the field
	 */
	public static List<String> joinProfiles(Field field) {
		if (field != null && field.isAnnotationPresent(JoinProfile.class)) {
			return Arrays.asList(field.getAnnotation(JoinProfile.class).profiles());
		}
		return new ArrayList<>(0);
	}

	/**
	 * Join all relation fields from the source class. Stop on cycled relation field.
	 * @param source   the source class, where fields will be searched
	 * @param joins    the target joins collection
	 * @param profiles the profiles to join. If empty, all non transient relations will be used.
	 * @param <T> the source type
	 */
	public static <T extends Yopable> void joinProfiles(
		Class<T> source,
		Collection<IJoin<T, ?  extends Yopable>> joins,
		String... profiles) {
		joinAll(source, joins, new HashSet<>(), profiles);
	}

	/**
	 * Join all relation fields from the source class. Stop on transient relations and cycled relation field.
	 * @param source the source class, where fields will be searched
	 * @param joins  the target joins collection
	 * @param <T> the source type
	 */
	public static <T extends Yopable> void joinAll(Class<T> source, Collection<IJoin<T, ?  extends Yopable>> joins) {
		joinAll(source, joins, new HashSet<>());
	}

	/**
	 * Join all relation fields from the source class.
	 * @param source       the source class, where fields will be searched
	 * @param joins        the target joins collection
	 * @param cycleBreaker a set of fields that already were processed. Is used to stop on cycles.
	 * @param profiles     the profiles to join. If empty, all non transient relations will be used.
	 * @param <T> the source type
	 */
	private static <T extends Yopable> void joinAll(
		Class<T> source,
		Collection<IJoin<T, ? extends Yopable>> joins,
		Set<Field> cycleBreaker,
		String... profiles) {

		List<Field> fields = joinedFields(source);

		for (Field field : fields) {
			if (profiles.length == 0) {
				if (! ORMUtil.isNotTransient(field)) {
					continue;
				}
			} else if (! CollectionUtils.containsAny(joinProfiles(field), profiles)) {
				logger.debug("Field [{}] is not marked for any profile of {}", field, profiles);
				continue;
			}

			if (cycleBreaker.contains(field)) {
				throw new YopJoinCycleException(field);
			}
			cycleBreaker.add(field);

			IJoin<T, Yopable> join = IJoin.onField(field);
			joins.add(join);

			Class<Yopable> newTarget = join.getTarget(field);

			try {
				joinAll(newTarget, join.getJoins(), cycleBreaker, profiles);
			} catch (YopJoinCycleException e) {
				e.addProcessedJoins(joins);
				logger.debug("Cycle detected in joins directive", e);
			}
		}
	}

	/**
	 * Print a join relation as an ordered collection of lines into a target list.
	 * This method recursively walks through the join using {@link IJoin#getJoins()}.
	 * @param prefix the line prefix (should depend on what was printed before)
	 * @param from   the source class for the join clause
	 * @param join   the join clause
	 * @param isTail true if this join clause has no sibling ('└───' will be printed instead of '├───')
	 * @param into   the target list of lines
	 * @param <From> the source type for the join clause
	 */
	@SuppressWarnings("unchecked")
	public static <From extends Yopable> void printJoin(
		String prefix,
		Class<From> from,
		IJoin<From, ?> join,
		boolean isTail,
		List<String> into) {

		List<IJoin> children = new ArrayList<>(join.getJoins());
		Field field = join.getField(from);
		Class to = join.getTarget(field);
		String name = field.getName() + "→" + to.getName();
		if (field.isAnnotationPresent(JoinProfile.class)) {
			name += " - profiles: " + Arrays.toString(field.getAnnotation(JoinProfile.class).profiles());
		}

		JoinLine line = ORMUtil.isNotTransient(field) ? JoinLine.DEFAULT: JoinLine.TRANSIENT;
		into.add(prefix + (isTail ? line.tail : line.notTail) + name);

		for (int i = 0; i < children.size() - 1; i++) {
			IJoin child = children.get(i);
			printJoin(prefix + line.verticalLine(isTail), to, child, false, into);
		}

		if (children.size() > 0) {
			IJoin child = children.get(children.size() - 1);
			printJoin(prefix + line.verticalLine(isTail), to, child, true, into);
		}
	}

	/**
	 * An enum for the 'print' representation of joins.
	 * <br>
	 * The 'print' representation of joins is similar to the linux 'tree' command.
	 * <br>
	 * We just want to print weaker lines for relations that are 'transient' or 'YopTransient'.
	 */
	private enum JoinLine {
		TRANSIENT("└┄┄┄", "├┄┄┄"), DEFAULT("└───", "├───");

		private String tail;
		private String notTail;

		JoinLine(String tail, String notTail) {
			this.tail = tail;
			this.notTail = notTail;
		}

		String verticalLine(boolean isTail) {
			return isTail ? "    " : "│   ";
		}
	}

}
