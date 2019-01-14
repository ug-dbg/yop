package org.yop.orm.sql.dialect;

import org.yop.orm.util.MessageUtil;

/**
 * Some SQL keywords and patterns.
 */
@SuppressWarnings("WeakerAccess")
abstract class SQL {

	static final String CREATE = " CREATE TABLE {0} ({1}) ";
	static final String DROP = " DROP TABLE {0} ";
	static final String PK = " CONSTRAINT {0} PRIMARY KEY ({1}) ";
	static final String FK = " CONSTRAINT {0} FOREIGN KEY ({1}) REFERENCES {2}({3}) ON DELETE CASCADE ";
	static final String NK = " CONSTRAINT {0} UNIQUE ({1}) ";

	static final String PARAM_COLUMN       = "{:column}";
	static final String PARAM_COLUMNS      = "{:columns}";
	static final String PARAM_VALUE        = "{:value}";
	static final String PARAM_VALUES       = "{:values}";
	static final String PARAM_TABLE        = "{:table}";
	static final String PARAM_TABLE_ALIAS  = "{:table_alias}";
	static final String PARAM_JOINS        = "{:joins}";
	static final String PARAM_WHERE        = "{:where}";
	static final String PARAM_ORDER_BY     = "{:order_by}";
	static final String PARAM_COL_AND_VALS = "{:column_and_values}";
	static final String PARAM_ID_COL_EQ    = "{:idcolumn_equals}";
	static final String PARAM_SUB_SEL      = "{:sub_select}";
	static final String PARAM_ANY          = "{:any}";

	static final String SELECT     = "SELECT";
	static final String INSERT     = "INSERT";
	static final String DELETE     = "DELETE";
	static final String UPDATE     = "UPDATE";

	static final String DISTINCT   = "DISTINCT";
	static final String EXISTS     = "EXISTS";
	static final String INTO       = "INTO";
	static final String FROM       = "FROM";
	static final String WHERE      = "WHERE";
	static final String FOR_UPDATE = "FOR UPDATE";
	static final String COUNT      = "COUNT";
	static final String IN         = "IN";
	static final String ON         = "ON";
	static final String AND        = "AND";
	static final String SET        = "SET";
	static final String VALUES     = "VALUES";
	static final String EQ         = "=";

	/** Select [what] FROM [table] [table_alias] [join clause] WHERE [where clause] [order by clause] [extra] */
	static final String DEFAULT_SELECT_PATTERN = join(
		SELECT,
		PARAM_COLUMNS,
		FROM,
		PARAM_TABLE,
		PARAM_TABLE_ALIAS,
		PARAM_JOINS,
		WHERE,
		PARAM_WHERE,
		PARAM_ORDER_BY,
		PARAM_ANY
	);

	/** Select [what] FROM [table] [table_alias] [join clause] WHERE [where clause] [order by clause] [extra] FOR UPDATE */
	static final String DEFAULT_SELECT_FOR_UPDATE_PATTERN = join(
		SELECT,
		PARAM_COLUMNS,
		FROM,
		PARAM_TABLE,
		PARAM_TABLE_ALIAS,
		PARAM_JOINS,
		WHERE,
		PARAM_WHERE,
		PARAM_ORDER_BY,
		PARAM_ANY,
		FOR_UPDATE
	);

	/** Select distinct([what]) FROM [table] [table_alias] [join clause] WHERE [where clause] [order by clause] [extra] */
	static final String DEFAULT_SELECT_DISTINCT_PATTERN = join(
		SELECT,
		DISTINCT,
		"(",
		PARAM_COLUMN,
		")",
		FROM,
		PARAM_TABLE,
		PARAM_TABLE_ALIAS,
		PARAM_JOINS,
		WHERE,
		PARAM_WHERE,
		PARAM_ORDER_BY,
		PARAM_ANY
	);

	/** Select distinct([what]) FROM [table] [table_alias] [join clause] WHERE [where clause] [order by clause] [extra] */
	static final String DEFAULT_SELECT_DISTINCT_FOR_UPDATE_PATTERN = join(
		SELECT,
		DISTINCT,
		"(",
		PARAM_COLUMN,
		")",
		FROM,
		PARAM_TABLE,
		PARAM_TABLE_ALIAS,
		PARAM_JOINS,
		WHERE,
		PARAM_WHERE,
		PARAM_ORDER_BY,
		PARAM_ANY,
		FOR_UPDATE
	);

	/** COUNT(DISTINCT :idColumn) column selection */
	static final String DEFAULT_COUNT_DISTINCT_PATTERN = join(
		COUNT + "(",
		DISTINCT,
		PARAM_COLUMN,
		")"
	);

	/** DELETE [columns] FROM [table] [join clauses] WHERE [where clause] */
	static final String DEFAULT_DELETE_PATTERN = join(
		DELETE,
		PARAM_COLUMNS,
		FROM,
		PARAM_TABLE,
		PARAM_JOINS,
		WHERE,
		PARAM_WHERE
	);

	/** DELETE FROM [table] WHERE [column] IN ([values]) */
	static final String DEFAULT_DELETE_IN_PATTERN = join(
		DELETE,
		FROM,
		PARAM_TABLE,
		WHERE,
		PARAM_COLUMN,
		IN,
		"(",
		PARAM_VALUES,
		")"
	);

	/** INSERT INTO [table] ([columns]) VALUES ([column values]) */
	static final String DEFAULT_INSERT_PATTERN = join(
		INSERT,
		INTO,
		PARAM_TABLE,
		"(",
		PARAM_COLUMNS,
		")",
		VALUES,
		"(",
		PARAM_VALUES,
		")"
	);

	/** UPDATE [table] SET ([column=value]+) WHERE ([idColumn] = ? ) */
	static final String DEFAULT_UPDATE_PATTERN = join(
		UPDATE,
		PARAM_TABLE,
		SET,
		PARAM_COL_AND_VALS,
		WHERE,
		"(",
		PARAM_ID_COL_EQ,
		")"
	);

	/** [table] [table alias] on [column name] = [value]. Then prefix with the join type. */
	static final String DEFAULT_JOIN_ON_PATTERN = join(
		PARAM_TABLE,
		PARAM_TABLE_ALIAS,
		ON,
		PARAM_COLUMN,
		EQ,
		PARAM_VALUE
	);

	/** [column] IN ([comma separated values]) */
	static final String DEFAULT_IN_PATTERN = join(
		PARAM_COLUMN,
		IN,
		"(",
		PARAM_VALUES,
		")"
	);

	/** EXISTS ([sub-select]) */
	static final String DEFAULT_EXISTS_PATTERN = join(
		EXISTS,
		"(",
		PARAM_SUB_SEL,
		")"
	);

	/** [a] = [b] */
	static final String DEFAULT_EQUALS_PATTERN = "{:a} = {:b}";

	/** Default where clause is always added. So I don't have to check if the 'WHERE' keyword is required ;-) */
	static final String DEFAULT_WHERE = " 1=1 ";

	/**
	 * Join using space separator.
	 * @param elements the elements to join
	 * @return the joined element, space separated
	 */
	private static String join(String... elements) {
		return MessageUtil.join(" ", elements);
	}

}
