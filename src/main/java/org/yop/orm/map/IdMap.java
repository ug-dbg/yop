package org.yop.orm.map;

import com.google.common.base.Joiner;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Results;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A map of ids per class.
 * <br>
 * This object is required to find all the IDs that will have to be deleted
 * if the DBMS does not support multi-table delete.
 * <br>
 * But actually, can't it be quite usefull to get a data map of ID for a given data graph ?
 * {@code Class<? extends Yopable>} → {@code Set<Long>}
 */
public class IdMap {

	private static final String SEPARATOR = Context.SQL_SEPARATOR;

	/** The ID map. One class → a collection of IDs */
	private Map<Class<? extends Yopable>, Set<Long>> ids = new HashMap<>();

	/**
	 * Get the IDs for the given class.
	 * @param clazz the target class
	 * @return the IDs this map has for the given class
	 */
	public Collection<Long> getIdsForClass(Class<? extends Yopable> clazz) {
		return ids.getOrDefault(clazz, new HashSet<>());
	}

	/**
	 * Return an entry set of the IDs per class.
	 * @return the {@link Map#entrySet()}of {@link #ids}
	 */
	public Set<Map.Entry<Class<? extends Yopable>, Set<Long>>> entries() {
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
	private void put(Class<? extends Yopable> clazz, Long id) {
		this.ids.putIfAbsent(clazz, new HashSet<>());
		this.ids.get(clazz).add(id);
	}

	/**
	 * The executor action that can be used to map a specific SQL request resultset into a new instance of IdMap.
	 * <br>
	 * See : {@link org.yop.orm.query.Select#toSQLIDsRequest(Parameters)}.
	 * @param target the root target class (Context will be built from it)
	 * @return the Action that can be given to the {@link Executor}
	 */
	public static Executor.Action populateAction(Class<? extends Yopable> target) {
		return results -> {
			IdMap map = new IdMap();
			while (results.getResultSet().next()) {
				map(results, target, Context.root(target).getPath(), map);
			}
			return map;
		};
	}

	/**
	 * Recursively map on entry of the resultset of an SQL request into the given {@link IdMap}.
	 * <br>
	 * It should actually work with any Yop SELECT query.
	 * <br>
	 * This method is quite similar to {@link Mapper#mapRelationFields(Results, Object, String)}
	 * @param results the SQL query result
	 * @param yopable the target class
	 * @param context the current context
	 * @param map the target IdMap that will be populated with IDs
	 * @param <T> the target type
	 * @throws SQLException an error occurred reading the resultset
	 */
	private static <T extends Yopable> void map(
		Results results,
		Class<T> yopable,
		String context,
		IdMap map)
		throws SQLException {

		map.put(yopable, readId(results, yopable, context));
		List<Field> fields = Reflection.getFields(yopable, JoinTable.class, false);
		for (Field field : fields) {
			String newContext = context + SEPARATOR + field.getName() + SEPARATOR;
			if(Collection.class.isAssignableFrom(field.getType())) {
				Class<? extends Yopable> targetClass = Mapper.getRelationFieldType(field);
				newContext += targetClass.getSimpleName();

				if(Mapper.noContext(results, newContext, targetClass)) continue;
				map(results, targetClass, newContext, map);
			} else if (Yopable.class.isAssignableFrom(field.getType())){
				@SuppressWarnings("unchecked")
				Class<? extends Yopable> targetClass = (Class<? extends Yopable>) field.getType();
				newContext += targetClass.getSimpleName();

				if(Mapper.noContext(results, newContext, targetClass)) continue;
				map(results, targetClass, newContext, map);
			} else {
				throw new YopMappingException(
					" Field type [" + field.getType().getName()
					+ "] @ [" + field.getDeclaringClass().getName() + "#" + field.getName()
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
	 * @throws SQLException an error occured reading the resultset
	 */
	private static <T extends Yopable> Long readId(
		Results results,
		Class<T> target,
		String context)
		throws SQLException {

		String idColumn = ORMUtil.getIdColumn(target);
		String idColumnContext = context + SEPARATOR + idColumn;
		return results.getResultSet().getLong(results.getQuery().getShortened(idColumnContext));
	}
}