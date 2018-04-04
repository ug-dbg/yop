package org.yop.orm.sql;

import org.apache.commons.lang3.StringUtils;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.model.YopableEquals;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A batch query is an SQL query with several batches of parameters.
 * <br>
 *     <b>
 *         If there are some {@link #elements} in this query, it is somehow assumed that :
 *         <ul>
 *             <li>There are as many elements as parameter batches</li>
 *             <li>element[i] ↔ parameter_batch[i]</li>
 *         </ul>
 *     </b>
 *     This is not some very good design. I should do something about it.
 * <br>
 */
public class BatchQuery extends Query {

	/** The query parameters batches */
	private final List<Parameters> parametersBatches = new ArrayList<>();

	/** The query parameters batches index. Init at -1. Use {@link #nextBatch()} to move the cursor. */
	private Integer batchCursor = -1;

	/**
	 * Default constructor : SQL query.
	 *
	 * @param sql the SQL query to execute
	 */
	public BatchQuery(String sql, Type type) {
		super(sql, type);
	}

	/**
	 * Add a batch of parameters to the query parameters batches {@link #parametersBatches}.
	 * @param batch the parameters batch
	 * @return the current query, for chaining purposes
	 */
	public BatchQuery addParametersBatch(Parameters batch) {
		this.parametersBatches.add(batch);
		return this;
	}

	/**
	 * If there are some batches left after the current cursor position, increment the position and return true.
	 * <br>
	 * Return false else.
	 * <br><br>
	 * {@inheritDoc}
	 */
	@Override
	public boolean nextBatch() {
		if(this.batchCursor >= this.parametersBatches.size() - 1) {
			return false;
		}
		this.batchCursor++;
		return true;
	}

	/**
	 * Return the {@link Parameters} from {@link #parametersBatches} at index {@link #batchCursor}.
	 * <br><br>
	 * {@inheritDoc}
	 */
	@Override
	public Parameters getParameters() {
		if (this.batchCursor < 0) {
			throw new ArrayIndexOutOfBoundsException(
				"The current batch query parameters cursor "
				+ "has not been initialized. Please use Query#nextBatch first."
				+ "Query is [" + this.toString() + "]"
			);
		}

		if (this.batchCursor >= this.parametersBatches.size()) {
			throw new ArrayIndexOutOfBoundsException(
				"The current batch query parameters cursor [" + this.batchCursor + "] "
				+ "is too high for the parameters batches whose size is [" + this.parametersBatches.size() + "]. "
				+ "Query is [" + this.toString() + "]"
			);
		}
		return this.parametersBatches.get(this.batchCursor);
	}

	@Override
	public String parametersToString() {
		return String.valueOf(this.parametersBatches);
	}

	@Override
	public String toString() {
		return "BatchQuery{" +
			"sql='" + this.sql + '\'' +
			", parametersBatches=" + this.parametersBatches +
			", target="            + this.target +
			", askGeneratedKeys="  + this.askGeneratedKeys +
			", generatedIds="      + this.generatedIds +
			", tooLongAliases="    + this.tooLongAliases +
		'}';
	}

	/**
	 * Deduplicate and merge a list of queries that have the same SQL.
	 * <br>
	 * If the query type is {@link Query.Type#INSERT}, query parameters and source elements are deduplicated :
	 * <ul>
	 *     <li>if there are {@link Query#elements}, it is assumed that 1 element ↔ 1 parameter batch</li>
	 *     <li>if there are no {@link Query#elements}, all parameter batches are added</li>
	 * </ul>
	 * @param queries the queries to merge.
	 * @return a single deduplicated BatchQuery (as a singleton list)
	 *         or simply the deduplicated queries if {@link Constants#USE_BATCH_INSERTS} is set to false.
	 * @throws YopRuntimeException when there are more than 1 SQL query among the queries
	 */
	public static List<Query> merge(List<Query> queries) {
		List<Query> uniqueQueries = queries.stream().distinct().collect(Collectors.toList());
		BatchQuery merged = null;

		// For insert queries, we are going to deduplicate, using YopableEquals.equals.
		Set<YopableEquals> deduplicated = new HashSet<>();

		for (Query query : uniqueQueries) {
			if(merged == null) {
				if(query.getType() == Type.INSERT && ! Constants.USE_BATCH_INSERTS) {
					// We are asked not to batch INSERT queries (driver limitation, for instance)
					return uniqueQueries;
				}
				merged = toBatch(query);
			}

			if(!StringUtils.equals(merged.sql, query.getSql())) {
				throw new YopRuntimeException("Could not merge batch queries with different SQL !");
			}

			if (query.getType() == Type.INSERT && ! query.getElements().isEmpty()) {
				mergeInsert(query, merged, deduplicated);
			} else {
				merge(query, merged);
			}
		}

		// Only one batch ? Return a SimpleQuery, for coherence purposes.
		if (merged != null && merged.parametersBatches.size() == 1) {
			return Collections.singletonList(
				new SimpleQuery(merged.sql, merged.getType(), merged.parametersBatches.get(0))
			);
		}

		return Collections.singletonList(merged);
	}

	/**
	 * Create a batch query from a query.
	 * <br>
	 * <b>Source elements and query parameters are not added here !</b>
	 * @param query the source query
	 * @return a batch query with the same {@link Query#sql}, {@link Query#target} and {@link #askGeneratedKeys}
	 */
	private static BatchQuery toBatch(Query query) {
		BatchQuery batch = new BatchQuery(query.getSql(), query.getType());
		batch.target = query.target;
		batch.askGeneratedKeys(query.askGeneratedKeys(), query.getTarget());
		return batch;
	}

	/**
	 * Merge an {@link Query.Type#INSERT} query into a batch query.
	 * <br>
	 * This method checks for duplicate (See {@link org.yop.orm.annotations.NaturalId}) in the source elements.
	 * @param insert       the INSERT query
	 * @param onto         the target batch query
	 * @param deduplicated a set of deduplicated source elements. It will be updated with the insert elements.
	 */
	private static void mergeInsert(Query insert, BatchQuery onto, Set<YopableEquals> deduplicated) {
		// Simple query : may have several source elements but no parameter batches
		if (insert instanceof SimpleQuery) {
			onto.parametersBatches.add(insert.getParameters());
		}

		// For each source element, deduplicate. And if batch query, add the parameter batch with same index.
		// In that latter case, it is assumed that :
		// element[i] ↔ parameter_batch[i]
		for (int i = 0; i < insert.getElements().size(); i++) {
			Yopable element = insert.getElements().get(i);

			if (!deduplicated.contains(new YopableEquals(element))) {
				onto.getElements().add(element);
				deduplicated.add(new YopableEquals(element));

				if (insert instanceof org.yop.orm.sql.BatchQuery){
					onto.parametersBatches.add(((BatchQuery) insert).parametersBatches.get(i));
				}
			}
		}
	}

	/**
	 * Merge a query into a target batch query.
	 * <br>
	 * <b>
	 *     This method does not check for duplicates at all ! Source elements and query parameters are added "as is" !
	 * </b>
	 * @param query the source query
	 * @param onto  the target batch query
	 */
	private static void merge(Query query, BatchQuery onto) {
		onto.getElements().addAll(query.elements);

		if (query instanceof SimpleQuery) {
			onto.parametersBatches.add(query.getParameters());
		} else if (query instanceof org.yop.orm.sql.BatchQuery){
			onto.parametersBatches.addAll(((BatchQuery) query).parametersBatches);
		}
	}
}
