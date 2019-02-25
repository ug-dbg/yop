package org.yop.orm.query;

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
import org.yop.orm.sql.Config;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Query;
import org.yop.orm.sql.SQLPart;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
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
public class Upsert<T extends Yopable> extends SQLRequest<Upsert<T>, T> implements JsonAble {

	private static final Logger logger = LoggerFactory.getLogger(Upsert.class);

	/** Elements to save/update */
	protected final Yopables<T> elements = new Yopables<>(this.joins);

	private final List<Field> targetFields = new ArrayList<>();

	/** If set to true, any insert will do a preliminary SELECT query to find any entry whose natural key matches */
	protected boolean checkNaturalID = false;

	/** If set to true, {@link #checkNaturalID} will be propagated when using {@link #subUpsert(IJoin, Yopable)} */
	protected boolean propagateCheckNaturalID = false;

	/**
	 * Protected constructor, please use {@link #from(Class)}
	 * @param target the target class
	 */
	protected Upsert(Class<T> target) {
		super(Context.root(target));
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
		Field field = join.getField(this.getTarget());
		Object children = Reflection.readField(field, on);
		if(children == null) {
			return null;
		}

		Class<U> target = join.getTarget(field);
		boolean naturalKey = ! ORMUtil.getNaturalKeyFields(join.getTarget(field)).isEmpty();
		if (children instanceof Collection) {
			if(! ((Collection) children).isEmpty()) {
				return new Upsert<>(target)
					.onto((Collection<U>) children)
					.checkNaturalID(naturalKey && this.checkNaturalID, this.propagateCheckNaturalID);
			}
			return null;
		} else if (children instanceof Yopable) {
			return new Upsert<>(target)
				.onto((U) children)
				.checkNaturalID(naturalKey && this.checkNaturalID, this.propagateCheckNaturalID);
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
		return this.toJSON( this.context);
	}

	@Override
	public <U extends Yopable> JsonObject toJSON(Context<U> context) {
		JsonObject out = (JsonObject) JsonAble.super.toJSON(context);
		out.addProperty("target", this.getTarget().getCanonicalName());
		return out;
	}

	/**
	 * Create an Upsert query from the given json String representation.
	 * @param json         the Upsert query JSON representation
	 * @param config       the SQL config (sql separator, use batch inserts...)
	 * @param classLoaders the class loaders to use to try to load the target resource
	 * @param <T> the target context type. This should match the one set in the JSON representation of the query !
	 * @return a new Upsert query whose state is set from its JSON representation
	 */
	public static <T extends Yopable> Upsert<T> fromJSON(String json, Config config, ClassLoader... classLoaders) {
		try {
			JsonParser parser = new JsonParser();
			JsonObject selectJSON = (JsonObject) parser.parse(json);
			String targetClassName = selectJSON.getAsJsonPrimitive("target").getAsString();
			Class<T> target = Reflection.forName(targetClassName, classLoaders);
			Upsert<T> upsert = Upsert.from(target);
			upsert.fromJSON(Context.root(upsert.getTarget()), selectJSON, config);
			return upsert;
		} catch (RuntimeException e) {
			throw new YopSerializableQueryException(
				"Could not create query from JSON [" + StringUtils.abbreviate(json, 30) + "]", e
			);
		}
	}

	/**
	 * Check for natural ID before insert. Merge and update existing rows.
	 * <br>
	 * @return the current UPSERT request, for chaining purpose
	 */
	public Upsert<T> checkNaturalID() {
		return this.checkNaturalID(false);
	}

	/**
	 * Check for natural ID before insert. Merge and update existing rows.
	 * <br>
	 * The propagation parameter can be useful with joins :
	 * you might want - or not - to check for natural ID in your joined data. Default is 'false'.
	 * <br>
	 * @param propagate if true, {@link #checkNaturalID} will be propagated to any {@link #subUpsert(IJoin, Yopable)}
	 * @return the current UPSERT request, for chaining purpose
	 */
	public Upsert<T> checkNaturalID(boolean propagate) {
		this.checkNaturalID = true;
		this.propagateCheckNaturalID = propagate;
		return this;
	}

	/**
	 * Check for natural ID before insert. Merge and update existing rows. Explicit value and propagate value.
	 * <br>
	 * @param value     the value for {@link #checkNaturalID}
	 * @param propagate the value for {@link #propagateCheckNaturalID}
	 * @return the current UPSERT request, for chaining purpose
	 */
	protected Upsert<T> checkNaturalID(boolean value, boolean propagate) {
		this.checkNaturalID = value;
		this.propagateCheckNaturalID = propagate;
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
	 * Partial Update.
	 * <br>
	 * Restrict an update operation to the given fields (via their getters).
	 * <br>
	 * This makes no sense on insert operations → it will not make any difference on insert operations.
	 * @param getters the getters of the fields
	 * @return the current UPSERT, for chaining purposes
	 */
	@SafeVarargs
	public final Upsert<T> onFields(Function<T, ?>... getters) {
		for (Function<T, ?> getter : getters) {
			this.targetFields.add(Reflection.findField(this.getTarget(), getter));
		}
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
		for (SimpleQuery<T> query : this.toSQL(connection.config())) {
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
		Select<T> naturalIDQuery = Select.from(this.getTarget());
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
		relationsQueries.addAll(relation.toSQLDelete(connection.config()));
		relationsQueries.addAll(relation.toSQLInsert(connection.config()));
		relationsQueries.addAll(relation.toSQLUpdate(connection.config()));
		for (org.yop.orm.sql.Query query : relationsQueries) {
			Executor.executeQuery(connection, query);
		}
	}

	/**
	 * Generate a list of SQL Queries that will effectively do the upsert request.
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the Upsert queries for the current Upsert
	 */
	private List<SimpleQuery<T>> toSQL(Config config) {
		List<SimpleQuery<T>> queries = new ArrayList<>();

		for (T element : this.elements) {
			queries.add(
				this.toSQL(element, element.getId() == null ? Query.Type.INSERT : Query.Type.UPDATE, config)
			);
		}

		return queries;
	}

	/**
	 * Create a query for an element.
	 * @param element the target element
	 * @param type    the query type ({@link Query.Type#INSERT} or {@link Query.Type#UPDATE})
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return a new {@link SimpleQuery} for the target element
	 */
	protected SimpleQuery<T> toSQL(T element, Query.Type type, Config config) {
		return type == Query.Type.INSERT ? this.toSQLInsert(element, config) : this.toSQLUpdate(element, config);
	}

	/**
	 * Generate a SQL INSERT query for the given element
	 * @param element the element whose data to use
	 * @param config  the SQL config (sql separator, use batch inserts...)
	 * @return the generated Query
	 */
	private SimpleQuery toSQLInsert(T element, Config config) {
		Map<String, SQLPart> valueForColumn = this.valuePerColumn(element, config, true);
		List<String> columns = new ArrayList<>(valueForColumn.keySet());
		List<SQLPart> values = new ArrayList<>(valueForColumn.values());

		SQLPart sql = config.getDialect().insert(this.getTableName(), columns, values);
		SimpleQuery<T> query = new SimpleQuery<>(sql, Query.Type.INSERT, element, config);
		query.askGeneratedKeys(true, element.getClass());
		return query;
	}

	/**
	 * Generate a SQL UPDATE query for the given element
	 * @param element the element whose data to use
	 * @param config  the SQL config (sql separator, use batch inserts...)
	 * @return the generated Query
	 */
	private SimpleQuery<T> toSQLUpdate(T element, Config config) {
		List<SQLPart> values = new ArrayList<>(this.valuePerColumn(element, config, false).values());

		// UPDATE query : ID column must be set last (WHERE clause, not VALUES)
		Field idField = ORMUtil.getIdField(element.getClass());
		String idColumn = ORMUtil.getColumnName(idField);
		SQLPart whereIdColumn = config.getDialect().equals(
			idColumn,
			SQLPart.parameter("idcolumn", element.getId(), idField)
		);

		SQLPart sql = config.getDialect().update(this.getTableName(), values, whereIdColumn);
		return new SimpleQuery<>(sql, Query.Type.UPDATE, element, config);
	}

	/**
	 * Get the table name for the current context (read {@link Table} annotation.
	 * @return the table name for the current context
	 */
	private String getTableName() {
		return ORMUtil.getTableQualifiedName(this.getTarget());
	}

	/**
	 * Find all the columns and values to set.
	 * <br>
	 * <b>
	 *     Here is what we return here :
	 *     <ul>
	 *         <li>insert : columnName → SQLPart[columnName = ?]</li>
	 *         <li>update : columnName → SQLPart[?]</li>
	 *     </ul>
	 * </b>
	 * N.B. For a sequence ID field in an insert query, the column is present if and only if sequences are activated.
	 * @param element the element to read for its values
	 * @param config  the SQL config (sql separator, use batch inserts...)
	 * @param insert  true if values are for an insert query.
	 * @return the columns to select and their SQL part parameter.
	 */
	private Map<String, SQLPart> valuePerColumn(T element, Config config, boolean insert) {
		List<Field> fields = ORMUtil.getFields(this.getTarget(), Column.class);
		Field idField = ORMUtil.getIdField(this.getTarget());
		Map<String, SQLPart> out = new HashMap<>();

		for (Field field : fields) {
			if (! insert && ! this.targetFields.isEmpty() && ! this.targetFields.contains(field)) {
				logger.debug("Partial update : field [{}] is excluded.", Reflection.fieldToString(field));
				continue;
			}

			String columnName = ORMUtil.getColumnName(field);
			Object value = ORMUtil.readField(field, element);
			boolean isSequence = config.useSequences() && !ORMUtil.readSequence(idField, config).isEmpty();

			SQLPart column;
			if(field.equals(idField)) {
				value = getUpsertIdValue(element, config);

				if (insert && isSequence) {
					// ID field, insert, sequence → include this column as sequence nextval.
					column = new SQLPart((String) value);
				} else {
					// ID field, insert, not a sequence → do not include this column : autoincrement.
					continue;
				}

			} else {
				column = SQLPart.parameter(columnName, value, field);
			}

			// Update : columnName → [columnName = ?]
			// Insert → columnName → [?]
			if (! insert) {
				column = config.getDialect().equals(new SQLPart(columnName), column);
			}
			out.put(columnName, column);
		}

		return out;
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
	 * @param config  the SQL config (sql separator, use batch inserts...)
	 * @param <T> the element type
	 * @return the id value to set in the query
	 * @throws YopMappingException invalid @Id mapping ←→ ID value
	 */
	private static <T extends Yopable> Object getUpsertIdValue(T element, Config config) {
		if(element.getId() != null) {
			return element.getId();
		}
		Field idField = ORMUtil.getIdField(element.getClass());
		if(idField.getAnnotation(Id.class) != null && !idField.getAnnotation(Id.class).autoincrement()) {
			throw new YopMappingException("Element [" + element + "] has no ID and autoincrement is set to false !");
		}

		if(config.useSequences()
		&& !ORMUtil.readSequence(idField, config).isEmpty()) {
			return ORMUtil.readSequence(idField, config) + ".nextval";
		}
		return null;
	}

	/**
	 * SQL query + parameters aggregation.
	 */
	protected static class SimpleQuery<T extends Yopable> extends org.yop.orm.sql.SimpleQuery {
		private SimpleQuery(SQLPart sql, Type type, T element, Config config) {
			super(sql, type, config);
			this.elements.add(element);
			this.target = element == null ? null : element.getClass();
		}

		@SuppressWarnings("unchecked")
		public T getElement() {
			return this.elements.isEmpty() ? null : (T) this.elements.iterator().next();
		}
	}
}
