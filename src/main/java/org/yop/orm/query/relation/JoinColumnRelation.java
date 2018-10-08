package org.yop.orm.query.relation;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.annotations.JoinColumn;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.IJoin;
import org.yop.orm.sql.BatchQuery;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;
import org.yop.orm.sql.SimpleQuery;
import org.yop.orm.util.ORMUtil;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;

/**
 * A {@link JoinColumn} relation between Java objects, that can generate UPSERT/DELETE queries.
 * <br>
 * The generated SQL is all about updating relation column values.
 * <br>
 * Then, there is no INSERT/DELETE queries that will be generated using this Relation.
 * <br><br>
 * {@link JoinColumn} can either be unidirectional (either {@link JoinColumn#local()} or {@link JoinColumn#remote()} is set)
 * or bidirectional (both {@link JoinColumn#local()} and {@link JoinColumn#remote()} are set)
 * @param <From> the relation source type
 * @param <To>   the relation target type
 */
class JoinColumnRelation<From extends Yopable, To extends Yopable> implements Relation {

	private static final String UPDATE = " UPDATE {0} SET {1} = {2} WHERE {3} = {4} ";

	/** The source table name */
	private String sourceTable;

	/** The target table name */
	private String targetTable;

	/** The source table column that references an ID from the target table */
	private String sourceColumn;

	/** The target table column that references an ID from the source table */
	private String targetColumn;

	/**
	 * The relations to update.
	 * <br>
	 * Relations might actually be N ↔ N but Yop API deals with oriented acyclic graphs.
	 * <br><br>
	 * <b> 1 From → N To, several times</b>
	 */
	private final Map<From, Collection<To>> relations = new HashMap<>();

	/**
	 * Default constructor.
	 * Please use the 'factory' method {@link Relation#relation(Collection, IJoin)}, for consistency.
	 * @param sources the source objects
	 * @param join    the join directive
	 */
	@SuppressWarnings("unchecked")
	JoinColumnRelation(Collection<From> sources, IJoin<From, To> join) {
		sources.forEach(source -> this.relations.put(source, join.getTarget(source)));

		if(! sources.isEmpty()) {
			From source = sources.iterator().next();
			Field field = join.getField((Class<From>) source.getClass());
			JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);

			if (StringUtils.isNotBlank(joinColumn.local())) {
				this.sourceTable  = ORMUtil.getTableQualifiedName(source.getClass());
				this.sourceColumn = joinColumn.local();
			}

			if (StringUtils.isNotBlank(joinColumn.remote())) {
				this.targetTable  = ORMUtil.getTableQualifiedName(join.getTarget(field));
				this.targetColumn = joinColumn.remote();
			}
		}
	}

	/**
	 * Generate SQL update queries, for the given source objects, using the join directive.
	 * <br>
	 * The queries use {@link org.yop.orm.sql.Parameters.DelayedValue} to reference the objects IDs.
	 * @return a collection of {@link Query.Type#UPDATE} queries
	 */
	@Override
	public Collection<Query> toSQLUpdate() {
		Collection<Query> updates = new ArrayList<>(0);
		for (Map.Entry<From, Collection<To>> entry : this.relations.entrySet()) {
			From from = entry.getKey();

			if (this.sourceTable != null) {
				String sql = update(this.sourceTable, this.sourceColumn, entry.getKey().getIdColumn());
				for (To to : entry.getValue()) {
					Parameters parameters = new Parameters()
						.addParameter(this.sourceTable + "#" + this.sourceColumn, to::getId)
						.addParameter(this.sourceTable + "#id", from::getId);
					updates.add(new SimpleQuery(sql, Query.Type.UPDATE, parameters));
				}
			}

			if (this.targetTable != null) {
				for (To to : entry.getValue()) {
					String sql = update(this.targetTable, this.targetColumn, to.getIdColumn());
					Parameters parameters = new Parameters()
						.addParameter(this.targetTable + "#" + this.targetColumn, from::getId)
						.addParameter(this.targetTable + "#id", to::getId);
					updates.add(new SimpleQuery(sql, Query.Type.UPDATE, parameters));
				}
			}
		}
		return updates;
	}

	/**
	 * Generate SQL batch update queries, for the given source objects, using the join directive.
	 * <br>
	 * The queries use {@link org.yop.orm.sql.Parameters.DelayedValue} to reference the objects IDs.
	 * <br>
	 * This method simply uses {@link BatchQuery#merge(List)} on {@link Relation#toSQLUpdate()}.
	 * @return a collection of {@link Query.Type#UPDATE} queries
	 */
	@Override
	public Collection<Query> toSQLBatchUpdate() {
		Collection<Query> queries = this.toSQLUpdate();
		return BatchQuery.merge((List<Query>) queries);
	}

	@Override
	public String toString() {
		return "JoinColumnRelation{" +
			"sourceTable='" + this.sourceTable + '\'' +
			", targetTable='" + this.targetTable + '\'' +
			", sourceColumn='" + this.sourceColumn + '\'' +
			", targetColumn='" + this.targetColumn + '\'' +
			", From(" + RelationsToString.from(this.relations) + ")→To(" + RelationsToString.to(this.relations) + ")" +
			", relations=" + RelationsToString.toString(this.relations) +
		'}';
	}

	/**
	 * Update query formatting.
	 * @param table    the target table
	 * @param column   the target column
	 * @param idColumn the ID column
	 * @return the formatted UPDATE query : UPDATE [table] SET [column] = ? WHERE [idColumn] = ?
	 */
	private static String update(String table, String column, String idColumn) {
		return MessageFormat.format(
			UPDATE,
			table,
			column,
			"?",
			idColumn,
			"?"
		);
	}
}
