package org.yop.orm.query.batch;

import org.yop.orm.sql.BatchQuery;
import org.yop.orm.sql.Query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A map of queries that can be postponed.
 * <br>
 * Before they are finally executed, you can try to {@link #merge()} them into {@link BatchQuery} :
 * when the SQL query is the same,
 * the {@link org.yop.orm.sql.Parameters} are simply added as a new batch for the same query.
 * <br>
 * This is an {@link LinkedHashMap}, so the query order should be kept!
 */
class DelayedQueries extends LinkedHashMap<String, List<Query>> {

	/**
	 * Merge the queries of this map, when possible.
	 * <br>
	 * For each key of this map, the collection values will be merged using {@link BatchQuery#merge(List)}
	 * and added into the output query list.
	 * @return the delayed queries, merged when possible, that can be executed.
	 */
	public List<Query> merge() {
		return this.values().stream().map(BatchQuery::merge).collect(ArrayList::new, List::addAll, List::addAll);
	}

}
