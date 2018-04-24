package org.yop.orm.evaluation;

import com.google.common.base.Joiner;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.yop.orm.sql.Constants.DOT;
import static org.yop.orm.sql.Constants.SQL_SEPARATOR;

/**
 * A comparison path.
 * <br><br>
 * This should be used in a {@link Comparison} when you need to compare fields that are from different objects.
 * <br>
 * Build your path to the target attribute from the root context using successive method references
 * and then you can pass the {@link Path} to {@link Comparison#Comparison(Function, Operator, Comparable)}.
 * <br>
 * For instance :
 * <pre>
 * {@code
 * Path<Pojo, String> jopoName = Path.toSet(Pojo.class, Pojo::getJopos).andTo(Jopo::getName);
 * Set<Pojo> matches = Select
 * 	.from(Pojo.class)
 * 	.join(JoinSet.to(Pojo::getJopos))
 * 	.join(JoinSet.to(Pojo::getOthers).where(Where.compare(Other::getName, Operator.EQ, jopoName)))
 * 	.execute(connection);
 * }
 * </pre>
 * @param <From> the root of the path (context root when building the query)
 * @param <To>   the target type of the path (the type of the last field of the path)
 */
@SuppressWarnings("unchecked")
public class Path<From extends Yopable, To> implements Comparable<Path<From, To>>{

	/** All the getters to use to get from "From" to "To" */
	private List<Function<?, ?>> steps = new ArrayList<>();

	private Path() {}

	/**
	 * Initialize a path from a source and a getter.
	 * <br>
	 * The 'To' target might not be a {@link Yopable}.
	 * @param getter the getter to the 'To' type.
	 * @param <From> the root type
	 * @param <To>   the target type
	 * @return a Path object, from 'From' to to 'To' using the getter
	 */
	public static <From extends Yopable, To> Path<From, To> path(Function<From, To> getter) {
		Path<From, To> path = new Path<>();
		path.steps.add(getter);
		return path;
	}

	/**
	 * Initialize a path from a source and a getter.
	 * <br>
	 * The 'To' target <b>must</b> be a {@link Yopable}
	 * (it does not make sense to create a path to a non Yopable collection).
	 * @param getter the getter to the 'To' type.
	 * @param <From> the root type
	 * @param <To>   the target type
	 * @return a Path object, from 'From' to to 'To' using the getter
	 */
	public static <From extends Yopable, To extends Yopable> Path<From, To> pathSet(
		Function<From, Collection<To>> getter) {
		Path<From, To> path = new Path<>();
		path.steps.add(getter);
		return path;
	}

	/**
	 * Extend the current path to a new one, using a getter.
	 * <br>
	 * The 'To' target might not be a {@link Yopable}.
	 * @param getter the getter to use.
	 * @param <Next> the target type
	 * @return a new {@link Path} from the current one, using the getter.
	 */
	public <Next> Path<From , Next> to(Function<To, Next> getter) {
		Path<From, Next> next = new Path<>();
		next.steps.addAll(this.steps);
		next.steps.add(getter);
		return next;
	}

	/**
	 * Extend the current path to a new one, using a getter.
	 * <br>
	 * The 'To' target <b>must</b> be a {@link Yopable}
	 * (it does not make sense to create a path to a non Yopable collection).
	 * @param getter the getter to use.
	 * @param <Next> the target type
	 * @return a new {@link Path} from the current one, using the getter.
	 */
	public <Next extends Yopable> Path<From, Next> toSet(Function<To, Collection<Next>> getter) {
		Path<From, Next> next = new Path<>();
		next.steps.addAll(this.steps);
		next.steps.add(getter);
		return next;
	}

	/**
	 * Get the target class of a path. If collection → {@link Reflection#getCollectionTarget(Field)}.
	 * @param field the field to read
	 * @param <T> the target type of the field
	 * @return the target class if the field.
	 */
	private static <T> Class<T> getTarget(Field field) {
		if(Collection.class.isAssignableFrom(field.getType())) {
			return Reflection.getCollectionTarget(field);
		}
		return (Class<T>) field.getType();
	}

	/**
	 * Build the path, so it can be inserted into an SQL Query.
	 * <br>
	 * It looks like Pojo→relationToJopo→Jopo.attributeName
	 * @return the built path.
	 */
	public String toPath(Class<From> root) {
		Class<?> from = root;
		List<String> items = new ArrayList<>(steps.size());
		for (Function step : this.steps) {
			Field field = Reflection.findField(from, step);
			items.add(toPath(field));
			if (Collection.class.isAssignableFrom(field.getType())) {
				from = Reflection.getCollectionTarget(field);
			} else {
				from = field.getType();
			}
		}

		return ORMUtil.getTargetName(root)+ Joiner.on("").join(items);
	}

	@Override
	public int compareTo(Path<From, To> o) {
		return this.steps.size() - o.steps.size();
	}

	@Override
	public String toString() {
		return "Path {steps length [" + this.steps.size() + "]}";
	}

	/**
	 * Build the path portion for a field.
	 * <br>
	 * <ul>
	 *   <li>if target class is {@link Yopable} : →fieldName→targetClassName </li>
	 *   <li>if target class is not {@link Yopable} : .fieldName </li>
	 * </ul>
	 * @param field the considered field
	 * @return the field path portion.
	 */
	private static String toPath(Field field) {
		Class<?> target = getTarget(field);
		if (Yopable.class.isAssignableFrom(target)) {
			String out = SQL_SEPARATOR + field.getName();
			out += SQL_SEPARATOR + ORMUtil.getTargetName((Class<? extends Yopable>) target);
			return out;
		} else {
			return DOT + field.getName();
		}
	}
}
