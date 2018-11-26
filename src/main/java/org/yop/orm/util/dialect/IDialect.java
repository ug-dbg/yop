package org.yop.orm.util.dialect;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.gen.Column;
import org.yop.orm.gen.ForeignKey;
import org.yop.orm.gen.Table;
import org.yop.orm.util.MessageUtil;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Interface of an SQL dialect.
 * <br>
 * A dialect must :
 * <ul>
 *     <li>know the SQL type for a Java type</li>
 *     <li>generate SQL for SQL data structures (Column, Table)</li>
 *     <li>generate SQL for SQL requests (Create, Drop)</li>
 * </ul>
 * NB : Yop tries to generate some very basic SQL CRUD queries that does not rely on an SQL dialect.
 */
public interface IDialect {

	String CREATE = " CREATE TABLE {0} ({1}) ";
	String DROP = " DROP TABLE {0} ";
	String PK = " CONSTRAINT {0} PRIMARY KEY ({1}) ";
	String FK = " CONSTRAINT {0} FOREIGN KEY ({1}) REFERENCES {2}({3}) ON DELETE CASCADE ";
	String NK = " CONSTRAINT {0} UNIQUE ({1}) ";

	/**
	 * @return the default SQL type for this dialect.
	 */
	String getDefault();

	/**
	 * Set the SQL type for the given class.
	 * @param clazz the java type class
	 * @param type  the SQL type
	 */
	void setForType(Class clazz, String type);

	/**
	 * Get the SQL type for the given Java type.
	 * @param type the java type
	 * @return the SQL type
	 */
	String getForType(Class<?> type);

	/**
	 * Create a default dialect implementation, with "VARCHAR" default type.
	 * @return a new instance of {@link Dialect}.
	 */
	static IDialect defaultDialect() {
		return new Dialect("VARCHAR") {};
	}

	/**
	 * Generate the SQL CREATE query for the given table.
	 * @param table the table model
	 * @return the SQL CREATE query.
	 */
	default String toSQL(Table table) {
		Collection<String> elements = new ArrayList<>();
		elements.addAll(table.getColumns().stream().sorted().map(Column::toSQL).collect(Collectors.toList()));
		elements.addAll(table.getColumns().stream().map(c -> this.toSQLPK(table, c)).collect(Collectors.toList()));
		elements.addAll(table.getColumns().stream().map(this::toSQLFK).collect(Collectors.toList()));
		elements.addAll(this.toSQLNK(table));

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
	default String toSQL(Column column) {
		boolean autoincrement = column.getPk() != null && column.getPk().isAutoincrement();

		return column.getName()
			+ " " + this.type(column)
			+ (column.isNotNull() ? " NOT NULL " : "")
			+ (autoincrement ? this.autoIncrementKeyWord() : "");
	}

	/**
	 * An extra list of sql queries to be executed for a table.
	 * <br>
	 * Default : do nothing :-)
	 * @param table the considered table
	 * @return an ordered list of queries to execute so the table is entirely operational.
	 */
	default List<String> otherSQL(Table table) {
		return new ArrayList<>(0);
	}

	/**
	 * Changing the {@link #type(Column)} method might be required (hello Postgres!)
	 * @return the auto increment keyword.
	 */
	default String autoIncrementKeyWord() {
		return " AUTO_INCREMENT ";
	}

	/**
	 * Get the column SQL type, with length if applicable.
	 * @param column the input column
	 * @return the SQL type
	 */
	default String type(Column column) {
		String type = column.getType();
		return type + (StringUtils.equals("VARCHAR", type) ? "(" + column.getLength() + ")" : "");
	}

	/**
	 * Generate the SQL CREATE query primary key portion
	 * @param table  the table model
	 * @param column the column model
	 * @return the PK query portion. Empty if the column is not a PK.
	 */
	default String toSQLPK(Table table, Column column) {
		if(column.getPk() == null) {
			return "";
		}
		return MessageFormat.format(PK, table.name() + "_PK", column.getName());
	}

	/**
	 * Generate the SQL CREATE query foreign key portion
	 * @param column the column model
	 * @return the FK query portion. Empty if the column is not a FK.
	 */
	default String toSQLFK(Column column) {
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
	 * Generate the SQL CREATE query natural ID portion.
	 * @param table the table
	 * @return the natural key query portions. (There should be 1 or 0)
	 */
	default Collection<String> toSQLNK(Table table) {
		Set<String> nkColumnNames = table.getColumns()
			.stream()
			.filter(Column::isNaturalKey)
			.map(Column::getName)
			.collect(Collectors.toSet());

		if(nkColumnNames.isEmpty()) {
			return new ArrayList<>(0);
		}

		String constraintName = table.name() + "_NK";
		String constraint = MessageFormat.format(NK, constraintName, MessageUtil.join(",", nkColumnNames));
		return Collections.singletonList(constraint);
	}

	/**
	 * Generate the 'DROP' query for the given table.
	 * @param table the table to drop
	 * @return the 'DROP' query
	 */
	default String toSQLDrop(Table table) {
		return MessageFormat.format(DROP, table.qualifiedName());
	}
}
