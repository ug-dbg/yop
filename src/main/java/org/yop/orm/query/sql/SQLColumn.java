package org.yop.orm.query.sql;

import org.yop.orm.annotations.Column;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.SQLPart;
import org.yop.orm.util.ORMUtil;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Convenience class to store an SQL column (table name, qualified column name and alias) for a context.
 * <br>
 * A column has :
 * <ul>
 *     <li>a context : {@link  #context}. See {@link Context#getPath(Config)}. e.g. Book→author→Author</li>
 *     <li>a column name : {@link #name()}, read from the field {@link #field}</li>
 *     <li>a qualified column name : {@link #qualifiedName()}. e.g. : Book→author→Author.NAME</li>
 *     <li>an alias : {@link #alias()}. e.g. : Book→author→Author→NAME</li>
 *     <li>an SQL representation : {@link #toSQL()}. e.g. Book→author→Author.NAME AS "Book→author→Author→NAME"</li>
 * </ul>
 */
class SQLColumn extends SQLPart {

	private final Field field;
	private final Context<?> context;
	private final Config config;

	/**
	 * Private constructor. Please use factory methods.
	 * @param field   the column field
	 * @param context the column context
	 * @param config  the SQL config (separators are read from this)
	 */
	private SQLColumn(Field field, Context<?> context, Config config) {
		super();

		this.field = field;
		this.context = context;
		this.config = config;

		this.sql = this.toSQL();
	}

	/**
	 * Create an SQL column instance for the given field in a context.
	 * @param field   the field
	 * @param context the context
	 * @param config  the SQL config (separators are read from this)
	 * @return a new SQL column instance for the given field
	 */
	@SuppressWarnings("WeakerAccess")
	static SQLColumn column(Field field, Context<?> context, Config config) {
		return new SQLColumn(field, context, config);
	}

	/**
	 * Get the columns for the given context (find all @Column annotated fields)
	 * @param config the SQL config (sql separators are read from this)
	 * @return a Set of SQL columns
	 */
	static Set<SQLColumn> columns(Context<?> context, Config config) {
		Set<SQLColumn> columns = new HashSet<>();
		for (Field field : ORMUtil.getFields(context.getTarget(), Column.class)) {
			columns.add(SQLColumn.column(field, context, config));
		}
		return columns;
	}

	/**
	 * Create an SQL column instance for the id field of the context.
	 * @param context the context. The target class is read from {@link Context#getTarget()}.
	 * @param config  the SQL config (separators are read from this)
	 * @return a new SQL column instance for the given field
	 */
	static SQLColumn id(Context<?> context, Config config) {
		return new SQLColumn(ORMUtil.getIdField(context.getTarget()), context, config);
	}

	/**
	 * Is this SQL column an ID column ?
	 * @return true if {@link #field} is the ID field for {@link Context#getTarget()}.
	 */
	@SuppressWarnings("unchecked")
	boolean isId() {
		return ORMUtil.getIdField((Class) this.field.getDeclaringClass()) == this.field;
	}

	/**
	 * The qualified column declaration for this column.
	 * e.g. Book→author→Author.NAME AS "Book→author→Author→NAME"
	 * @return the SQL for the column selection
	 */
	@SuppressWarnings("WeakerAccess")
	String toSQL() {
		return this.qualified(this.config.dot()) + " AS \"" + this.alias() + "\"";
	}

	/**
	 * Get the table name of the current SQL column
	 * @return {@link ORMUtil#getTableName(Class)}
	 */
	String tableName() {
		return ORMUtil.getTableName(this.context.getTarget());
	}

	/**
	 * Get the context path for the current SQL column.
	 * @return {@link Context#getPath(Config)}
	 */
	private String tablePrefix() {
		return this.context.getPath(this.config);
	}

	/**
	 * Get the qualified name for the current SQL Column.
	 * @return the column name in context, e.g. Book→author→Author.NAME
	 */
	String qualifiedName() {
		return this.qualified(this.config.dot());
	}

	/**
	 * Qualify the current column name with the given separator.
	 * @param separator the separator to use.
	 * @return {@link #tablePrefix()} + separator + {@link #name()}
	 */
	private String qualified(String separator) {
		return this.tablePrefix() + separator + this.name();
	}

	/**
	 * Get the current column alias.
	 * @return {@link #qualified(String)} for the config {@link Config#sqlSeparator()}
	 */
	private String alias() {
		return this.qualified(this.config.sqlSeparator());
	}

	/**
	 * Get the current SQL column raw name.
	 * @return {@link ORMUtil#getColumnName(Field)}
	 */
	private String name() {
		return ORMUtil.getColumnName(this.field);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || this.getClass() != o.getClass()) return false;
		SQLColumn sqlColumn = (SQLColumn) o;
		return Objects.equals(this.field, sqlColumn.field)
			&& Objects.equals(this.tablePrefix(), sqlColumn.tablePrefix());
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.field, this.tablePrefix());
	}
}