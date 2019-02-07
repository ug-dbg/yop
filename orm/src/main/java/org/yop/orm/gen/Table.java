package org.yop.orm.gen;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Config;
import org.yop.orm.util.JoinUtil;
import org.yop.orm.util.MessageUtil;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;
import org.yop.orm.sql.dialect.IDialect;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Table model, that can generate SQL CREATE queries.
 * <br>
 * Tables are {@link Comparable}, comparing :
 * <ol>
 *     <li>{@link Table#isRelation()}</li>
 *     <li>{@link Table#qualifiedName()}</li>
 * </ol>
 */
public class Table implements Comparable<Table> {

	/** Relation tables last */
	private static final Comparator<Table> COMPARATOR = Comparator
		.comparing(Table::isRelation)
		.thenComparing(Table::qualifiedName);

	private String name;
	private String schema;
	private IDialect types;
	private boolean relation = false;

	private Collection<Column> columns = new ArrayList<>();

	private Table(IDialect types) {
		this.types = types;
	}

	public boolean isRelation() {
		return this.relation;
	}

	public String name() {
		return this.name;
	}

	public String schema() {
		return this.schema;
	}

	public String qualifiedName() {
		return MessageUtil.join(".", this.schema, this.name);
	}

	public Collection<Column> getColumns() {
		return this.columns;
	}

	public String toSQL() {
		return this.types.toSQL(this);
	}

	/**
	 * An extra list of sql queries to be executed for this table.
	 * @return an ordered list of queries to execute so the table is entirely operational.
	 */
	public List<String> otherSQL() {
		return this.types.otherSQL(this);
	}

	/**
	 * The 'DROP' query to drop this table.
	 * @return the DROP query for this table and the {@link #types} dialect.
	 */
	public String toSQLDROP() {
		return this.types.toSQLDrop(this);
	}

	@Override
	public String toString() {
		return "Table{" +
			"name='" + this.name + '\'' +
			", schema='" + this.schema + '\'' +
			", types=" + this.types +
			", relation=" + this.relation +
			", columns=" + this.columns +
		'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || this.getClass() != o.getClass()) return false;
		Table table = (Table) o;
		return Objects.equals(this.name, table.name) && Objects.equals(this.schema, table.schema);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name, this.schema);
	}

	@Override
	public int compareTo(@Nonnull Table o) {
		return COMPARATOR.compare(this, o);
	}

	/**
	 * Find all the tables required to map the Yopable classes with the given package prefix
	 * @param packageName the package name. Can be fickle.
	 * @param classLoader   the class loader to use
	 * @return the table objects that can be used to get INSERT queries
	 */
	public static Set<Table> findAllInClassPath(String packageName, ClassLoader classLoader, Config config) {
		Set<Class<? extends Yopable>> subtypes = ORMUtil.yopables(classLoader);

		Set<Table> tables = new TreeSet<>();
		subtypes
			.stream()
			.filter(c -> Reflection.packageName(c).equals(packageName))
			.filter(c -> ! c.isInterface() &&  ! Modifier.isAbstract(c.getModifiers() ))
			.forEach(clazz -> tables.addAll(Table.findTablesFor(clazz, config)));

		return tables;
	}

	/**
	 * Find the Yopable table and its relation tables required to map the Yopable class.
	 * @param clazz  the Yopable class
	 * @param config the SQL config. Needed for the sql separator to use.
	 * @return the table objects that can be used to get INSERT queries
	 */
	@SuppressWarnings("WeakerAccess")
	public static Set<Table> findTablesFor(Class<? extends Yopable> clazz, Config config) {
		Set<Table> tables = new HashSet<>();
		tables.add(Table.fromClass(clazz, config));
		readRelationTables(clazz, tables, config);
		return tables;
	}

	/**
	 * Find the Yopable table required to map the Yopable class.
	 * @param clazz  the Yopable class
	 * @param config the SQL config. Needed for the sql separator to use.
	 * @return the table object that can be used to get INSERT query
	 */
	private static Table fromClass(Class<? extends Yopable> clazz, Config config) {
		Table table = new Table(config.getDialect());
		table.schema = ORMUtil.getSchemaName(clazz);
		table.name   = ORMUtil.getTableName(clazz);

		table.columns = ORMUtil
			.getFields(clazz, org.yop.orm.annotations.Column.class)
			.stream()
			.map(field -> Column.fromField(field, config))
			.collect(Collectors.toList());

		table.columns.addAll(
			JoinUtil.joinColumnYopableFields(clazz)
			.stream()
			.map(field -> Column.fromJoinColumnField(field, config))
			.collect(Collectors.toList())
		);

		return table;
	}

	@SuppressWarnings("unchecked")
	private static Table fromRelationField(Field relationField, Config config) {
		JoinTable joinTable = relationField.getAnnotation(JoinTable.class);

		Table table = new Table(config.getDialect());
		table.relation = true;
		table.schema = joinTable.schema();
		table.name   = joinTable.table();

		Class<? extends Yopable> sourceClass = (Class<? extends Yopable>) relationField.getDeclaringClass();
		Column source = new Column(joinTable.sourceColumn(), Long.class, 0, config.getDialect());
		createJoinTableColumnAttributes(source, sourceClass, joinTable.sourceForeignKey(), joinTable, config);
		table.columns.add(source);

		Class<? extends Yopable> targetClass;
		if(ORMUtil.isCollection(relationField)) {
			targetClass = Reflection.getCollectionTarget(relationField);
		} else {
			targetClass = (Class<? extends Yopable>)relationField.getType();
		}
		Column target = new Column(joinTable.targetColumn(), Long.class, 0, config.getDialect());
		createJoinTableColumnAttributes(target, targetClass, joinTable.targetForeignKey(), joinTable, config);
		table.columns.add(target);

		return table;
	}

	private static void readRelationTables(
		Class<? extends Yopable> clazz,
		Set<Table> tables,
		Config config) {
		tables.addAll(JoinUtil
			.joinTableFields(clazz)
			.stream()
			.map(field -> fromRelationField(field, config))
			.collect(Collectors.toSet()));
	}

	private static void createJoinTableColumnAttributes(
		Column column,
		Class<? extends Yopable> reference,
		String foreignKeyName,
		JoinTable joinTable,
		Config config) {

		String referencedTable = ORMUtil.getQualifiedTableName(reference, config);
		String referencedIdColumn = ORMUtil.getIdColumn(reference);
		foreignKeyName =
			StringUtils.isBlank(foreignKeyName)
			? "FK_" + joinTable.table() + "_" + column.getName()
			: foreignKeyName;

		foreignKeyName =
			foreignKeyName.length() > config.aliasMaxLength()
			? ORMUtil.uniqueShortened(foreignKeyName, config)
			: foreignKeyName;

		column.setFK(new ForeignKey(foreignKeyName, referencedTable, referencedIdColumn));
	}
}
