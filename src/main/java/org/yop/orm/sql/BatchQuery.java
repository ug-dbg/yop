package org.yop.orm.sql;

import java.util.ArrayList;
import java.util.List;

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
	public BatchQuery(String sql) {
		super(sql);
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
		if(this.batchCursor >= this.parametersBatches.size()) {
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
}
