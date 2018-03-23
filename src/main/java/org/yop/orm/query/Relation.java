package org.yop.orm.query;

import org.yop.orm.annotations.JoinTable;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.BatchQuery;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;
import org.yop.orm.sql.SimpleQuery;
import org.yop.orm.util.MessageUtil;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A relationship between two Yopable classes.
 * <br>
 * This class can generate the SQL you need to update a relation table from a {@link Join}.
 * <br>
 * A relation is oriented (<i>From → To</i>) and comes from a {@link Join} clause.
 * <br>
 * So {@code Relation<From, To>} ≠ {@code Relation<To, From>}
 * <br>
 * @param <From> the source type
 * @param <To>   the target type
 */
public class Relation<From extends Yopable, To extends Yopable> {

	private static final String DELETE_IN = " DELETE FROM {0} WHERE {1} IN ({2}) ";
	private static final String INSERT    = " INSERT INTO {0} ({1},{2}) VALUES (?, ?) ";

	/**
	 * The relations to update.
	 * <br>
	 * Relations might actually be N ↔ N but Yop API deals with oriented acyclic graphs.
	 * <br><br>
	 * <b> 1 From → N To, several times</b>
	 */
	private Map<From, Collection<To>> relations = new HashMap<>();

	/** The relation table name */
	private String relationTable;

	/** The relation table source column (for the 'From' type) */
	private String sourceColumn;

	/** The relation table target column (for the 'To' type) */
	private String targetColumn;

	/**
	 * Default constructor. For every source, the join clause will be used to define the relations to CRUD.
	 * @param sources the source objects
	 * @param join    the join clause
	 */
	@SuppressWarnings("unchecked")
	public Relation(Collection<From> sources, IJoin<From, To> join) {
		sources.forEach(source -> this.relations.put(source, join.getTarget(source)));

		if(! sources.isEmpty()) {
			From source = sources.iterator().next();
			Field field = join.getField((Class<From>) source.getClass());
			JoinTable joinTable = field.getAnnotation(JoinTable.class);
			this.relationTable = joinTable.table();
			this.sourceColumn = joinTable.sourceColumn();
			this.targetColumn = joinTable.targetColumn();
		}
	}

	/**
	 * Build the DELETE queries for this relation.
	 * <br>
	 * It is actually one single query that deletes every row for the given source objects IDs.
	 * @return the delete query, as a singleton list
	 */
	public Collection<Query> toSQLDelete() {
		Parameters parameters = new Parameters();
		String sql = deleteIn(
			this.relationTable,
			this.sourceColumn,
			this.relations.keySet().stream().map(y -> {
				parameters.addParameter(this.relationTable + "#" + sourceColumn, y::getId);
				return "?";
			}).collect(Collectors.toList())
		);
		return Collections.singletonList(new SimpleQuery(sql, Query.Type.DELETE, parameters));
	}

	/**
	 * Build the <b>INSERT</b> queries for this relation.
	 * <br>
	 * You should have used {@link #toSQLDelete()} before :)
	 * @return the insert queries
	 */
	public Collection<Query> toSQLInsert() {
		Collection<Query> inserts = new ArrayList<>();

		for (Map.Entry<From, Collection<To>> relation : this.relations.entrySet()) {
			String insert = insert(this.relationTable, this.sourceColumn, this.targetColumn);
			From from = relation.getKey();

			for (To to : relation.getValue()) {
				Parameters parameters = new Parameters();
				parameters.addParameter(this.relationTable + "#" + this.sourceColumn, from::getId);
				parameters.addParameter(this.relationTable + "#" + this.targetColumn, to::getId);
				inserts.add(new SimpleQuery(insert, Query.Type.INSERT, parameters));
			}
		}

		return inserts;
	}

	/**
	 * Build the <b>INSERT</b> queries for this relation.
	 * <br>
	 * You should have used {@link #toSQLDelete()} before :)
	 * @return the insert queries
	 */
	public Collection<BatchQuery> toSQLBatchInsert() {
		Collection<BatchQuery> inserts = new ArrayList<>();

		for (Map.Entry<From, Collection<To>> relation : this.relations.entrySet()) {
			String insert = insert(this.relationTable, this.sourceColumn, this.targetColumn);
			BatchQuery batchQuery = new BatchQuery(insert, Query.Type.INSERT);
			From from = relation.getKey();

			for (To to : relation.getValue()) {
				Parameters parameters = new Parameters();
				parameters.addParameter(this.relationTable + "#" + this.sourceColumn, from::getId);
				parameters.addParameter(this.relationTable + "#" + this.targetColumn, to::getId);
				batchQuery.addParametersBatch(parameters);
			}
			inserts.add(batchQuery);
		}

		return inserts;
	}

	/**
	 * Generate a 'DELETE IN' query
	 * @param from         the relation table name
	 * @param sourceColumn the source column name
	 * @param ids          the IDs for the IN clause. Mostly a list of '?' if you want to use query parameters.
	 * @return the {@link #DELETE_IN} formatted query
	 */
	private static String deleteIn(String from, String sourceColumn, Collection<String> ids) {
		return MessageFormat.format(DELETE_IN, from, sourceColumn, MessageUtil.join(",", ids));
	}

	/**
	 * Generate a 'DELETE IN' query. The formatted query has 2 query parameters, for source and target column values.
	 * @param into         the relation table name
	 * @param sourceColumn the source column name
	 * @param targetColumn the target column name
	 * @return the {@link #INSERT} formatted query
	 */
	private static String insert(String into, String sourceColumn, String targetColumn) {
		return MessageFormat.format(INSERT, into, sourceColumn, targetColumn);
	}
}
