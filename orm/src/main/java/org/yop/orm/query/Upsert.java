package org.yop.orm.query;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.Table;
import org.yop.orm.evaluation.NaturalKey;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.exception.YopSerializableQueryException;
import org.yop.orm.model.JsonAble;
import org.yop.orm.model.Yopable;
import org.yop.orm.model.YopableEquals;
import org.yop.orm.model.Yopables;
import org.yop.orm.query.relation.Relation;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Upsert : save or update instances of T to the database.
 * <br><br>
 * Example :
 * <br>
 * <pre>
 * {@code
 * Upsert.from(Organisation.class).checkNaturalID().onto(organisation).joinAll().execute(connection);
 * }
 * </pre>
 *
 * @param <T> the type to upsert.
 */
public class Upsert<T extends Yopable> implements JsonAble {

	private static final Logger logger = LoggerFactory.getLogger(Upsert.class);

	protected static final String INSERT = " INSERT INTO {0} ({1}) VALUES ({2}) ";
	protected static final String UPDATE = " UPDATE {0} SET {1} WHERE ({2}) ";

	/** Target class */
	protected Class<T> target;

	/** Join clauses */
	protected final IJoin.Joins<T> joins = new IJoin.Joins<>();

	/** Elements to save/update */
	protected final Yopables<T> elements = new Yopables<>(this.joins);

	/** If set to true, any insert will do a preliminary SELECT query to find any entry whose natural key matches */
	protected boolean checkNaturalID = false;

	/**
	 * Protected constructor, please use {@link #from(Class)}
	 * @param target the target class
	 */
	protected Upsert(Class<T> target) {
		this.target = target;
	}

	/**
	 * Create a sub-Upsert request for the given join, on a given source element.
	 * @param join the join to use for this sub-upsert
	 * @param on   the source element
	 * @param <U>  the target type of the sub-upsert
	 * @return the sub-upsert, or null if the field value is null
	 * @throws YopMappingException invalid field mapping for the given join
	 */
	@SuppressWarnings("unchecked")
	private <U extends Yopable> Upsert<U> subUpsert(IJoin<T, U> join, T on) {
		Field field = join.getField(this.target);
		Object children = Reflection.readField(field, on);
		if(children == null) {
			return null;
		}

		if (children instanceof Collection) {
			if(! ((Collection) children).isEmpty()) {
				return new Upsert<>(join.getTarget(field)).onto((Collection<U>) children);
			}
			return null;
		} else if (children instanceof Yopable) {
			return new Upsert<>(join.getTarget(field)).onto((U) children);
		}

		throw new YopMappingException(
			"Invalid type [" + children.getClass().getName() + "] " +
			"for [" + Reflection.fieldToString(field) + "] " +
			"on [" + on + "]"
		);

	}

	/**
	 * Init upsert request.
	 * @param clazz the target class
	 * @param <Y> the target type
	 * @return an UPSERT request instance
	 */
	public static <Y extends Yopable> Upsert<Y> from(Class<Y> clazz) {
		return new Upsert<>(clazz);
	}

	public JsonObject toJSON() {
		return this.toJSON(Context.root(this.target));
	}

	@Override
	public <U extends Yopable> JsonObject toJSON(Context<U> context) {
		JsonObject out = (JsonObject) JsonAble.super.toJSON(context);
		out.addProperty("target", this.target.getCanonicalName());
		return out;
	}

	public static <T extends Yopable> Upsert<T> fromJSON(String json) {
		try {
			JsonParser parser = new JsonParser();
			JsonObject selectJSON = (JsonObject) parser.parse(json);
			String targetClassName = selectJSON.getAsJsonPrimitive("target").getAsString();
			Class<T> target = Reflection.forName(targetClassName);
			Upsert<T> upsert = Upsert.from(target);
			upsert.fromJSON(Context.root(upsert.target), selectJSON);
			return upsert;
		} catch (RuntimeException e) {
			throw new YopSerializableQueryException(
				"Could not create query from JSON [" + StringUtils.abbreviate(json, 30) + "]", e
			);
		}
	}

	/**
	 * What is the target Yopable of this Upsert query ?
	 * @return {@link #target}
	 */
	public Class<T> getTarget() {
		return this.target;
	}

	/**
	 * (Left) join to a new type.
	 * @param join the join clause
	 * @param <R> the target join type
	 * @return the current SELECT request, for chaining purpose
	 */
	public <R extends Yopable> Upsert<T> join(IJoin<T, R> join) {
		this.joins.add(join);
		return this;
	}

	/**
	 * Update the whole data graph. Stop on transient fields.
	 * <br>
	 * <b>⚠⚠⚠ There must be no cycle in the data graph model ! ⚠⚠⚠</b>
	 * <br><br>
	 * <b>⚠⚠⚠ Any join previously set is cleared ! Please add transient fetch clause after this ! ⚠⚠⚠</b>
	 * @return the current SELECT request, for chaining purpose
	 */
	public Upsert<T> joinAll() {
		this.joins.clear();
		IJoin.joinAll(this.target, this.joins);
		return this;
	}

