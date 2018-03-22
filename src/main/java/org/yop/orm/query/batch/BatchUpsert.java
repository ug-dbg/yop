package org.yop.orm.query.batch;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.evaluation.NaturalKey;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.IJoin;
import org.yop.orm.query.Relation;
import org.yop.orm.query.Select;
import org.yop.orm.query.Upsert;
import org.yop.orm.sql.*;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.ORMUtil;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.yop.orm.sql.Parameters.Parameter;

/**
 * Batch Upsert : save or update instances of T to the database.
 * <br>
 * Use batch mechanism (see {@link org.yop.orm.sql.BatchQuery}) when possible.
 * <br>
 * Batches are very implementation sensitive. Some drivers does not seem to play well with this.
 * <br>
 * For now, this is not very efficient. There is plenty of room for optimization :-)
 *
 * @param <T> the type to upsert.
 */
public class BatchUpsert<T extends Yopable> extends Upsert<T> {

	private static final Logger logger = LoggerFactory.getLogger(BatchUpsert.class);

	/**
	 * Private constructor, please use {@link #from(Class)}
	 * @param target the target class
	 */
	private BatchUpsert(Class<T> target) {
		super(target);
	}

	/**
	 * Init upsert request.
	 * @param clazz the target class
	 * @param <Y> the target type
	 * @return an UPSERT request instance
	 */
	public static <Y extends Yopable> Upsert<Y> from(Class<Y> clazz) {
		return new BatchUpsert<>(clazz);
	}

	/**
	 * Execute the upsert request, using batches when possible.
	 * <br>
	 * <br>
	 * <b>How is it supposed to work ?</b>
	 * <br>
	 * The idea here is to create a sub-upsert request for every join and recurse-execute until the end of the graph.
	 * <br>
	 * Every execution should then do the insert/update/delete for the current objects and its joins.
	 * <br>
	 * @param connection the connection to use.
	 */
	@SuppressWarnings("unchecked")
	public void execute(IConnection connection) {
		Map<String, List<org.yop.orm.sql.BatchQuery>> delayable = new HashMap<>();
		this.execute(connection, delayable);

		List<org.yop.orm.sql.BatchQuery> merged = new ArrayList<>();
		delayable.values().forEach(batch -> merged.add(org.yop.orm.sql.BatchQuery.merge(batch)));

		merged.forEach(batch -> Executor.executeQuery(connection, batch));
	}

	/**
	 * Execute the upsert request, using batches when possible.
	 * <br>
	 * <br>
	 * <b>How is it supposed to work ?</b>
	 * <br>
	 * The idea here is to create a sub-upsert request for every join and recurse-execute until the end of the graph.
	 * <br>
	 * Every execution should then do the insert/update/delete for the current objects and its joins.
	 * <br>
	 * @param connection the connection to use.
	 * @param delayed    the delayable batch queries, that should be executed after this method ends
	 */
	@SuppressWarnings("unchecked")
	private void execute(IConnection connection, Map<String, List<org.yop.orm.sql.BatchQuery>> delayed) {
		if(this.elements.isEmpty()) {
			logger.warn("Upsert on no element. Are you sure you did not forget using #onto() ?");
			return;
		}

		// Recurse through the data graph to upsert data tables, by creating a sub upsert for every join
		for (T element : this.elements) {
			for (IJoin<T, ? extends Yopable> join : this.joins) {
				BatchUpsert sub = this.subUpsert(join, element);
				if(sub != null) {
					for (IJoin<? extends Yopable, ? extends Yopable> iJoin : join.getJoins()) {
						sub.join(iJoin);
					}
					sub.execute(connection, delayed);
				}
			}
		}

		// If the user asked for natural key checking, do a preliminary SELECT request to find any existing ID
		if (this.checkNaturalID) {
			Select<T> naturalIDQuery = Select.from(this.target);
			for (T element : this.elements.stream().filter(e -> e.getId() == null).collect(Collectors.toList())) {
				naturalIDQuery.where().or(new NaturalKey<>(element));
			}
			Map<T, T> existing = Maps.uniqueIndex(naturalIDQuery.execute(connection, Select.Strategy.EXISTS), e -> e);

			// Assign ID on an element if there is a saved element that matched its natural ID
			// ⚠⚠⚠ The equals and hashcode method are quite important here ! ⚠⚠⚠
			this.elements.forEach(e -> e.setId(existing.getOrDefault(e, e).getId()));
		}

		// Upsert the current data table and, when required, set the generated ID
		Collection<T> updated = upsertOrDelay(this.toSQL(), delayed, connection);

		// Upsert the relation tables of the specified joins (DELETE then INSERT, actually)
		for (IJoin<T, ? extends Yopable> join : this.joins) {
			updateRelation(connection, updated, join, delayed);
		}
	}

