package org.yop.orm.sql;

import org.apache.commons.lang3.StringUtils;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IResultCursor;
import org.yop.orm.util.ORMUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The query results. Aggregates :
 * <ul>
 *     <li>query</li>
 *     <li>result cursor</li>
 * </ul>
 */
public class Results {

	/** The query cursor (JDBC result set). The result set closing is not handled in this class at all ! */
	private final IResultCursor cursor;

	/** The query that was executed */
	private final Query query;

	/**
	 * A set of context paths known to this request.
	 * <br>
	 * RootClass[→relation→NextClass]*
	 * <br>
	 * This is a cache set for {@link #noContext(String, Class)}.
	 */
	private Set<String> contexts = new HashSet<>();

	/**
	 * key : context (RootClass[→relation→NextClass]*)
	 * <br>
	 * value : fully qualified ID column name ((RootClass[→relation→NextClass]*→ID_COLUMN_NAME)
	 * <br>
	 * This is a cache map for {@link #noContext(String, Class)}.
	 * <br><br>
	 * <b>N.B.</b> The map values are the shortened aliases of the fully qualified ID column names.
	 * See {@link Query#getShortened(String)}
	 */
	private Map<String, String> contextsIDColumns = new HashMap<>();

	/**
	 * Default constructor : resultset and original query
	 * @param cursor the resultset from the query execution
	 * @param query     the executed query
	 */
	Results(IResultCursor cursor, Query query) {
		this.cursor = cursor;
		this.query = query;
	}

	/**
	 * @return the resultset from the query execution
	 */
	public IResultCursor getCursor() {
		return this.cursor;
	}

	/**
	 * @return the executed query
	 */
	public Query getQuery() {
		return this.query;
	}

	/**
	 * Check if the current cursor row has eligible data for the given context.
	 * <br>
	 * The row does have context when there is a column for its ID and the value for this column is not null.
	 * @param context     the context to check (RootClass[→relation→NextClass]*)
	 * @param targetClass the target class (required to read the ID column name)
	 * @return true if there is no data for this context on the current row
	 */
	public boolean noContext(String context, Class<? extends Yopable> targetClass) {
		if (this.contexts.isEmpty()) {
			int columns = this.getCursor().getColumnCount();
			for (int x = 1; x <= columns; x++) {
				this.contexts.add(StringUtils.substringBeforeLast(
					this.getQuery().getAlias(this.getCursor().getColumnName(x)),
					this.query.config.sqlSeparator()
				));
			}
		}

		if (!this.contexts.contains(context)) {
			return true;
		}

		if (!this.contextsIDColumns.containsKey(context)) {
			String idColumn = this.getQuery().getShortened(
				context + this.query.getConfig().sqlSeparator() + ORMUtil.getIdColumn(targetClass)
			);
			this.contextsIDColumns.put(context, idColumn);
		}

		return this.cursor.getObject(this.contextsIDColumns.get(context)) == null;
	}
}
