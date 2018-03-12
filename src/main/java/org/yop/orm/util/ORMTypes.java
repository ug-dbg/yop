package org.yop.orm.util;

import com.google.common.primitives.Primitives;
import org.apache.commons.lang.StringUtils;
import org.yop.orm.gen.Column;
import org.yop.orm.gen.ForeignKey;
import org.yop.orm.gen.Table;

import java.sql.Time;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DBMS specificities.
 * <br>
 * This is pretty raw/patchy. This was written to prepare unit tests context.
 * <br><br>
 * This is required for schema generation and should only be used for this purpose only !
  * <br>
  * Yop tries to generate some very basic SQL CRUD queries that does not rely on an SQL dialect.
 */
public class ORMTypes extends HashMap<Class<?>, String> {

	protected static final String CREATE = " CREATE TABLE {0} ({1}) ";
	private static final String PK = " CONSTRAINT {0} PRIMARY KEY ({1}) ";
	private static final String FK = " CONSTRAINT {0} FOREIGN KEY ({1}) REFERENCES {2}({3}) ON DELETE CASCADE ";

	/** The default SQL type */
	private String defaultType;

	/**
	 * @return the default SQL type → {@link #defaultType}
	 */
	public String getDefault() {
		return this.defaultType;
	}

	/**
	 * Get the SQL type for the given Java type.
	 * @param type the java type
	 * @return the SQL type
	 */
	public String getForType(Class<?> type) {
		Class<?> wrapped = Primitives.wrap(type);

		for (Entry<Class<?>, String> entry : this.entrySet()) {
			if(wrapped.isAssignableFrom(entry.getKey())) return entry.getValue();
		}

		return this.getDefault();
	}

	/**
	 * Default constructor, with default type.
	 * Add basic types matching.
	 * @param defaultType the {@link #defaultType} to set.
	 */
	protected ORMTypes(String defaultType) {
		this.defaultType = defaultType;
		this.put(String.class,     "VARCHAR");
		this.put(Character.class,  "VARCHAR");

		this.put(Integer.class, "INTEGER");
		this.put(Long.class,    "BIGINT");
		this.put(Short.class,   "INTEGER");
		this.put(Byte.class,    "INTEGER");

		this.put(Float.class,  "REAL");
		this.put(Double.class, "REAL");

		this.put(Date.class,          "DATE");
		this.put(Calendar.class,      "TIMESTAMP");
		this.put(Instant.class,       "TIMESTAMP");
		this.put(LocalDate.class,     "TIMESTAMP");
		this.put(LocalDateTime.class, "TIMESTAMP");

		this.put(Time.class,               "TIMESTAMP");
		this.put(java.sql.Date.class,      "DATE");
		this.put(java.sql.Timestamp.class, "TIMESTAMP");
	}

	/**
	 * Generate the SQL CREATE query for the given table.
	 * @param table the table model
	 * @return the SQL CREATE query.
	 */
	public String toSQL(Table table) {
		Collection<String> elements = new ArrayList<>();
		elements.addAll(table.getColumns().stream().sorted().map(Column::toString).collect(Collectors.toList()));
		elements.addAll(table.getColumns().stream().map(c -> this.toSQLPK(table, c)).collect(Collectors.toList()));
		elements.addAll(table.getColumns().stream().map(this::toSQLFK).collect(Collectors.toList()));

		return MessageFormat.format(
			CREATE,
			table.qualifiedName(),
			MessageUtil.join(", ", elements)
		);
	}

	/**
	 * Generate the SQL CREATE query portion for a given column
	 * @param column the column model
	 * @return the SQL CREATE query portion for the given column
	 */
	public String toSQL(Column column) {
		boolean autoincrement = column.getPk() != null && column.getPk().isAutoincrement();

		return column.getName()
			+ " " + this.type(column)
			+ (column.isNotNull() ? " NOT NULL " : "")
			+ (autoincrement ? this.autoIncrementKeyWord() : "");
	}

	/**
	 * An extra list of sql queries to be executed for this table.
	 * <br>
	 * Default : do nothing :-)
	 * @return an ordered list of queries to execute so the table is entirely operationnal.
	 */
	public List<String> otherSQL(Table table) {
		return new ArrayList<>(0);
	}

	/**
	 * Changing the {@link #type(Column)} method might be required (hello Postgres!)
	 * @return the auto increment keyword.
	 */
	protected String autoIncrementKeyWord() {
		return " AUTO_INCREMENT ";
	}

	/**
	 * Get the column SQL type, with length if applicable.
	 * @param column the input column
	 * @return the SQL type
	 */
	protected String type(Column column) {
		String type = column.getType();
		return type + (StringUtils.equals("VARCHAR", type) ? "(" + column.getLength() + ")" : "");
	}

	/**
	 * Generate the SQL CREATE query primary key portion
	 * @param table  the table model
	 * @param column the column model
	 * @return the PK query portion. Empty if the column is not a PK.
	 */
	protected String toSQLPK(Table table, Column column) {
		if(column.getPk() == null) {
			return "";
		}
		return MessageFormat.format(PK, table.qualifiedName() + "_PK", column.getName());
	}

	/**
	 * Generate the SQL CREATE query foreign key portion
	 * @param column the column model
	 * @return the FK query portion. Empty if the column is not a FK.
	 */
	protected String toSQLFK(Column column) {
		ForeignKey fk = column.getFk();
		if(fk == null) {
			return "";
		}
		return MessageFormat.format(
			FK,
			fk.getName(),
			column.getName(),
			fk.getReferencedTable(),
			fk.getReferencedColumn()
		);
	}

	/**
	 * Generate the SQL CREATE query natural ID portion
	 * @param naturalKeys the natural key columns
	 * @return the natural key query portion. Empty string for now.
	 */
	public String toSQLNK(Set<Column> naturalKeys) {
		return "";
	}
}