	/**
	 * Check for natural ID before insert. Merge and update existing rows.
	 * <br>
	 * @return the current SELECT request, for chaining purpose
	 */
	public Upsert<T> checkNaturalID() {
		this.checkNaturalID = true;
		return this;
	}

	/**
	 * Add an element to be saved/updated
	 * @param element the element to be saved/updated
	 * @return the current UPSERT, for chaining purposes
	 */
	public Upsert<T> onto(T element) {
		this.elements.add(element);
		return this;
	}

	/**
	 * Add several elements to be saved/updated
	 * @param elements the elements to be saved/updated
	 * @return the current UPSERT, for chaining purposes
	 */
	public Upsert<T> onto(Collection<T> elements) {
		this.elements.addAll(elements);
		return this;
	}

	/**
	 * Execute the upsert request.
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
			for (IJoin<T, ?> join : this.joins) {
				Upsert sub = this.subUpsert(join, element);
				if(sub != null) {
					for (IJoin iJoin : join.getJoins()) {
						sub.join(iJoin);
					}
					sub.execute(connection);
				}
			}
		}

		// If the user asked for natural key checking, do a preliminary SELECT request to find any existing ID
		if (this.checkNaturalID) {
			this.findNaturalIDs(connection);
		}

		// Upsert the current data table and, when required, set the generated ID
		Set<T> updated = new HashSet<>();
		for (SimpleQuery<T> query : this.toSQL()) {
			Executor.executeQuery(connection, query);
			updated.add(query.getElement());
		}

		// Upsert the relation tables of the specified joins (DELETE then INSERT, actually)
		for (IJoin<T, ? extends Yopable> join : this.joins) {
			updateRelation(connection, updated, join);
		}
	}

	/**
	 * For each element in {@link #elements} try to find its ID from database,
	 * using its {@link org.yop.orm.annotations.NaturalId}.
	 * <br>
	 * A {@link Select} query is executed with {@link NaturalKey} restrictions.
	 * @param connection the database connection to use
	 */
	protected void findNaturalIDs(IConnection connection) {
		// Find existing elements
		Select<T> naturalIDQuery = Select.from(this.target);
		for (T element : this.elements.stream().filter(e -> e.getId() == null).collect(Collectors.toList())) {
			naturalIDQuery.where().or(new NaturalKey<>(element));
		}

		// Map with YopableEquals as key (YopableEquals has built-in natural ID equals/hashcode methods).
		Map<YopableEquals, T> existing = Maps.uniqueIndex(
			naturalIDQuery.execute(connection, Select.Strategy.EXISTS),
			YopableEquals::new
		);

		// Assign ID on an element if there is a saved element that matched its natural ID
		// ⚠⚠⚠ The equals and hashcode methods from YopableEquals are quite important here ! ⚠⚠⚠
		this.elements.forEach(e -> e.setId(existing.getOrDefault(new YopableEquals(e), e).getId()));
	}

	/**
	 * Update a relationship for the given source elements.
	 * <br><br>
	 * This method will generate and execute :
	 * <ol>
	 *     <li>1 DELETE query to wipe any entry related to the source elements in the relation table</li>
	 *     <li>INSERT queries to create every From → To entry</li>
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

		Relation relation = Relation.relation(elements, join);
		Collection<org.yop.orm.sql.Query> relationsQueries = new ArrayList<>();
		relationsQueries.addAll(relation.toSQLDelete());
		relationsQueries.addAll(relation.toSQLInsert());
		relationsQueries.addAll(relation.toSQLUpdate());
		for (org.yop.orm.sql.Query query : relationsQueries) {
			Executor.executeQuery(connection, query);
		}
	}

	/**
	 * Generate a list of SQL Queries that will effectively do the upsert request.
	 * @return the Upsert queries for the current Upsert
	 */
	private List<SimpleQuery<T>> toSQL() {
		List<SimpleQuery<T>> queries = new ArrayList<>();

		for (T element : this.elements) {
			queries.add(
				element.getId() == null ? this.toSQLInsert(element) : this.toSQLUpdate(element)
			);
		}

		return queries;
	}

	/**
	 * Generate a SQL INSERT query for the given element
	 * @param element the element whose data to use
	 * @return the generated Query
	 */
	protected SimpleQuery toSQLInsert(T element) {
		Parameters parameters = this.values(element);
		List<String> columns = parameters.stream().map(Parameters.Parameter::getName).collect(Collectors.toList());
		List<String> values = parameters.stream().map(Parameters.Parameter::toSQLValue).collect(Collectors.toList());

		String sql = MessageFormat.format(
			INSERT,
			this.getTableName(),
			Joiner.on(", ").join(columns),
			Joiner.on(", ").join(values)
		);

		parameters.removeIf(Parameters.Parameter::isSequence);
		SimpleQuery<T> query = new SimpleQuery<>(sql, Query.Type.INSERT, parameters, element);
		query.askGeneratedKeys(true, element.getClass());
		return query;
	}

