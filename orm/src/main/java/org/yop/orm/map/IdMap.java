package org.yop.orm.map;

import com.google.common.base.Joiner;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Results;
import org.yop.orm.util.JoinUtil;
import org.yop.orm.util.ORMUtil;
import org.yop.reflection.Reflection;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A map of ids per class.
 * <br>
 * This object is required to find all the IDs that will have to be deleted
 * if the DBMS does not support multi-table delete.
 * <br>
 * But actually, can't it be quite useful to get a data map of ID for a given data graph ?
 * {@code Class<?>} → {@code Set<Comparable>}
 */
public class IdMap {

	/** The ID map. One class → a collection of IDs */
	private final Map<Class<?>, Set<Comparable>> ids = new HashMap<>();

	/**
	 * Get the IDs for the given class.
	 * @param clazz the target class
	 * @return the IDs this map has for the given class
	 */
	public Collection<Comparable> getIdsForClass(Class<?> clazz) {
		return this.ids.getOrDefault(clazz, new HashSet<>());
	}

	/**
	 * Return an entry set of the IDs per class.
	 * @return the {@link Map#entrySet()}of {@link #ids}
	 */
	public Set<Map.Entry<Class<?>, Set<Comparable>>> entries() {
		return this.ids.entrySet();
	}

	@Override
	public String toString() {
		return "IdMap{" + "\n"
			+ Joiner.on("\n").join(
				this.entries()
				.stream()
				.map(e -> e.getKey().getSimpleName() + "→" + e.getValue())
				.collect(Collectors.toSet())
		) + "\n" + '}';
	}

	/**
	 * Add a known ID for the given class
	 * @param clazz the target class
	 * @param id the ID to add
	 */
	private void put(Class<?> clazz, Comparable id) {
		this.ids.putIfAbsent(clazz, new HashSet<>());
		this.ids.get(clazz).add(id);
	}

	/**
	 * The executor action that can be used to map a specific SQL request resultset into a new instance of IdMap.
	 * <br>
	 * See : {@link org.yop.orm.query.sql.Select#toSQLIDsRequest(boolean, Config)}.
	 * @param target the root target class (Context will be built from it)
	 * @param config the SQL config. Needed for the sql separator to use.
	 * @return the Action that can be given to the {@link Executor}
	 */
	public static Executor.Action<IdMap> populateAction(Class<?> target, Config config) {
		return results -> {
			IdMap map = new IdMap();
			while (results.getCursor().next()) {
				map(results, target, Context.root(target).getPath(config), map);
			}
			return map;
		};
	}

	/**
	 * Recursively map on entry of the resultset of an SQL request into the given {@link IdMap}.
	 * <br>
	 * It should actually work with any Yop SELECT query.
	 * <br>
	 * This method is quite similar to {@link Mapper#mapRelationFields(Results, Object, String, FirstLevelCache)}
	 * @param results the SQL query result
	 * @param yopable the target class
	 * @param context the current context
	 * @param map the target IdMap that will be populated with IDs
	 * @param <T> the target type
	 */
	private static <T> void map(
		Results results,
		Class<T> yopable,
		String context,
		IdMap map) {

		map.put(yopable, readId(results, yopable, context));
		List<Field> fields = JoinUtil.joinedFields(yopable);
		String separator = results.getQuery().getConfig().sqlSeparator();
		for (Field field : fields) {
			String newContext = context + separator + field.getName() + separator;
			if(ORMUtil.isCollection(field)) {
				Class<?> targetClass = ORMUtil.getRelationFieldType(field);
				newContext += ORMUtil.getTargetName(targetClass);

				if(results.noContext(newContext, targetClass)) continue;
				map(results, targetClass, newContext, map);
			} else if (ORMUtil.isYopable(field)){
				@SuppressWarnings("unchecked")
				Class<?> targetClass = field.getType();
				newContext += ORMUtil.getTargetName(targetClass);

				if(results.noContext(newContext, targetClass)) continue;
				map(results, targetClass, newContext, map);
			} else {
				throw new YopMappingException(
					" Field type [" + field.getType().getName()
					+ "] @ [" + Reflection.fieldToString(field)
					+ "] is unsupported. Sorry about that :-( "
				);
			}
		}
	}

	/**
	 * Read an ID from a resultset entry, for a given context and a given target class
	 * @param results the result set
	 * @param target  the target type
	 * @param context the current context
	 * @param <T> the target type
	 * @return the ID on the current row, whose column matches [context→columnIDName]
	 * @throws org.yop.orm.exception.YopSQLException an error occurred reading the resultset
	 */
	private static <T> Comparable readId(Results results, Class<T> target, String context) {
		return (Comparable) Mapper.read(results, ORMUtil.getIdField(target), context);
	}
}
