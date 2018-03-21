package org.yop.orm.query.batch;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.evaluation.NaturalKey;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.IJoin;
import org.yop.orm.query.Relation;
import org.yop.orm.query.Select;
import org.yop.orm.query.Upsert;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.ORMUtil;

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
		if(this.elements.isEmpty()) {
			logger.warn("Upsert on no element. Are you sure you did not forget using #onto() ?");
			return;
		}

		// Recurse through the data graph to upsert data tables, by creating a sub upsert for every join
		for (T element : this.elements) {
			for (IJoin<T, ? extends Yopable> join : this.joins) {
				Upsert sub = this.subUpsert(join, element);
				if(sub != null) {
					for (IJoin<? extends Yopable, ? extends Yopable> iJoin : join.getJoins()) {
						sub.join(iJoin);
					}
					sub.execute(connection);
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
		Set<T> updated = new HashSet<>();
		for (BatchQuery<T> query : this.toSQL()) {
			Executor.executeQuery(connection, query);
			if(query.getGeneratedIds().isEmpty() || query.getGeneratedIds().size() == query.elements.size()) {
				for (int i = 0; i < query.getGeneratedIds().size(); i++) {
					query.elements.get(i).setId(query.getGeneratedIds().get(i));
				}
			} else {
				throw new YopRuntimeException(
					"Generated IDs length [" + query.getGeneratedIds().size() +"] "
					+ "does not match the query inserted elements size [" + query.elements.size() + "]"
				);
			}
			updated.addAll(query.elements);
		}

		// Upsert the relation tables of the specified joins (DELETE then INSERT, actually)
		for (IJoin<T, ? extends Yopable> join : this.joins) {
			updateRelation(connection, updated, join);
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
		IJoin<T, ? extends Yopable> join) {

		Relation<T, ? extends Yopable> relation = new Relation<>(elements, join);
		Collection<org.yop.orm.sql.Query> relationsQueries = new ArrayList<>();
		relationsQueries.addAll(relation.toSQLDelete());
		relationsQueries.addAll(relation.toSQLBatchInsert());
		for (org.yop.orm.sql.Query query : relationsQueries) {
			Executor.executeQuery(connection, query);
		}
	}

	/**
	 * Generate a couple of SQL batch Queries that will effectively do the upsert request.
	 * @return the Upsert queries for the current Upsert : 1 for inserts, one for updates.
	 */
	private List<BatchQuery<T>> toSQL() {
		List<T> elementsToInsert = new ArrayList<>();
		List<T> elementsToUpdate = new ArrayList<>();

		for (T element : this.elements) {
			if(element.getId() == null) {
				elementsToInsert.add(element);
			} else {
				elementsToUpdate.add(element);
			}
		}

		List<BatchQuery<T>> out = new ArrayList<>();
		if(!elementsToInsert.isEmpty()) {
			out.add(toSQLInserts(elementsToInsert));
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
	 * SQL batch query + parameters aggregation.
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
