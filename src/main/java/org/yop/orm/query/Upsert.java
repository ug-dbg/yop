package org.yop.orm.query;

import com.google.common.base.Joiner;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.Table;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Upsert : save or update instances of T to the database.
 *
 * @param <T> the type to upsert.
 */
public class Upsert<T extends Yopable> {

	private static final String INSERT = " INSERT INTO {0} ({1}) VALUES ({2}) ";

	/** Target class */
	private Class<T> target;

	/** Elements to save/update */
	private Collection<T> elements = new ArrayList<>();

	/** Join clauses */
	private Collection<IJoin<T, ? extends Yopable>> joins = new ArrayList<>();

	/**
	 * Private constructor, please use {@link #from(Class)}
	 * @param target the target class
	 */
	private Upsert(Class<T> target) {
		this.target = target;
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
	 * Fetch the whole data graph. Stop on transient fields.
	 * <br>
	 * <b>⚠⚠⚠ There must be no cycle in the data graph model ! ⚠⚠⚠</b>
	 * @return the current SELECT request, for chaining purpose
	 */
	public Upsert<T> joinAll() {
		throw new UnsupportedOperationException("Not implemented yet !");
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
	 * @param connection the connection to use.
	 */
	public void execute(Connection connection) {
		for (Query query : this.toSQL()) {
			Executor.executeQuery(connection, query.sql, query.parameters);
		}
	}

	/**
	 * Generate a list of SQL Queries that will effectively do the upsert request.
	 * @return the Upsert queries for the current Upsert
	 */
	private List<Query> toSQL() {
		List<Query> queries = new ArrayList<>();

		for (T element : this.elements) {
			Parameters parameters = this.values(element);
			List<String> columns = parameters.stream().map(Parameters.Parameter::getName).collect(Collectors.toList());
			List<String> values = parameters.stream().map(p -> "?").collect(Collectors.toList());

			String sql = MessageFormat.format(
				INSERT,
				this.getTableName(),
				Joiner.on(", ").join(columns),
				Joiner.on(", ").join(values)
			);

			queries.add(new Query(sql, parameters));
		}

		return queries;
	}

	/**
	 * Get the table name for the current context (read {@link Table} annotation.
	 * @return the table name for the current context
	 */
	private String getTableName() {
		if(this.target.isAnnotationPresent(Table.class)) {
			return this.target.getAnnotation(Table.class).name();
		}
		return this.target.getSimpleName().toUpperCase();
	}

	/**
	 * Find all the columns and values to set
	 * @return the columns to select
	 */
	private Parameters values(T element) {
		List<Field> fields = Reflection.getFields(this.target, Column.class);
		Field idField = Yopable.getIdField(this.target);
		Parameters parameters = new Parameters();
		for (Field field : fields) {
			if(field.equals(idField)) {
				String value = getInsertIdValue(element);
				if(value != null) {
					parameters.addParameter(element.getIdColumn(), value);
				}
				continue;
			}
			try {
				parameters.addParameter(field.getAnnotation(Column.class).name(), this.getFieldValue(field, element));
			} catch (IllegalAccessException e) {
				throw new YopMappingException(
					"Could not read [" + field.getGenericType() + "#" + field.getName() + "] on [" + element + "]"
				);
			}
		}

		return parameters;
	}

	/**
	 * Read the value of a field so it can be inserted in an SQL query.
	 * This method can handle the 'enum' case, i.e. read the strategy and return the name() or ordinal().
	 * @param field   the field to read
	 * @param element the element holding the field
	 * @return the field value
	 * @throws IllegalAccessException could not access the filed
	 * @throws YopMappingException    invalid enum strategy on the field. What did you do bro ?
	 */
	private Object getFieldValue(Field field, T element) throws IllegalAccessException {
		if(field.getType().isEnum()) {
			Column.EnumStrategy strategy = field.getAnnotation(Column.class).enum_strategy();
			switch (strategy) {
				case NAME:    return ((Enum)field.get(element)).name();
				case ORDINAL: return ((Enum)field.get(element)).ordinal();
				default: throw new YopMappingException("Unknown enum strategy [" + strategy.name() + "] !");
			}
		}
		return field.get(element);
	}

	/**
	 * Get the ID that should be in the SQL insert query for the given element.
	 * <ol>
	 *     <li>element has an ID field set → return the value of the ID field</li>
	 *     <li>autoincrement is set to false and id is null → mapping exception</li>
	 *     <li>sequence name is set → sequence name + .nextval</li>
	 *     <li>null (→ i.e. do not put me in the insert query)</li>
	 * </ol>
	 * @param element the element to check
	 * @param <T> the element type
	 * @return the id value to set in the query
	 * @throws YopMappingException invalid @Id mapping ←→ ID value
	 */
	private static <T extends Yopable> String getInsertIdValue(T element) {
		if(element.getId() != null) {
			return String.valueOf(element.getId());
		}
		Field idField = Yopable.getIdField(element.getClass());
		if(idField.getAnnotation(Id.class) != null && !idField.getAnnotation(Id.class).autoincrement()) {
			throw new YopMappingException("Element [" + element + "] has no ID and autoincrement is set to false !");
		}
		if(idField.isAnnotationPresent(Id.class) && !idField.getAnnotation(Id.class).sequence().isEmpty()) {
			return idField.getAnnotation(Id.class).sequence() + ".nextval";
		}
		return null;
	}

	/**
	 * SQL query + parameters aggregation.
	 */
	private static class Query {
		private String sql;
		private Parameters parameters;

		private Query(String sql, Parameters parameters) {
			this.sql = sql;
			this.parameters = parameters;
		}
	}
}
