package org.yop.orm.sql.dialect;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.gen.Column;
import org.yop.orm.gen.ForeignKey;
import org.yop.orm.gen.Table;
import org.yop.orm.query.sql.Paging;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.SQLPart;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.MessageUtil;

import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.yop.orm.sql.dialect.SQL.*;

/**
 * Interface of an SQL dialect.
 * <br>
 * A dialect must :
 * <ul>
 *     <li>know the SQL type for a Java type</li>
 *     <li>generate SQL for SQL data structures (Column, Table)</li>
 *     <li>generate SQL for SQL requests (Create, Drop, Select, Insert, Update, Delete)</li>
 * </ul>
 * NB : Yop tries to generate some very basic SQL CRUD queries that does not rely on an SQL dialect.
 */
@SuppressWarnings("unused")
public interface IDialect {

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
	 * Get the Java type for the given SQL type.
	 * @param sqlType the sql type
	 * @return the java type class
	 */
	Class<?> getForType(int sqlType);

	/**
	 * Yop uses SQL column labels to store the path of an entity in the data graph.
	 * <br>
	 * This is the separator to use for these paths.
	 * @return default value : '→'
	 */
	default String pathSeparator() {
		return "→";
	}

	/**
	 * When a column name in a query is too long, YOP replaces it with a generated alias.
	 * @return default value : 40
	 */
	default int aliasMaxLength() {
		return 40;
	}

	/**
	 * Some dialects does not support auto-increment syntax and uses sequences instead, e.g. Oracle.
	 * @return default value : false
	 */
	default boolean useSequences() {
		return false;
	}

	/**
	 * Some DBMS does not support batch inserts.
	 * @return default value : true
	 */
	default boolean useBatchInserts() {
		return true;
	}

	/**
	 * Some DBMS does not support NULL in unique constraints
	 * (or only one NULL, but I'm not willing to tune dialects that finely).
	 * @return default value : true
	 */
	default boolean nullInNK() {
		return true;
	}

	/**
	 * The maximum amount of JDBC parameters this dialect supports.
	 * <br>
	 * This is mostly used when deleting by ID.
	 * @return default value : 1000
	 */
	default int maxParameters() {
		return 1000;
	}

	/**
	 * The paging method of this dialect. See {@link Paging}.
	 * @return default value : {@link org.yop.orm.query.sql.Paging.Method#TWO_QUERIES}
	 */
	default Paging.Method pagingMethod() {
		return Paging.Method.TWO_QUERIES;
	}

	/**
	 * The dialect default column length (if {@link Column#length} is not set).
	 * @return 50
	 */
	default Integer defaultColumnLength() {
		return 50;
	}

	/**
	 * Set the value of a parameter in a statement.
	 * <br>
	 * Override this if your JDBC driver does not fully support {@link PreparedStatement#setObject(int, Object)}.
	 * @param statement the SQL statement
	 * @param index     the parameter index <b>1-based</b> (first is 1, second is 2...)
	 * @param parameter the parameter to set
	 * @throws SQLException see {@link PreparedStatement#setObject(int, Object)}
	 */
	default void setParameter(
		PreparedStatement statement,
		int index,
		Parameters.Parameter parameter)
		throws SQLException {
		statement.setObject(index, parameter.getValue());
	}

	/**
	 * Implement to do something before the connection is closed.
	 * @param connection the connection adapter
	 */
	default void onClose(IConnection connection) {}

