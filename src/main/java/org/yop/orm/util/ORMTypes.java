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

	/** PostGres types */
	public static final ORMTypes POSTGRES = new ORMTypes("VARCHAR") {
		{
			this.put(Boolean.class, "boolean");
		}

		@Override
		protected String autoIncrementKeyWord() {
			return " SERIAL ";
		}

		@Override
		protected String type(Column column) {
			return column.getPk() == null ? super.type(column) : "";
		}
	};

	/** Oracle types. */
	public static final ORMTypes ORACLE = new ORMTypes("VARCHAR");

	/** MS-SQL types. */
	public static final ORMTypes MSSQL = new ORMTypes("NVARCHAR") {
		{
			this.put(String.class,     "NVARCHAR");
			this.put(Character.class,  "NVARCHAR");

			this.put(Integer.class, "INTEGER");
			this.put(Long.class,    "BIGINT");
			this.put(Short.class,   "INTEGER");
			this.put(Byte.class,    "INTEGER");

			this.put(Float.class,  "REAL");
			this.put(Double.class, "REAL");
			this.put(Date.class,          "DATE");
			this.put(Calendar.class,      "DATETIME");
			this.put(Instant.class,       "DATETIME");
			this.put(LocalDate.class,     "DATE");
			this.put(LocalDateTime.class, "DATETIME");

			this.put(Time.class,               "DATETIME");
			this.put(java.sql.Date.class,      "DATE");
			this.put(java.sql.Timestamp.class, "DATETIME");
		}

		@Override
		protected String type(Column column) {
			String type = column.getType();
			return type + (StringUtils.endsWith(type, "VARCHAR") ? "(" + column.getLength() + ")" : "");
		}

		@Override
		protected String autoIncrementKeyWord() {
			return " IDENTITY (1,1) ";
		}

		@Override
		public String toSQL(Table table) {
			Collection<String> elements = new ArrayList<>();
			elements.addAll(table.getColumns().stream().sorted().map(Column::toString).collect(Collectors.toList()));
			elements.addAll(table.getColumns().stream().map(c -> this.toSQLPK(table, c)).collect(Collectors.toList()));

			Set<String> referenced = new HashSet<>();
			for (Column column : table.getColumns()) {
				if(column.getFk() != null) {
					String reference = column.getFk().getReferencedTable() + "." + column.getFk().getReferencedTable();
					if(referenced.contains(reference)) continue;

					elements.add(this.toSQLFK(column));
					referenced.add(reference);
				}
			}

			return MessageFormat.format(
				CREATE,
				table.qualifiedName(),
				MessageUtil.join(", ", elements)
			);
		}
	};

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
			elements.addAll(table.getColumns().stream().sorted().map(Column::toString).collect(Collectors.toList()));
			elements.addAll(table.getColumns().stream().map(this::toSQLFK).collect(Collectors.toList()));

			return MessageFormat.format(
				CREATE,
				table.qualifiedName(),
				MessageUtil.join(", ", elements)
			);
		}
	};
}
