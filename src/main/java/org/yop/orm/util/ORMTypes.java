package org.yop.orm.util;

import com.google.common.primitives.Primitives;
import org.yop.orm.gen.Column;
import org.yop.orm.gen.ForeignKey;
import org.yop.orm.gen.Table;

import java.sql.Time;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DBMS specificities.
 * <br>
 * This is pretty raw. This was written to prepare unit tests context.
 */
public class ORMTypes extends HashMap<Class<?>, String> {

	private static final String CREATE = " CREATE TABLE {0} ({1}) ";
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
	private ORMTypes(String defaultType) {
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
		elements.addAll(table.getColumns().stream().map(Column::toString).collect(Collectors.toList()));
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
			+ " " + column.getType()
			+ (column.isNotNull() ? " NOT NULL " : "")
			+ (autoincrement ? " AUTO_INCREMENT " : "");
	}

	/**
	 * Generate the SQL CREATE query primary key portion
	 * @param table  the table model
	 * @param column the column model
	 * @return the PK query portion. Empty if the column is not a PK.
	 */
	private String toSQLPK(Table table, Column column) {
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
	String toSQLFK(Column column) {
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

	/** Default Types. Should work with MySQL. At least I hope. */
	public static final ORMTypes DEFAULT = new ORMTypes("VARCHAR");

	/** SQLite types */
	public static final ORMTypes SQLITE = new ORMTypes("TEXT") {
		{
			this.put(String.class,     "TEXT");
			this.put(Character.class,  "TEXT");

			this.put(Integer.class, "INTEGER");
			this.put(Long.class,    "INTEGER");
			this.put(Short.class,   "INTEGER");
			this.put(Byte.class,    "INTEGER");

			this.put(Float.class,  "REAL");
			this.put(Double.class, "REAL");
		}

		@Override
		public String toSQL(Column column) {
			return column.getName()
				+ " " + column.getType()
				+ (column.isNotNull() ? " NOT NULL " : "")
				+ (column.getPk() != null ? " PRIMARY KEY " : "");
		}

		@Override
		public String toSQL(Table table) {
			Collection<String> elements = new ArrayList<>();
			elements.addAll(table.getColumns().stream().map(Column::toString).collect(Collectors.toList()));
			elements.addAll(table.getColumns().stream().map(this::toSQLFK).collect(Collectors.toList()));

			return MessageFormat.format(
				CREATE,
				table.qualifiedName(),
				MessageUtil.join(", ", elements)
			);
		}
	};
}