	/**
	 * Create a batch sub-Upsert request for the given join, on a given source element.
	 * @param join the join to use for this sub-upsert
	 * @param on   the source element
	 * @param <U>  the target type of the sub-upsert
	 * @return the sub-upsert, or null if the field value is null
	 * @throws YopMappingException invalid field mapping for the given join
	 */
	@SuppressWarnings("unchecked")
	private <U extends Yopable> BatchUpsert<U> subUpsert(IJoin<T, U> join, T on) {
		Field field = join.getField(this.target);
		try {
			Object children = field.get(on);
			if(children == null) {
				return null;
			}

			if (children instanceof Collection) {
				if(! ((Collection) children).isEmpty()) {
					return (BatchUpsert<U>) new BatchUpsert<>(join.getTarget(field)).onto((Collection<U>) children);
				}
				return null;
			} else if (children instanceof Yopable) {
				return (BatchUpsert<U>) new BatchUpsert<>(join.getTarget(field)).onto((U) children);
			}

			throw new YopMappingException(
				"Invalid type [" + children.getClass().getName()
				+ "] for [" + field.getDeclaringClass().getName() + "#" + field.getName()
				+ "] on [" + on + "]"
			);

		} catch (IllegalAccessException e) {
			throw new YopMappingException(
				"Could not access [" + field.getDeclaringClass().getName() + "#" + field.getName() + "] on [" + on + "]"
			);
		}
	}

	/**
	 * Generate a couple of SQL batch Queries that will effectively do the upsert request.
	 * @return the Upsert queries for the current Upsert : 1 for inserts, one for updates.
	 */
	private List<org.yop.orm.sql.Query> toSQL() {
		List<T> elementsToInsert = new ArrayList<>();
		List<T> elementsToUpdate = new ArrayList<>();

		for (T element : this.elements) {
			if(element.getId() == null) {
				elementsToInsert.add(element);
			} else {
				elementsToUpdate.add(element);
			}
		}

		List<org.yop.orm.sql.Query> out = new ArrayList<>();
		if(!elementsToInsert.isEmpty()) {
			if (Constants.USE_BATCH_INSERTS) {
				out.add(toSQLInserts(elementsToInsert));
			} else {
				elementsToInsert.forEach(element -> out.add(super.toSQLInsert(element)));
			}
		}
		if(!elementsToUpdate.isEmpty()) {
			out.add(toSQLUpdates(elementsToUpdate));
		}

		return out;
	}

	/**
	 * Create a batch query for all the given elements to insert.
	 * @param elementsToInsert the elements to insert.
	 * @return an INSERT batch query for the elements
	 */
	@SuppressWarnings("unchecked")
	private BatchQuery<T> toSQLInserts(List<T> elementsToInsert) {
		if(elementsToInsert.isEmpty()) {
			throw new YopRuntimeException("Trying to create batch query on no target element !");
		}

		BatchQuery<T> query = null;
		for (T element : elementsToInsert) {
			Parameters parameters = this.values(element);

			// First element : create the SQL query.
			// Every next element will simply be added as a new batch for the same query :)
			if(query == null) {
				List<String>  columns = parameters.stream().map(Parameter::getName).collect(Collectors.toList());
				List<String> values = parameters.stream().map(Parameter::toSQLValue).collect(Collectors.toList());

				String sql = MessageFormat.format(
					INSERT,
					this.getTableName(),
					Joiner.on(", ").join(columns),
					Joiner.on(", ").join(values)
				);
				query = new BatchQuery<>(sql, elementsToInsert, this.target);
				query.askGeneratedKeys(true, this.target);
			}

			parameters.removeIf(Parameter::isSequence);
			query.addParametersBatch(parameters);
		}
		return query;
	}

	/**
	 * Create a batch query for all the given elements to update.
	 * @param elementsToUpdate the elements to update.
	 * @return an UPDATE batch query for the elements
	 */
	@SuppressWarnings("unchecked")
	private BatchQuery<T> toSQLUpdates(List<T> elementsToUpdate) {
		if(elementsToUpdate.isEmpty()) {
			throw new YopRuntimeException("Trying to create batch query on no target element !");
		}

		BatchQuery<T> query = null;
		for (T element : elementsToUpdate) {
			Parameters parameters = this.values(element);

			// UPDATE query : exclude ID column.
			parameters.removeIf(p -> ORMUtil.getIdColumn(element.getClass()).equals(p.getName()));

			if(query == null) {
				this.target = (Class<T>) element.getClass();

				String whereClause = element.getIdColumn() + " = " + element.getId();
				String sql = MessageFormat.format(
					UPDATE,
					this.getTableName(),
					Joiner.on(',').join(parameters.stream().map(p -> p.getName() + "=?").collect(Collectors.toList())),
					whereClause
				);

				query = new BatchQuery<>(sql, elementsToUpdate, this.target);
			}

			parameters.removeIf(Parameter::isSequence);
			query.addParametersBatch(parameters);
		}
		return query;
	}

