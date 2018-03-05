package org.yop.orm.gen;

import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.example.Pojo;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.MessageUtil;
import org.yop.orm.util.ORMTypes;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Table model, that can generate SQL CREATE queries.
 * <br>
 * This is pretty raw. This was written to prepare unit tests context.
 */
public class Table {

	private String name;
	private String schema;
	private ORMTypes types;
	private boolean relation = false;

	private Collection<Column> columns = new ArrayList<>();

	private Table(ORMTypes types) {
		this.types = types;
	}

	public boolean isRelation() {
		return relation;
	}

	public String qualifiedName() {
		return MessageUtil.join(".", schema, name);
	}

	public Collection<Column> getColumns() {
		return this.columns;
	}

	private String toSQL() {
		return this.types.toSQL(this);
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
	public static Set<Table> findAllInClassPath(String packagePrefix, ORMTypes types) {
		Set<Class<? extends Yopable>> subtypes = new Reflections(packagePrefix).getSubTypesOf(Yopable.class);

		Set<Table> tables = new TreeSet<>(Comparator.comparing(Table::isRelation).thenComparing(Table::qualifiedName));
		subtypes
			.stream()
			.filter(c -> ! c.isInterface() &&  ! Modifier.isAbstract(c.getModifiers() ))
			.forEach(clazz -> tables.addAll(Table.findTablesFor(clazz, types)));

		return tables;
	}

	/**
	 * Find the Yopable table and its relation tables required to map the Yopable class.
	 * @param clazz the Yopable class
	 * @param types the ORM type, giving DBMS hints
	 * @return the table objects that can be used to get INSERT queries
	 */
	private static Set<Table> findTablesFor(Class<? extends Yopable> clazz, ORMTypes types) {
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
	private static Table fromClass(Class<? extends Yopable> clazz, ORMTypes types) {
		Table table = new Table(types);
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
		JoinTable joinTable = relationField.getAnnotation(JoinTable.class);

		Table table = new Table(types);
		table.relation = true;
		table.schema = joinTable.schema();
		table.name   = joinTable.table();

		@SuppressWarnings("unchecked")
		Class<? extends Yopable> sourceClass = (Class<? extends Yopable>) relationField.getDeclaringClass();
		Column source = new Column(joinTable.sourceColumn(), types.getForType(Long.class), 0, types);
		createJoinTableColumnAttributes(source, sourceClass, joinTable.sourceForeignKey(), joinTable);
		table.columns.add(source);

		@SuppressWarnings("unchecked")
		Class<? extends Yopable> targetClass = (Class<? extends Yopable>)relationField.getType();
		if(Collection.class.isAssignableFrom(targetClass)) {
			targetClass = Reflection.getCollectionTarget(relationField);
		}
		Column target = new Column(joinTable.targetColumn(), types.getForType(Long.class), 0, types);
		createJoinTableColumnAttributes(target, targetClass, joinTable.targetForeignKey(), joinTable);
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

	private static void createJoinTableColumnAttributes(
		Column column,
		Class<? extends Yopable> reference,
		String foreignKeyName,
		JoinTable joinTable) {

		String referencedTable = ORMUtil.getQualifiedTableName(reference);
		String referencedIdColumn = ORMUtil.getIdColumn(reference);
		foreignKeyName =
			StringUtils.isBlank(foreignKeyName)
			? "FK_" + joinTable.table() + "_" + referencedTable + "_" + referencedIdColumn + "_" + column.getName()
			: foreignKeyName;

		column.setFK(new ForeignKey(foreignKeyName, referencedTable, referencedIdColumn));
	}
}
