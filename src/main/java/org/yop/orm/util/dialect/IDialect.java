package org.yop.orm.util.dialect;

import com.google.common.base.Joiner;
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

	/** Select [what] FROM [table] [table_alias] [join clause] WHERE [where clause] [order by clause] [extra] */
	String SELECT = " SELECT {0} FROM {1} {2} {3} WHERE {4} {5} {6}";

	/** Select distinct([what]) FROM [table] [table_alias] [join clause] WHERE [where clause] [extra] */
	String SELECT_DISTINCT = " SELECT DISTINCT({0}) FROM {1} {2} {3} WHERE {4} {5}";

	/** COUNT(DISTINCT :idColumn) column selection */
	String COUNT_DISTINCT = " COUNT(DISTINCT {0}) ";

	/** DELETE [columns] FROM [table] [join clauses] WHERE [where clause] */
	String DELETE = " DELETE {0} FROM {1} {2} WHERE {3} ";

	/** DELETE FROM [table] WHERE [column] IN ([values]) */
	String DELETE_IN = " DELETE FROM {0} WHERE {1} IN ({2}) ";

	/** INSERT INTO [table] ([columns]) VALUES ([column values]) */
	String INSERT = " INSERT INTO {0} ({1}) VALUES ({2}) ";

	/** UPDATE [table] SET ([column=value]+) WHERE ([idColumn] = ? ) */
	String UPDATE = " UPDATE {0} SET {1} WHERE ({2} = ?) ";

	/** [table] [table alias] on [column name] = [value]. Then prefix with the join type. */
	String JOIN_ON = " {0} {1} on {2} = {3} ";

	/** [column] IN ([comma separated values]) */
	String IN = " {0} IN ({1}) ";

	String AND = " AND ";

	String EXISTS = " EXISTS ({0}) ";

	/** Default where clause is always added. So I don't have to check if the 'WHERE' keyword is required ;-) */
	String DEFAULT_WHERE = " 1=1 ";

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
	 * <br>
	 * e.g.  CONSTRAINT BOOK_NK UNIQUE (publish_date, name, author_id)
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

	/**
	 * Generate 'COUNT (DISTINCT [column alias])'.
	 * @param columnAlias the column alias
	 * @return COUNT (DISTINCT alias)
	 */
	default String toSQLCount(String columnAlias) {
		return MessageFormat.format(COUNT_DISTINCT, columnAlias);
	}

	/**
	 * Build the Select query from component clauses
	 * @param what        Mandatory. Columns clause.
	 * @param from        Mandatory. Target table.
	 * @param as          Mandatory. Target table alias.
	 * @param joinClause  Optional. Join clause.
	 * @param whereClause Optional. Where clause.
	 * @param orderClause Optional. Order clause.
	 * @param extras      Any extra clause.
	 * @return the SQL select query.
	 */
	default String select(
		String what,
		String from,
		String as,
		String joinClause,
		String whereClause,
		String orderClause,
		String... extras) {
		String extra = MessageUtil.concat(extras);
		return MessageFormat.format(
			SELECT,
			what,
			from,
			as,
			joinClause,
			StringUtils.isBlank(whereClause) ? DEFAULT_WHERE : whereClause,
			orderClause,
			extra
		);
	}

	/**
	 * Build the 'distinct' Select query from component clauses.
	 * @param what        Mandatory. Column clause that is to be distinct.
	 * @param from        Mandatory. Target table.
	 * @param as          Mandatory. Target table alias.
	 * @param joinClause  Optional. Join clause.
	 * @param whereClause Optional. Where clause.
	 * @param extras      Any extra clause.
	 * @return the SQL 'distinct' select query.
	 */
	default String selectDistinct(
		String what,
		String from,
		String as,
		String joinClause,
		String whereClause,
		String... extras) {
		String extra = MessageUtil.concat(extras);
		return MessageFormat.format(
			SELECT_DISTINCT,
			what,
			from,
			as,
			joinClause,
			StringUtils.isBlank(whereClause) ? DEFAULT_WHERE : whereClause,
			extra
		);
	}

	/**
	 * Generate a 'DELETE' query
	 * @param columnsClause the column clause (columns to select, comma separated)
	 * @param tableName    the target table name
	 * @param joinClauses  the joined table clauses
	 * @param whereClause  the where clause. Optional.
	 * @return the {@link #DELETE} formatted query
	 */
	default String delete(
		String columnsClause,
		String tableName,
		String joinClauses,
		String whereClause) {
		return MessageFormat.format(
			DELETE,
			columnsClause,
			tableName,
			joinClauses,
			StringUtils.isBlank(whereClause) ? DEFAULT_WHERE : whereClause
		);
	}

	/**
	 * Generate a 'DELETE IN' query
	 * @param from         the relation table name
	 * @param sourceColumn the source column name
	 * @param ids          the IDs for the IN clause. Mostly a list of '?' if you want to use query parameters.
	 * @return the {@link #DELETE_IN} formatted query
	 */
	default String deleteIn(String from, String sourceColumn, Collection<String> ids) {
		return MessageFormat.format(DELETE_IN, from, sourceColumn, MessageUtil.join(",", ids));
	}

	/**
	 * Generate an 'INSERT INTO' query.
	 * @param tableName the target table name
	 * @param columns   the columns to select
	 * @param values    the column values
	 * @return the {@link #INSERT} formatted query
	 */
	default String insert(String tableName, List<String> columns, List<String> values) {
		return MessageFormat.format(INSERT, tableName, Joiner.on(", ").join(columns), Joiner.on(", ").join(values));
	}

	/**
	 * Generate an 'UPDATE' query.
	 * @param tableName       the target table name
	 * @param columnAndValues the column and values '=' separated. e.g 'name=?'
	 * @param idColumn        the id column (required for : WHERE idColumn = ?)
	 * @return the {@link #UPDATE} formatted query
	 */
	default String update(String tableName, List<String> columnAndValues, String idColumn) {
		return MessageFormat.format(UPDATE, tableName, Joiner.on(", ").join(columnAndValues), idColumn);
	}

	/**
	 * Generate an 'UPDATE' query for a single column, useful for join tables update.
	 * <br>
	 * e.g. UPDATE table SET column=? WHERE idColumn=?
	 * @param tableName  the target table name
	 * @param column     the column name
	 * @param idColumn   the id column (required for : WHERE idColumn = ?)
	 * @return the {@link #UPDATE} formatted query
	 */
	default String update(String tableName, String column, String idColumn) {
		return MessageFormat.format(UPDATE, tableName, column + "=?", idColumn);
	}

	/**
	 * Generate a JOIN clause using {@link #JOIN_ON}.
	 * <br>
	 * e.g. left join join_table joined_table_alias on join_column_a = root_table_alias.column_a
	 * @param joinType   the join type to use (see {@link org.yop.orm.query.AbstractJoin.JoinType#sql})
	 * @param table      the table name
	 * @param tableAlias the table alias
	 * @param left       the left side of the "on" clause
	 * @param right      the right side of the "on" clause
	 * @return the formatted SQL join clause
	 */
	default String join(String joinType, String table, String tableAlias, String left, String right) {
		return joinType + MessageFormat.format(JOIN_ON, table, tableAlias, left, right);
	}

	/**
	 * Generate an 'IN' clause using {@link #IN}.
	 * <br>
	 * e.g. idColumn IN (?,?,?,?)
	 * @param column the column name
	 * @param values the values : for each value, a '?' will be added in 'IN ()'.
	 * @return the {@link #IN} formatted clause
	 */
	default String in(String column, Collection<?> values) {
		return MessageFormat.format(IN, column, values.stream().map(v -> "?").collect(Collectors.joining(",")));
	}

	/**
	 * Simply join the where clauses using " AND ". Clauses can be null or empty.
	 * @param whereClauses the where clauses to join
	 * @return the new where clause
	 */
	default String where(String... whereClauses) {
		return this.where(Arrays.asList(whereClauses));
	}

	/**
	 * Simply join the where clauses using " AND ". Clauses can be null or empty.
	 * @param whereClauses the where clauses to join
	 * @return the new where clause
	 */
	default String where(Collection<String> whereClauses) {
		return MessageUtil.join(AND, whereClauses);
	}

	/**
	 * Create a 'SELECT WHERE EXISTS ()' query, using a subselect, from the different parts of the query.
	 * <br>
	 * FIXME : using this method is requires the parameters to be in the same order as the query build.
	 * @param idAlias              the main table ID alias
	 * @param columns              the columns to select
	 * @param from                 the main table
	 * @param as                   the main table alias
	 * @param joinClause           the join clause
	 * @param joinClauseWhere      the where clause from all the join clauses
	 * @param subSelectIdAlias     the main table ID alias for the subselect query
	 * @param subSelectAs          the subselect main table alias
	 * @param subSelectJoinClause  the subselect join clauses
	 * @param subSelectWhereClause the subselect where clause
	 * @param pagingClause         the paging clause
	 * @param orderClause          the 'order by' clause
	 * @return the assembled SELECT WHERE EXIST query
	 */
	default String selectWhereExists(
		String idAlias,
		String columns,
		String from,
		String as,
		String joinClause,
		String joinClauseWhere,
		String subSelectIdAlias,
		String subSelectAs,
		String subSelectJoinClause,
		String subSelectWhereClause,
		String pagingClause,
		String orderClause) {

		String subSelect = this.selectDistinct(
			subSelectIdAlias,
			from,
			subSelectAs,
			subSelectJoinClause,
			this.where(subSelectWhereClause, idAlias + " = " + subSelectIdAlias),
			pagingClause
		);

		String where = this.where(joinClauseWhere, MessageFormat.format(EXISTS, subSelect));
		return this.select(columns, from, as, joinClause, where, orderClause);
	}

	/**
	 * Create a 'SELECT WHERE ID IN ()' query, using a subselect, from the different parts of the query.
	 * <br>
	 * FIXME : using this method is requires the parameters to be in the same order as the query build.
	 * @param idAlias         the main table ID alias
	 * @param columns         the columns to select
	 * @param from            the main table
	 * @param as              the main table alias
	 * @param joinClause      the join clause
	 * @param joinClauseWhere the where clause from all the join clauses
	 * @param whereClause     the where clause
	 * @param pagingOrderBy   the 'order by' clause that can be required for the paging clause
	 * @param pagingClause    the paging clause
	 * @param orderClause     the 'order by' clause
	 * @return the assembled SELECT WHERE ID IN query
	 */
	default String selectWhereIdIn(
		String idAlias,
		String columns,
		String from,
		String as,
		String joinClause,
		String joinClauseWhere,
		String whereClause,
		String pagingOrderBy,
		String pagingClause,
		String orderClause) {

		String inSubQuery = this.select(
			idAlias,
			from,
			as,
			"",
			this.where(whereClause, joinClauseWhere),
			pagingOrderBy,
			pagingClause
		);

		String where = idAlias + MessageFormat.format(IN, "", inSubQuery);
		return this.select(columns, from, as, joinClause, where, orderClause);
	}
}
