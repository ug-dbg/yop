package org.yop.orm.gen;

import com.google.common.base.Joiner;
import org.reflections.Reflections;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.example.Pojo;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.MessageUtil;
import org.yop.orm.util.ORMTypes;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Table model, that can generate SQL CREATE queries.
 * <br>
 * This is pretty raw. This was written to prepare unit tests context.
 */
public class Table {

	private static final String CREATE = " CREATE TABLE {0} ({1});";

	private String name;
	private String schema;

	private Collection<Column> columns = new ArrayList<>();

	private String qualifiedName() {
		return MessageUtil.join(".", schema, name);
	}

	private String toSQL() {
		return MessageFormat.format(CREATE, this.qualifiedName(), Joiner.on(", ").join(this.columns));
	}

	@Override
	public String toString() {
		return this.toSQL();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Table table = (Table) o;
		return Objects.equals(name, table.name) && Objects.equals(schema, table.schema);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, schema);
	}

	/**
	 * Find all the tables required to map the Yopable classes with the given package prefix
	 * @param packagePrefix the package prefix. Can be fickle.
	 * @return the table objects that can be used to get INSERT queries
	 */
	public static Set<Table> findAllInClassPath(String packagePrefix) {
		Set<Class<? extends Yopable>> subtypes = new Reflections().getSubTypesOf(Yopable.class);
		Set<Table> tables = new HashSet<>();
		subtypes
			.stream()
			.filter(c -> ! c.isInterface() &&  ! Modifier.isAbstract(c.getModifiers() ))
			.forEach(clazz -> tables.addAll(Table.findTablesFor(clazz, ORMTypes.SQLITE)));
		return tables;
	}

	/**
	 * Find the Yopable table and its relation tables required to map the Yopable class.
	 * @param clazz the Yopable class
	 * @param types the ORM type, giving DBMS hints
	 * @return the table objects that can be used to get INSERT queries
	 */
	public static Set<Table> findTablesFor(Class<? extends Yopable> clazz, ORMTypes types) {
		Set<Table> tables = new HashSet<>();
		tables.add(Table.fromClass(clazz, types));
		readRelationTables(Pojo.class, types, tables);
		return tables;
	}

	/**
	 * Find the Yopable table required to map the Yopable class.
	 * @param clazz the Yopable class
	 * @param types the ORM type, giving DBMS hints
	 * @return the table object that can be used to get INSERT query
	 */
	public static Table fromClass(Class<? extends Yopable> clazz, ORMTypes types) {
		Table table = new Table();
		table.schema = ORMUtil.getSchemaName(clazz);
		table.name   = ORMUtil.getTableName(clazz);

		table.columns = Reflection
			.getFields(clazz, org.yop.orm.annotations.Column.class)
			.stream()
			.map(field -> Column.fromField(field, types))
			.collect(Collectors.toList());

		return table;
	}

	private static Table fromRelationField(Field relationField, ORMTypes types) {
			Table table = new Table();
			table.schema = relationField.getAnnotation(JoinTable.class).schema();
			table.name   = relationField.getAnnotation(JoinTable.class).table();

			Column source = new Column();
			source.type = types.getForType(Long.class);
			source.name = relationField.getAnnotation(JoinTable.class).sourceColumn();
			table.columns.add(source);

			Column target = new Column();
			target.type = types.getForType(Long.class);
			target.name = relationField.getAnnotation(JoinTable.class).targetColumn();
			table.columns.add(target);

			return table;
		}

	private static void readRelationTables(Class<? extends Yopable> clazz, ORMTypes types, Set<Table> tables) {
		tables.addAll(Reflection
			.getFields(clazz, org.yop.orm.annotations.JoinTable.class, false)
			.stream()
			.map(field -> fromRelationField(field, types))
			.collect(Collectors.toSet()));
	}

	/**
	 * A table column that can generate a column clause in an SQL CREATE query.
	 */
	private static class Column {
		private String name;
		private String type;
		private String attributes = "";

		private String toSQL() {
			return this.name + " "
				+ this.type
				+ this.attributes
				+ " ";
		}

		@Override
		public String toString() {
			return this.toSQL();
		}

		@SuppressWarnings("unchecked")
		private static Column fromField(Field field, ORMTypes types) {
			Column column = new Column();
			column.name = ORMUtil.getColumnName(field);
			column.type = ORMUtil.getColumnType(field, types);

			boolean primaryKey = false;
			boolean autoincrement = false;
			int length = ORMUtil.getColumnLength(field);
			if(field.equals(ORMUtil.getIdField((Class<? extends Yopable>)field.getDeclaringClass()))) {
				primaryKey = true;
				autoincrement = !field.isAnnotationPresent(Id.class) || field.getAnnotation(Id.class).autoincrement();
			}
			column.attributes = types.getColumnAttributes(autoincrement, primaryKey, length);
			return column;
		}
	}
}
