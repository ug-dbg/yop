package org.yop.orm.query.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.IJoin;
import org.yop.orm.query.Upsert;
import org.yop.orm.query.relation.Relation;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.*;

import static org.yop.orm.sql.Parameters.Parameter;

/**
 * Batch Upsert : save or update instances of T to the database.
 * <br>
 * Use batch mechanism (see {@link org.yop.orm.sql.BatchQuery}) when possible.
 * <br>
 * Batches are very implementation sensitive. Some drivers does not seem to play well with this.
 * <br>
 * For instance {@link Statement#getGeneratedKeys()} might not be working. See {@link Config#useBatchInserts()}.
 * <br><br>
 * When delaying an insert, the generated ID might be required in further queries.
 * We use a {@link org.yop.orm.sql.Parameters.DelayedValue} to create a query parameter whose value is not yet known.
 * <br>
 * The {@link Yopable#getId()} method is referenced as the parameter's {@link org.yop.orm.sql.Parameters.DelayedValue}.
 * <br><br>
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
	 * Check for natural ID before insert. Merge and update existing rows.
	 * <br>
	 * @return the current BATCH UPSERT request, for chaining purpose
	 */
	protected BatchUpsert<T> checkNaturalID(boolean value, boolean propagate) {
		return (BatchUpsert<T>) super.checkNaturalID(value, propagate);
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
	public void execute(IConnection connection) {
		DelayedQueries delayable = new DelayedQueries();
		this.execute(connection, delayable);
		delayable.merge().forEach(batch -> Executor.executeQuery(connection, batch));
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
	 * @param delayed    the ordered delayable batch queries, that should be executed after this method ends
	 */
	@SuppressWarnings("unchecked")
	private void execute(IConnection connection, DelayedQueries delayed) {
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

		// If the user asked for natural key checking, do a preliminaryBATCH UPSERTrequest to find any existing ID
		if (this.checkNaturalID) {
			this.findNaturalIDs(connection);
		}

		// Upsert the current data table and, when required, set the generated ID
		Collection<T> updated = delay(this.toSQL(connection.config()), delayed);

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
		Field field = join.getField(this.getTarget());
		Object children = Reflection.readField(field, on);
		if(children == null) {
			return null;
		}

		Class<U> target = join.getTarget(field);
		boolean naturalKey = ! ORMUtil.getNaturalKeyFields(join.getTarget(field)).isEmpty();
		if (children instanceof Collection) {
			if(! ((Collection) children).isEmpty()) {
				return ((BatchUpsert<U>) new BatchUpsert<>(target)
					.onto((Collection<U>) children))
					.checkNaturalID(naturalKey && this.checkNaturalID, this.propagateCheckNaturalID);
			}
			return null;
		} else if (children instanceof Yopable) {
			return ((BatchUpsert<U>) new BatchUpsert<>(target)
				.onto((U) children))
				.checkNaturalID(naturalKey && this.checkNaturalID, this.propagateCheckNaturalID);
		}

		throw new YopMappingException(
			"Invalid type [" + children.getClass().getName() + "] " +
			"for [" + Reflection.fieldToString(field) + "] " +
			"on [" + on + "]"
		);

	}

	/**
	 * Generate a couple of SQL batch Queries that will effectively do the upsert request.
	 * @param config the SQL config. Needed for the sql separator to use or if batch inserts are allowed.
	 * @return the Upsert queries for the current Upsert : 1 for inserts, one for updates.
	 */
	private List<Query> toSQL(Config config) {
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
			if (config.useBatchInserts()) {
				out.add(this.toSQL(elementsToInsert, Query.Type.INSERT, config));
			} else {
				elementsToInsert.forEach(element -> out.add(this.toSQL(element, Query.Type.INSERT, config)));
			}
		}
		if(!elementsToUpdate.isEmpty()) {
			out.add(this.toSQL(elementsToUpdate, Query.Type.UPDATE, config));
		}

		return out;
	}

	/**
	 * Create a batch query for all the given elements to insert.
	 * @param elements the elements to insert.
	 * @param config  the SQL config (sql separator, use batch inserts...)
	 * @return an INSERT batch query for the elements
	 */
	private BatchQuery<T> toSQL(List<T> elements, Query.Type type, Config config) {
		if(elements.isEmpty()) {
			throw new YopRuntimeException("Trying to create batch query on no target element !");
		}

		BatchQuery<T> query = null;
		SimpleQuery reference = null;
		Class<T> target = this.context.getTarget();
		int batch = 0;
		for (T element : elements) {
			batch++;
			if (reference == null) {
				reference = this.toSQL(element, type, config);

				query = new BatchQuery<>(reference.getSql(), type, config, elements, target);
				query.addParametersBatch(reference.getParameters());
				query.askGeneratedKeys(Query.Type.INSERT == type, this.getTarget());
				continue;
			}
			Parameters parameters = new Parameters();
			for (Parameter parameter : reference.getParameters()) {
				Field field = parameter.getField();
				String name = parameter.getName();
				Object fieldValue = ORMUtil.readField(field, element);
				parameters.addParameter(name + "#" + batch, fieldValue, field, parameter.isSequence());
			}
			query.addParametersBatch(parameters);
		}
		return query;
	}

	/**
	 * Do delay the given queries (add to the delayed query map) and return all the queries' source elements.
	 * @param queries    the queries to delay
	 * @param delayed    the delayed queries where to add the queries that can be delayed
	 * @param <T> the queries target type
	 * @return the updated/delayed elements
	 */
	private static <T extends Yopable> Collection<T> delay(List<Query> queries, DelayedQueries delayed) {
		Set<T> updated = new HashSet<>();
		for (Query query : queries) {
			@SuppressWarnings("unchecked")
			List<T> elements = (List<T>) query.getElements();
			delayed.putIfAbsent(query.getSql(), new ArrayList<>());
			delayed.get(query.getSql()).add(query);
			updated.addAll(elements);
		}

		return updated;
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
	 * @param delayed    the delayed query map (any delayable query will be added to this map)
	 * @param <T> the source type
	 */
	private static <T extends Yopable> void updateRelation(
		IConnection connection,
		Collection<T> elements,
		IJoin<T, ? extends Yopable> join,
		DelayedQueries delayed) {

		Relation relation = Relation.relation(elements, join);
		for (Query query : relation.toSQLDelete(connection.config())) {
			Executor.executeQuery(connection, query);
		}

		for (Query insert : relation.toSQLBatchInsert(connection.config())) {
			delayed.putIfAbsent(insert.getSql(), new ArrayList<>());
			delayed.get(insert.getSql()).add(insert);
		}

		for (Query update : relation.toSQLBatchUpdate(connection.config())) {
			delayed.putIfAbsent(update.getSql(), new ArrayList<>());
			delayed.get(update.getSql()).add(update);
		}
	}

	/**
	 * SQL batch query with a typed element list constructor.
	 */
	private static class BatchQuery<T extends Yopable> extends org.yop.orm.sql.BatchQuery {
		private BatchQuery(String sql, Type type, Config config, List<T> elements, Class<T> target) {
			super(sql, type, config);
			this.elements.addAll(elements);
			this.target = target;
		}
	}
}
