package org.yop.orm.query.relation;

import org.yop.orm.annotations.JoinTable;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.IJoin;
import org.yop.orm.sql.*;
import org.yop.orm.util.ORMUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A {@link JoinTable} relation between Java objects, that can generate UPSERT/DELETE queries.
 * <br>
 * The generated SQL is simply about adding/removing entries from a join table.
 * <br>
 * Then, there is no UPDATE query that will be generated using this Relation.
 * @param <From> the relation source type
 * @param <To>   the relation target type
 */
class JoinTableRelation<From extends Yopable, To extends Yopable> implements Relation {

	/**
	 * The relations to update.
	 * <br>
	 * Relations might actually be N ↔ N but Yop API deals with oriented acyclic graphs.
	 * <br><br>
	 * <b> 1 From → N To, several times</b>
	 */
	private final Map<From, Collection<To>> relations = new HashMap<>();

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
	JoinTableRelation(Collection<From> sources, IJoin<From, To> join) {
		sources.forEach(source -> this.relations.put(source, join.getTarget(source)));

		if(! sources.isEmpty()) {
			From source = sources.iterator().next();
			Field field = join.getField((Class<From>) source.getClass());
			JoinTable joinTable = field.getAnnotation(JoinTable.class);

			this.relationTable = ORMUtil.getJoinTableQualifiedName(joinTable);
			this.sourceColumn = joinTable.sourceColumn();
			this.targetColumn = joinTable.targetColumn();
		}
	}

	/**
	 * Build the DELETE queries for this relation.
	 * <br>
	 * It is actually one single query that deletes every row for the given source objects IDs.
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the delete query, as a singleton list
	 */
	@Override
	public Collection<Query> toSQLDelete(Config config) {
		if (this.relations.isEmpty()) {
			return new ArrayList<>(0);
		}

		Parameters parameters = new Parameters();
		Collection<From> sources = this.relations.keySet();
		sources.forEach(from -> parameters.addParameter(this.relationTable + "#" + this.sourceColumn, from::getId));
		List<String> ids = sources.stream().map(f -> "?").collect(Collectors.toList());

		String sql = config.getDialect().deleteIn(
			this.relationTable,
			this.sourceColumn,
			ids
		);
		return Collections.singletonList(new SimpleQuery(sql, Query.Type.DELETE, parameters, config));
	}

	/**
	 * Build the <b>INSERT</b> queries for this relation.
	 * <br>
	 * You should have used {@link #toSQLDelete(Config)} before :)
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the insert queries
	 */
	@Override
	public Collection<Query> toSQLInsert(Config config) {
		Collection<Query> inserts = new ArrayList<>();

		String insert = config.getDialect().insert(
			this.relationTable,
			Arrays.asList(this.sourceColumn, this.targetColumn),
			Arrays.asList("?", "?")
		);
		for (Map.Entry<From, Collection<To>> relation : this.relations.entrySet()) {
			From from = relation.getKey();

			for (To to : relation.getValue()) {
				Parameters parameters = new Parameters();
				parameters.addParameter(this.relationTable + "#" + this.sourceColumn, from::getId);
				parameters.addParameter(this.relationTable + "#" + this.targetColumn, to::getId);
				inserts.add(new SimpleQuery(insert, Query.Type.INSERT, parameters, config));
			}
		}

		return inserts;
	}

	/**
	 * Build the <b>INSERT</b> queries for this relation.
	 * <br>
	 * You should have used {@link #toSQLDelete(Config)} before :)
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the insert queries
	 */
	@Override
	public Collection<Query> toSQLBatchInsert(Config config) {
		Collection<Query> inserts = new ArrayList<>();

		String insert = config.getDialect().insert(
			this.relationTable,
			Arrays.asList(this.sourceColumn, this.targetColumn),
			Arrays.asList("?", "?")
		);
		for (Map.Entry<From, Collection<To>> relation : this.relations.entrySet()) {
			BatchQuery batchQuery = new BatchQuery(insert, Query.Type.INSERT, config);
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

	@Override
	public String toString() {
		return "JoinColumnRelation{" +
			"relationTable='" + this.relationTable+ '\'' +
			", sourceColumn='" + this.sourceColumn + '\'' +
			", targetColumn='" + this.targetColumn + '\'' +
			", From(" + RelationsToString.from(this.relations) + ")→To(" + RelationsToString.to(this.relations) + ")" +
			", relations=" + RelationsToString.toString(this.relations) +
		'}';
	}
}