	/**
	 * Do the upsert queries or delay if possible (i.e. for now, if the query is not an INSERT)
	 * @param queries    the queries to execute/delay
	 * @param delayed    the delayed queries where to add the queries that can be delayed
	 * @param connection the connection to use to execute the queries
	 * @param <T> the queries target type
	 * @return the updated/delayed elements
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Yopable> Collection<T> upsertOrDelay(
		List<org.yop.orm.sql.Query> queries,
		Map<String, List<org.yop.orm.sql.BatchQuery>> delayed,
		IConnection connection) {

		Set<T> updated = new HashSet<>();
		for (org.yop.orm.sql.Query query : queries) {
			List<T> elements = getQueryElements(query);

			// For now, we have to do inserts immediately because we need the generated keys for further requests.
			// With a callback like mechanism on query Parameter value, we could maybe delay the inserts !
			if(query.askGeneratedKeys()) {
				insert(query, elements, connection);
			} else {
				delay(query, delayed);
			}
			updated.addAll(elements);
		}

		return updated;
	}

	/**
	 * Execute an INSERT query (either batch or not) and set the generated IDs on the source elements.
	 * @param insert     the INSERT query
	 * @param elements   the source elements - their respective IDs will be set using the generated IDs.
	 * @param connection the connection to use for the request(s)
	 * @param <T> the type of the source elements to upsert.
	 */
	private static <T extends Yopable> void insert(
		org.yop.orm.sql.Query insert,
		List<T> elements,
		IConnection connection) {

		Executor.executeQuery(connection, insert);
		List<Long> generatedIds = insert.getGeneratedIds();

		if (generatedIds.isEmpty() || generatedIds.size() == elements.size()) {
			for (int i = 0; i < generatedIds.size(); i++) {
				elements.get(i).setId(generatedIds.get(i));
			}
		} else {
			throw new YopRuntimeException(
				"Generated IDs length [" + generatedIds.size() + "] "
				+ "does not match the query inserted elements size [" + elements.size() + "]. "
				+ "Maybe your JDBC driver does not support batch inserts. Sorry about that. "
				+ "Query was [" + insert + "]."
			);
		}
	}

	/**
	 * Delay the given query, i.e. add the query to the delayed map.
	 * @param query   the query to delay
	 * @param delayed the delayed queries map
	 * @throws YopRuntimeException if the query is not a {@link org.yop.orm.sql.BatchQuery}.
	 */
	private static void delay(org.yop.orm.sql.Query query, Map<String, List<org.yop.orm.sql.BatchQuery>> delayed) {
		delayed.putIfAbsent(query.getSql(), new ArrayList<>());
		if (query instanceof org.yop.orm.sql.BatchQuery) {
			delayed.get(query.getSql()).add((org.yop.orm.sql.BatchQuery) query);
		} else {
			throw new YopRuntimeException(
				"Query to delay [" + query + "] is not a batch query ! "
				+ "This is not expected in a batch upsert !"
			);
		}
	}

	/**
	 * Update a relationship for the given source elements, using batch queries if possible.
	 * <br><br>
	 * This method will generate and execute :
	 * <ol>
	 *     <li>1 DELETE query to wipe any entry related to the source elements in the relation table</li>
	 *     <li>1 INSERT batch query to create every From → To entry</li>
	 * </ol>
	 * @param connection the connection to use
	 * @param elements   the source elements
	 * @param join       the join clause (≈ relation table)
	 * @param <T> the source type
	 */
	private static <T extends Yopable> void updateRelation(
		IConnection connection,
		Collection<T> elements,
		IJoin<T, ? extends Yopable> join,
		Map<String, List<org.yop.orm.sql.BatchQuery>> delayed) {

		Relation<T, ? extends Yopable> relation = new Relation<>(elements, join);
		for (org.yop.orm.sql.Query query : relation.toSQLDelete()) {
			Executor.executeQuery(connection, query);
		}

		for (org.yop.orm.sql.BatchQuery insert : relation.toSQLBatchInsert()) {
			delayed.putIfAbsent(insert.getSql(), new ArrayList<>());
			delayed.get(insert.getSql()).add(insert);
		}
	}

	/**
	 * Get the query elements, whether a {@link BatchQuery} or {@link org.yop.orm.query.Upsert.Query}.
	 * @param query the query
	 * @param <T> the query target type
	 * @return the elements concerned by the query
	 * @throws ClassCastException if query is neither a {@link BatchQuery} nor a {@link org.yop.orm.query.Upsert.Query}.
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Yopable> List<T> getQueryElements(org.yop.orm.sql.Query query) {
		if (query instanceof BatchQuery) {
			return ((BatchQuery) query).elements;
		} else {
			return Collections.singletonList((T) ((Query) query).getElement());
		}
	}

	/**
	 * SQL batch query + source elements aggregation.
	 */
	private static class BatchQuery<T extends Yopable> extends org.yop.orm.sql.BatchQuery {
		private final List<T> elements;

		private BatchQuery(String sql, List<T> elements, Class<T> target) {
			super(sql);
			this.elements = elements;
			this.target = target;
		}
	}
}
