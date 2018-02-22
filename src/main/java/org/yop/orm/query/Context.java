package org.yop.orm.query;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A context on a {@link Where}.
 * Aggregate anything that can be useful to generate SQL portions.
 * @param <T> the context target class
 */
public class Context<T extends Yopable> {

	public static final String SQL_SEPARATOR = "→";
	static final String DOT = ".";

	private Context<? extends Yopable> parent;
	private String relationToParent;

	/** The context target class */
	private Class<T> target;

	/**
	 * Private constructor for root context.
	 * <br>
	 * Please use {@link #to(Class, Field)} or {@link #root(Class)}.
	 * @param target the context target class
	 */
	private Context(Class<T> target) {
		this.parent = null;
		this.relationToParent = "";
		this.target = target;
	}

	/**
	 * Private complete constructor.
	 * <br>
	 * Please use {@link #to(Class, Field)}.
	 * @param parent   the parent context
	 * @param relation the relation name between the contexts
	 * @param target   the context target class
	 */
	private Context(Context<?> parent, String relation, Class<T> target) {
		this.parent = parent;
		this.relationToParent = relation;
		this.target = target;
	}

	/**
	 * Build the context path
	 * @return the fully qualified context path
	 */
	public String getPath() {
		Context<?> parent = this.parent;
		StringBuilder path = new StringBuilder(this.target.getSimpleName());

		if(StringUtils.isNotBlank(this.relationToParent) && parent != null) {
			path.insert(0, this.relationToParent + SQL_SEPARATOR);
			path.insert(0, parent.getPath() + SQL_SEPARATOR);
		}

		return path.toString();
	}

	/**
	 * Get the context target
	 * @return {@link #target}
	 */
	public Class<T> getTarget() {
		return target;
	}

	/**
	 * Get the table name for the current context (read {@link Table} annotation.
	 * @return the table name for the current context
	 */
	public String getTableName() {
		if(this.target.isAnnotationPresent(Table.class)) {
			return this.target.getAnnotation(Table.class).name();
		}
		return this.target.getSimpleName().toUpperCase();
	}

	/**
	 * Get the columns to fetch for the given context (find all @Column annotated fields)
	 * @return the columns to fetch
	 */
	public Set<SQLColumn> getColumns() {
		Set<SQLColumn> columns = new HashSet<>();
		String path = this.getPath();
		for (Field field : Reflection.getFields(this.target, Column.class)) {
			columns.add(new SQLColumn(
				path + DOT + field.getAnnotation(Column.class).name(),
				path + Context.SQL_SEPARATOR + field.getAnnotation(Column.class).name()
			));
		}
		return columns;
	}

	/**
	 * Create the ID alias of the current target and add a prefix.
	 * @param prefix the prefix to use
	 * @return the target class ID alias
	 */
	public String idAlias(String prefix) {
		Field idField = Reflection.get(this.target, this.newInstance().getIdFieldName());
		if(idField != null && idField.isAnnotationPresent(Column.class)) {
			return prefix + DOT + idField.getAnnotation(Column.class).name();
		}
		return prefix + DOT + this.newInstance().getIdFieldName().toUpperCase();
	}

	/**
	 * Create a new instance of the target class.
	 * @return a new instance of T
	 */
	private T newInstance() {
		try {
			return this.target.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(
				"Could not instanciate Yopable class [" + this.target.getName() + "]",
				e
			);
		}
	}

	@Override
	public String toString() {
		return "Context{" + this.getPath() + "}";
	}

	/**
	 * Create a root context from the target class
	 * @param root the target class, as root context
	 * @param <T> the target type
	 * @return a new root context
	 */
	public static <T extends Yopable> Context<T> root(Class<T> root) {
		return new Context<>(root);
	}

	/**
	 * Create a new context, from a parent and to a target class
	 * @param to    the target class
	 * @param using the field to use to get from parent to To
	 * @param <To> the target type
	 * @return the new context
	 */
	@SuppressWarnings("unchecked")
	public <To extends Yopable> Context<To> to(Class<To> to, Field using) {
		return new Context<>(this, using.getName(), to);
	}

	/**
	 * Convenience class to store an SQL column (qualified ID and alias)
	 */
	public static class SQLColumn {
		private String qualifiedId;
		private String alias;

		SQLColumn(String qualifiedId, String alias) {
			this.qualifiedId = qualifiedId;
			this.alias = alias;
		}

		public String toSQL() {
			return this.qualifiedId + " AS " + this.alias;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SQLColumn sqlColumn = (SQLColumn) o;
			return Objects.equals(qualifiedId, sqlColumn.qualifiedId) && Objects.equals(alias, sqlColumn.alias);
		}

		@Override
		public int hashCode() {
			return Objects.hash(qualifiedId, alias);
		}

		@Override
		public String toString() {
			return this.alias;
		}
	}
}