	/**
	 * Create a default dialect implementation, with "VARCHAR" default type.
	 * @return a new instance of {@link Dialect}.
	 */
	static IDialect defaultDialect() {
		return new Dialect(JDBCType.VARCHAR.getName()) {};
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
			+ " " + this.type(column) + " "
			+ (column.isNotNull() || (column.isNaturalKey() && ! this.nullInNK()) ? " NOT NULL " : "")
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
		return MessageFormat.format(FK,
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
	 * Get the pattern for a SELECT query.
	 * <br>
	 * This is a pipe to either {@link #selectAndLockPattern(boolean)} or {@link #selectPattern(boolean)}.
	 * @param lock     true → SELECT... FOR UPDATE (or whatever the name in the underlying dialect)
	 * @param distinct true → SELECT DISTINCT([column])
	 * @return the select pattern to use
	 */
	default String selectPattern(boolean lock, boolean distinct) {
		return lock ? this.selectAndLockPattern(distinct) : this.selectPattern(distinct);
	}

	/**
	 * Get the pattern for a SELECT query <b>WITH NO LOCKING</b>.
	 * @param distinct true → SELECT DISTINCT([column])
	 * @return the select pattern to use
	 */
	default String selectPattern(boolean distinct) {
		return distinct ? DEFAULT_SELECT_DISTINCT_PATTERN : DEFAULT_SELECT_PATTERN;
	}

	/**
	 * Get the pattern for a SELECT query <b>WITH LOCKING</b>.
	 * @param distinct true → SELECT DISTINCT(id)
	 * @return the select pattern to use
	 */
	default String selectAndLockPattern(boolean distinct) {
		return distinct ? DEFAULT_SELECT_DISTINCT_FOR_UPDATE_PATTERN : DEFAULT_SELECT_FOR_UPDATE_PATTERN;
	}

	/**
	 * Generate 'COUNT (DISTINCT [column alias])'.
	 * @param columnAlias the column alias
	 * @return COUNT (DISTINCT alias)
	 */
	default String toSQLCount(String columnAlias) {
		return SQLPart.forPattern(DEFAULT_COUNT_DISTINCT_PATTERN, columnAlias).toString();
	}

	/**
	 * Build the Select query from component clauses
	 * @param lock        true to lock the SELECT results.
	 * @param what        Mandatory. Columns clause.
	 * @param from        Mandatory. Target table.
	 * @param as          Mandatory. Target table alias.
	 * @param joinClause  Optional. Join clause.
	 * @param whereClause Optional. Where clause.
	 * @param orderClause Optional. Order clause.
	 * @param extras      Any extra clause.
	 * @return the SQL select query.
	 */
	default SQLPart select(
		boolean lock,
		CharSequence what,
		CharSequence from,
		CharSequence as,
		CharSequence joinClause,
		CharSequence whereClause,
		CharSequence orderClause,
		CharSequence... extras) {

		String pattern = this.selectPattern(lock, false);
		CharSequence any = SQLPart.join(" ", extras);
		CharSequence safeWhereClause = StringUtils.isBlank(whereClause.toString()) ? DEFAULT_WHERE : whereClause;
		return SQLPart.forPattern(pattern, what, from, as, joinClause, safeWhereClause, orderClause, any);
	}

	/**
	 * Build the 'distinct' Select query from component clauses.
	 * @param lock        true to lock the SELECT results.
	 * @param what        Mandatory. Column clause that is to be distinct.
	 * @param from        Mandatory. Target table.
	 * @param as          Mandatory. Target table alias.
	 * @param joinClause  Optional. Join clause.
	 * @param whereClause Optional. Where clause.
	 * @param extras      Any extra clause.
	 * @return the SQL 'distinct' select query.
	 */
	default SQLPart selectDistinct(
		boolean lock,
		CharSequence what,
		CharSequence from,
		CharSequence as,
		CharSequence joinClause,
		CharSequence whereClause,
		CharSequence... extras) {

		String pattern = this.selectPattern(lock, true);
		CharSequence any = SQLPart.join(" ", extras);
		CharSequence safeWhereClause = StringUtils.isBlank(whereClause.toString()) ? DEFAULT_WHERE : whereClause;
		return SQLPart.forPattern(pattern, what, from, as, joinClause, safeWhereClause, "", any);
	}

	/**
	 * Generate a 'DELETE' query
	 * @param columnsClause the column clause (columns to select, comma separated)
	 * @param tableName    the target table name
	 * @param joinClauses  the joined table clauses
	 * @param whereClause  the where clause. Optional.
	 * @return the {@link SQL#DELETE} formatted query
	 */
	default SQLPart delete(
		CharSequence columnsClause,
		CharSequence tableName,
		CharSequence joinClauses,
		CharSequence whereClause) {

		CharSequence safeWhereClause = StringUtils.isBlank(whereClause.toString()) ? DEFAULT_WHERE : whereClause;
		return SQLPart.forPattern(DEFAULT_DELETE_PATTERN, columnsClause, tableName, joinClauses, safeWhereClause);
	}

	/**
	 * Generate a 'DELETE IN' query
	 * @param from         the relation table name
	 * @param sourceColumn the source column name
	 * @param ids          the IDs for the IN clause. Mostly a list of '?' if you want to use query parameters.
	 * @return the {@link SQL#DEFAULT_DELETE_IN_PATTERN} formatted query
	 */
	default SQLPart deleteIn(String from, String sourceColumn, List<SQLPart> ids) {
		return SQLPart.forPattern(DEFAULT_DELETE_IN_PATTERN, from, sourceColumn, SQLPart.join(",", ids));
	}

	/**
	 * Generate an 'INSERT INTO' query.
	 * @param tableName the target table name
	 * @param columns   the columns to select
	 * @param values    the column values
	 * @return the {@link SQL#INSERT} formatted query
	 */
	default SQLPart insert(String tableName, List<? extends CharSequence> columns, List<? extends CharSequence> values) {
		return SQLPart.forPattern(
			DEFAULT_INSERT_PATTERN,
			tableName,
			SQLPart.join(" , ", columns),
			SQLPart.join(" , ", values)
		);
	}

	/**
	 * Generate an 'UPDATE' query.
	 * @param tableName       the target table name
	 * @param columnAndValues the column and values '=' separated. e.g 'name=?'
	 * @param idColumn        the id column (required for : WHERE idColumn = ?)
	 * @return the {@link SQL#UPDATE} formatted query
	 */
	default SQLPart update(String tableName, List<? extends CharSequence> columnAndValues, SQLPart idColumn) {
		return SQLPart.forPattern(DEFAULT_UPDATE_PATTERN, tableName, SQLPart.join(", ", columnAndValues), idColumn);
	}

	/**
	 * Generate an 'UPDATE' query for a single column, useful for join tables update.
	 * <br>
	 * e.g. UPDATE table SET column=? WHERE idColumn=?
	 * @param tableName      the target table name
	 * @param columnAndValue the column name and value
	 * @param idColumn   the id column (required for : WHERE idColumn = ?)
	 * @return the {@link SQL#UPDATE} formatted query
	 */
	default SQLPart update(String tableName, SQLPart columnAndValue, SQLPart idColumn) {
		return SQLPart.forPattern(DEFAULT_UPDATE_PATTERN, tableName, columnAndValue, idColumn);
	}

	/**
	 * Generate a JOIN clause using {@link SQL#DEFAULT_JOIN_ON_PATTERN}.
	 * <br>
	 * e.g. left join join_table joined_table_alias on join_column_a = root_table_alias.column_a
	 * @param joinType   the join type to use (see {@link org.yop.orm.query.sql.SQLJoin.JoinType#sql})
	 * @param table      the table name
	 * @param tableAlias the table alias
	 * @param left       the left side of the "on" clause
	 * @param right      the right side of the "on" clause
	 * @return the formatted SQL join clause
	 */
	default String join(String joinType, String table, String tableAlias, String left, String right) {
		return joinType + SQLPart.forPattern(DEFAULT_JOIN_ON_PATTERN, table, tableAlias, left, right).toString();
	}

	/**
	 * Generate an 'IN' clause using {@link SQL#IN}.
	 * <br>
	 * e.g. idColumn IN (?,?,?,?)
	 * @param column the column name
	 * @param values the values : for each value, a '?' will be added in 'IN ()'.
	 * @return the {@link SQL#IN} formatted clause
	 */
	default SQLPart in(String column, List<? extends CharSequence> values) {
		return SQLPart.forPattern(DEFAULT_IN_PATTERN, column, SQLPart.join(" , ", values));
	}

	/**
	 * Simply join the where clauses using " AND ". Clauses can be null or empty.
	 * @param whereClauses the where clauses to join
	 * @return the new where clause
	 */
	default SQLPart where(CharSequence... whereClauses) {
		return this.where(Arrays.asList(whereClauses));
	}

	/**
	 * Simply join the where clauses using " AND ". Clauses can be null or empty.
	 * @param whereClauses the where clauses to join
	 * @return the new where clause
	 */
	default SQLPart where(List<? extends CharSequence> whereClauses) {
		return SQLPart.join(" " + AND + " ", whereClauses);
	}

	/**
	 * Join the 2 sequences using {@link SQL#DEFAULT_EQUALS_PATTERN} pattern.
	 * @param a the left part
	 * @param b the right part
	 * @return a new SQL part, for the {@link SQL#DEFAULT_EQUALS_PATTERN} pattern.
	 */
	default SQLPart equals(CharSequence a, CharSequence b) {
		return SQLPart.forPattern(DEFAULT_EQUALS_PATTERN, a, b);
	}

	/**
	 * Create a 'SELECT WHERE EXISTS ()' query, using a subselect, from the different parts of the query.
	 * <br>
	 * @param lock                 true to lock the SELECT results.
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
	default SQLPart selectWhereExists(
		boolean lock,
		CharSequence idAlias,
		CharSequence columns,
		CharSequence from,
		CharSequence as,
		CharSequence joinClause,
		CharSequence joinClauseWhere,
		CharSequence subSelectIdAlias,
		CharSequence subSelectAs,
		CharSequence subSelectJoinClause,
		CharSequence subSelectWhereClause,
		CharSequence pagingClause,
		CharSequence orderClause,
		CharSequence... extras) {

		SQLPart subSelect = this.selectDistinct(
			false,
			subSelectIdAlias,
			from,
			subSelectAs,
			subSelectJoinClause,
			this.where(subSelectWhereClause, this.equals(idAlias, subSelectIdAlias)),
			pagingClause
		);

		SQLPart where = this.where(joinClauseWhere, SQLPart.forPattern(DEFAULT_EXISTS_PATTERN, subSelect));
		return this.select(lock, columns, from, as, joinClause, where, orderClause, extras);
	}

	/**
	 * Create a 'SELECT WHERE ID IN ()' query, using a subselect, from the different parts of the query.
	 * <br>
	 * @param lock            true to lock the SELECT results.
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
	default SQLPart selectWhereIdIn(
		boolean lock,
		CharSequence idAlias,
		CharSequence columns,
		CharSequence from,
		CharSequence as,
		CharSequence joinClause,
		CharSequence joinClauseWhere,
		CharSequence whereClause,
		CharSequence pagingOrderBy,
		CharSequence pagingClause,
		CharSequence orderClause,
		CharSequence... extras) {

		SQLPart inSubQuery = this.select(
			false,
			idAlias,
			from,
			as,
			"",
			this.where(whereClause, joinClauseWhere),
			pagingOrderBy,
			pagingClause
		);

		SQLPart whereInSubQuery = SQLPart.join(" ", idAlias, SQLPart.forPattern(DEFAULT_IN_PATTERN, "", inSubQuery));
		return this.select(lock, columns, from, as, joinClause, whereInSubQuery, orderClause, extras);
	}
}
