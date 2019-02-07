package org.yop.orm.query;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Config;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A context is basically a path in an oriented acyclic graph.
 * <br><br>
 * It has a target class, a parent context (that can be null) and a relation name to the parent context.
 * <br>
 * The path to a current can then be recursively built (see {@link #getPath(Config)}) :
 * <i>
 * RootClass→relationA→IntermediaryClass→relationB→currentContextClass
 * </i>
 * @param <T> the context target class
 */
public class Context<T extends Yopable> {

	/** Parent context. If null, this context is the root context. */
	private final Context<? extends Yopable> parent;

	/** Relation (field) name to the parent context. If null, this context is the root context. */
	private final String relationToParent;

	/** The context target class */
	private final Class<T> target;

	private String targetAliasSuffix = "";

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
	 * Copy the current context.
	 * <b>⚠ This is a shallow copy : the fields of the returned copy and the current instance are shared ! ⚠</b>
	 * @param targetAliasSuffix a target alias suffix. Can be null or empty
	 * @return a shallow copy of the current context, with the given alias suffix
	 */
	public Context<T> copy(String targetAliasSuffix) {
		Context<T> copy = new Context<>(this.parent, this.relationToParent, this.target);
		copy.targetAliasSuffix = StringUtils.isBlank(targetAliasSuffix) ? "" : targetAliasSuffix;
		return copy;
	}

	/**
	 * Build the context path recursively.
	 * <br>
	 * The resulting path should look like this :
	 * <b>RootClass→relationA→IntermediaryClass→relationB→currentContextClass</b>
	 * <br><br>
	 * A context that is not root has a reference to the parent context, using a relation name.
	 * So we can recursively build the path of the given context.
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the fully qualified context path
	 */
	public String getPath(Config config) {
		Context<?> parent = this.parent;
		StringBuilder path = new StringBuilder(ORMUtil.getTargetName(this.target)).append(this.targetAliasSuffix);

		if(StringUtils.isNotBlank(this.relationToParent) && parent != null) {
			path.insert(0, this.relationToParent + config.sqlSeparator());
			path.insert(0, parent.getPath(config) + config.sqlSeparator());
		}

		return path.toString();
	}

	/**
	 * Build the context path recursively, for a given field.
	 * <br>
	 * The resulting path should look like this :
	 * <b>RootClass→relationA→IntermediaryClass→relationB→currentContextClass.FIELD_COLUMN_NAME</b>
	 * @param columnField the considered field for the path building. Must be a @Column field.
	 * @param config      the SQL config (sql separator, use batch inserts...)
	 * @return the fully qualified context path for the given field
	 */
	public String getPath(Field columnField, Config config) {
		return this.getPath(config) + config.dot() + ORMUtil.getColumnName(columnField);
	}

	/**
	 * Get the context target
	 * @return {@link #target}
	 */
	public Class<T> getTarget() {
		return this.target;
	}

	/**
	 * Get the table name for the current context (read {@link Table} annotation.
	 * @return the table name for the current context
	 */
	public String getTableName() {
		return ORMUtil.getTableQualifiedName(this.target);
	}

	/**
	 * Get the columns to fetch for the given context (find all @Column annotated fields)
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the columns to fetch
	 */
	public Set<SQLColumn> getColumns(Config config) {
		Set<SQLColumn> columns = new HashSet<>();
		String path = this.getPath(config);
		Field idField = ORMUtil.getIdField(this.target);
		for (Field field : ORMUtil.getFields(this.target, Column.class)) {
			String name = field.getAnnotation(Column.class).name();
			columns.add(new SQLColumn(
				name,
				path + config.dot() + name,
				path + config.sqlSeparator() + name,
				field.equals(idField)
			));
		}
		return columns;
	}

	/**
	 * Create the context table alias of the current target
	 * @return the target table alias
	 */
	public String tableAlias() {
		return ORMUtil.getTargetName(this.target) + this.targetAliasSuffix;
	}

	/**
	 * Create the ID alias of the current target and add a prefix.
	 * @param prefix the prefix to use
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the target class ID alias
	 */
	public String idAlias(String prefix, Config config) {
		T newInstance = Reflection.newInstanceNoArgs(this.target);
		Field idField = Reflection.get(this.target, newInstance.getIdFieldName());
		if(idField != null && idField.isAnnotationPresent(Column.class)) {
			return prefix + config.dot() + idField.getAnnotation(Column.class).name();
		}
		return prefix + config.dot() + newInstance.getIdFieldName().toUpperCase();
	}

	/**
	 * 2 contexts are considered the same if their {@link #getPath(Config)} returns the same value.
	 * <br><br>
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		return obj != null
			&& obj instanceof Context
			&& StringUtils.equals(this.getPath(Config.DEFAULT), ((Context) obj).getPath(Config.DEFAULT));
	}

	/**
	 * 2 contexts are considered the same if their {@link #getPath(Config)} returns the same value.
	 * <br>
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return this.getPath(Config.DEFAULT).hashCode();
	}

	@Override
	public String toString() {
		return "Context{" + this.getPath(Config.DEFAULT) + "}";
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
	 * Give me the root context from my context path !
	 * @return the root context of the current context, i.e. the first context whose parent is null
	 */
	public Context<? extends Yopable> root() {
		Context<?> context = this;
		while (context.parent != null) {
			context = context.parent;
		}
		return context;
	}

	/**
	 * A Fake context, where you can set a predetermined path.
	 * <br>
	 * I am not very proud of this.
	 * @param <T> the target type
	 */
	static class FakeContext<T extends Yopable> extends Context<T> {
		private final String fakePath;

		FakeContext(Context<T> context, String fakePath) {
			super(context.getTarget());
			this.fakePath = fakePath;
		}

		@Override
		public String getPath(Config config) {
			return this.fakePath;
		}
	}

	/**
	 * Convenience class to store an SQL column (qualified ID and alias)
	 */
	static class SQLColumn {
		private final String name;
		private final String qualifiedId;
		private final String alias;

		/** Is this column the ID of a Yopable class ? */
		private final boolean id;

		SQLColumn(String name, String qualifiedId, String alias, boolean id) {
			this.name = name;
			this.qualifiedId = qualifiedId;
			this.alias = alias;
			this.id = id;
		}

		public String toSQL() {
			return this.qualifiedId + " AS \"" + this.alias + "\"";
		}

		public String getName() {
			return this.name;
		}

		public String getQualifiedId() {
			return this.qualifiedId;
		}

		/**
		 * @return {@link #id}
		 */
		public boolean isId() {
			return this.id;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || this.getClass() != o.getClass()) return false;
			SQLColumn sqlColumn = (SQLColumn) o;
			return Objects.equals(this.qualifiedId, sqlColumn.qualifiedId)
				&& Objects.equals(this.alias, sqlColumn.alias);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.qualifiedId, this.alias);
		}

		@Override
		public String toString() {
			return this.alias;
		}
	}
}
