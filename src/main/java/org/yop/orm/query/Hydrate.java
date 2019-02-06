package org.yop.orm.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hydration is a simple tool that uses {@link Select} to fetch relations on Yopable objects whose IDs are set.
 * <br><br>
 * Example :
 * <pre>
 * {@code Hydrate.from(Pojo.class).onto(pojoInstance).fetchSet(Pojo::getJopos).execute(connection); }
 * </pre>
 *
 * @param <T> the target type
 */
public class Hydrate<T extends Yopable> extends AbstractRequest<Hydrate<T>, T>{

	private static final Logger logger = LoggerFactory.getLogger(Hydrate.class);

	/** Elements to hydrate */
	private final Map<Long, T> elements = new HashMap<>() ;

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
		this.elements.put(element.getId(), element);
		return this;
	}

	/**
	 * Add a collection of elements to the {@link #elements} to hydrate.
	 * @param elements the elements to hydrate
	 * @return the Hydrate instance, for chaining purposes
	 */
	public Hydrate<T> onto(Collection<T> elements) {
		for (T element : elements) {
			if(element.getId() == null) {
				logger.warn("Element ID for [{}] is not set. Cannot hydrate !");
				continue;
			}
			this.elements.put(element.getId(), element);
		}
		return this;
	}

	/**
	 * Hydrate !
	 * A {@link Select} query restricted to {@link #elements} IDs is executed
	 * and every element returned is used to hydrate the {@link #elements}.
	 * @param connection the connection to use for the hydration query
	 */
	public void execute(IConnection connection) {
		if(this.elements.isEmpty()) {
			logger.warn("Hydrate on no element. Are you sure you did not forget using #onto() ?");
			return;
		}
		if(this.joins.isEmpty()) {
			logger.warn("Hydrate on no relation. Are you sure you did not forget using #fetch or #fetchSet ?");
			return;
		}

		Set<Long> ids = this.elements.values().stream().map(Yopable::getId).collect(Collectors.toSet());
		Select<T> select = Select.from(this.getTarget()).where(Where.id(ids));
		this.joins.forEach(select::join);
		Set<T> elementsFromDB = select.execute(connection);

		for (T elementFromDB : elementsFromDB) {
			T element = this.elements.get(elementFromDB.getId());
			for (IJoin<T, ? extends Yopable> join : this.joins) {
				this.assign(element, elementFromDB, join);
			}
		}
	}

	/**
	 * Assign a relation (join) using an element from DB onto the target element to hydrate.
	 * @param element the element to hydrate
	 * @param fromDB  the same element, from DB
	 * @param join    the relation to hydrate
	 */
	private void assign(T element, T fromDB, IJoin<T, ?> join) {
		Collection<? extends Yopable> relationValue = join.getTarget(fromDB);
		Field relationField = join.getField(this.getTarget());

		if (ORMUtil.isCollection(relationField)) {
			Reflection.set(relationField, element, relationValue);
		} else if (ORMUtil.isYopable(relationField)) {
			if (relationValue.size() > 1) {
				throw new YopRuntimeException(
					"Something very weird happened : "
					+ "found more than 1 target value for relation ["
					+ Reflection.fieldToString(relationField) + "] "
					+ " on element [" + element + "]"
				);
			}
			if (relationValue.isEmpty()) {
				Reflection.set(relationField, element, null);
			} else {
				Reflection.set(relationField, element, relationValue.iterator().next());
			}
		}
	}
}
