package org.yop.orm.sql;

import org.apache.commons.lang3.StringUtils;
import org.yop.orm.exception.YopRuntimeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A batch query is an SQL query with several batches of parameters.
 */
public class BatchQuery extends Query {

	/** The query parameters batches */
	private List<Parameters> parametersBatches = new ArrayList<>();

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
	 * @param queries the queries to merge.
	 * @return a single deduplicated BatchQuery (as a singleton list)
	 *         or simply the deduplicated queries if {@link Constants#USE_BATCH_INSERTS} is set to false.
	 * @throws YopRuntimeException when there are more than 1 SQL query among the queries
	 */
	@SuppressWarnings("unchecked")
	public static List<Query> merge(List<Query> queries) {
		List<Query> uniqueQueries = queries.stream().distinct().collect(Collectors.toList());
		BatchQuery merged = null;

		for (Query query : uniqueQueries) {
			if(merged == null) {
				if(query.getType() == Type.INSERT && ! Constants.USE_BATCH_INSERTS) {
					// We are asked not to batch INSERT queries (driver limitation, for instance)
					return uniqueQueries;
				}

				merged = new BatchQuery(query.getSql(), query.getType());
				merged.target = query.target;
				merged.askGeneratedKeys(query.askGeneratedKeys(), query.getTarget());
			}
			merged.getElements().addAll(query.elements);

			if(!StringUtils.equals(merged.sql, query.getSql())) {
				throw new YopRuntimeException("Could not merge batch queries with different SQL !");
			}

			if (query instanceof SimpleQuery) {
				merged.parametersBatches.add(query.getParameters());
			} else if (query instanceof org.yop.orm.sql.BatchQuery){
				merged.parametersBatches.addAll(((BatchQuery) query).parametersBatches);
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
}