	/**
	 * Generate a SQL UPDATE query for the given element
	 * @param element the element whose data to use
	 * @return the generated Query
	 */
	private SimpleQuery<T> toSQLUpdate(T element) {
		Parameters parameters = this.values(element);

		// UPDATE query : ID column must be set last (WHERE clause, not VALUES)
		Parameters.Parameter idParameter = null;
		for (Parameters.Parameter parameter : parameters) {
			if (ORMUtil.getIdColumn(element.getClass()).equals(parameter.getName())) {
				idParameter = parameter;
				break;
			}
		}
		parameters.remove(idParameter);

		Collection<String> values = parameters.stream().map(p -> p.getName() + "=?").collect(Collectors.toList());

		String whereClause = element.getIdColumn() + " = ? ";
		String sql = MessageFormat.format(
			UPDATE,
			this.getTableName(),
			Joiner.on(',').join(values),
			whereClause
		);

		// Set the ID parameter back, at last position.
		parameters.add(idParameter);
		return new SimpleQuery<>(sql, Query.Type.UPDATE, parameters, element);
	}

	/**
	 * Get the table name for the current context (read {@link Table} annotation.
	 * @return the table name for the current context
	 */
	protected String getTableName() {
		return ORMUtil.getTableName(this.target);
	}

	/**
	 * Find all the columns and values to set.
	 * <br>
	 * A sequence parameter might be set if specified !
	 * @param element the element to read for its values
	 * @return the columns to select
	 */
	protected Parameters values(T element) {
		List<Field> fields = Reflection.getFields(this.target, Column.class);
		Field idField = ORMUtil.getIdField(this.target);
		Parameters parameters = new Parameters();
		for (Field field : fields) {
			if(field.equals(idField)) {
				setIdField(field, element, parameters);
				continue;
			}
			parameters.addParameter(
				field.getAnnotation(Column.class).name(),
				ORMUtil.readField(field, element),
				field
			);
		}

		return parameters;
	}

	/**
	 * Add the ID field value to the query parameters. <br>
	 * Check if the field is a sequence.
	 * @param idField    the id field
	 * @param element    the element where to read the field
	 * @param parameters the query parameters.
	 */
	private static void setIdField(Field idField, Yopable element, Parameters parameters) {
		Object value = getUpsertIdValue(element);

		if(value != null) {
			boolean isSequence = ORMUtil.useSequence()
			&& !ORMUtil.readSequence(idField).isEmpty();

			if (isSequence) {
				parameters.addSequenceParameter(element.getIdColumn(), value);
			} else {
				parameters.addParameter(element.getIdColumn(), value, idField);
			}
		}
	}

	/**
	 * Get the ID that should be in the SQL insert/update query for the given element.
	 * <ol>
	 *     <li>element has an ID field set → return the value of the ID field (UPDATE)</li>
	 *     <li>autoincrement is set to false and id is null → mapping exception</li>
	 *     <li>sequence name is set → sequence name + .nextval</li>
	 *     <li>null (→ i.e. do not put me in the insert query)</li>
	 * </ol>
	 * @param element the element to check
	 * @param <T> the element type
	 * @return the id value to set in the query
	 * @throws YopMappingException invalid @Id mapping ←→ ID value
	 */
	private static <T extends Yopable> Object getUpsertIdValue(T element) {
		if(element.getId() != null) {
			return element.getId();
		}
		Field idField = ORMUtil.getIdField(element.getClass());
		if(idField.getAnnotation(Id.class) != null && !idField.getAnnotation(Id.class).autoincrement()) {
			throw new YopMappingException("Element [" + element + "] has no ID and autoincrement is set to false !");
		}

		if(ORMUtil.useSequence()
		&& !ORMUtil.readSequence(idField).isEmpty()) {
			return ORMUtil.readSequence(idField) + ".nextval";
		}
		return null;
	}

	/**
	 * SQL query + parameters aggregation.
	 */
	protected static class SimpleQuery<T extends Yopable> extends org.yop.orm.sql.SimpleQuery {
		private SimpleQuery(String sql, Type type, Parameters parameters, T element) {
			super(sql, type, parameters);
			this.elements.add(element);
			this.target = element == null ? null : element.getClass();
		}

		@SuppressWarnings("unchecked")
		public T getElement() {
			return this.elements.isEmpty() ? null : (T) this.elements.iterator().next();
		}
	}
}
