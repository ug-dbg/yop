package org.yop.orm.sql.dialect;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.gen.Column;
import org.yop.orm.gen.Table;
import org.yop.orm.util.MessageUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * MS-SQL (a.k.a SQL server) dialect {@link Dialect} extension.
 * @see <a href="https://www.microsoft.com/en-us/sql-server">https://www.microsoft.com/en-us/sql-server</a>
 */
public class MSSQL extends Dialect {

	public static final Dialect INSTANCE = new MSSQL();

	/**
	 * Default constructor. Please use singleton {@link #INSTANCE}.
	 */
	private MSSQL() {
		super("NVARCHAR");
		this.setForType(String.class,     "NVARCHAR");
		this.setForType(Character.class,  "NVARCHAR");

		this.setForType(Integer.class, "INTEGER");
		this.setForType(Long.class,    "BIGINT");
		this.setForType(Short.class,   "INTEGER");
		this.setForType(Byte.class,    "INTEGER");

		this.setForType(Float.class,  "REAL");
		this.setForType(Double.class, "REAL");
		this.setForType(Date.class,          "DATETIME");
		this.setForType(Calendar.class,      "DATETIME");
		this.setForType(Instant.class,       "DATETIME");
		this.setForType(LocalDate.class,     "DATE");
		this.setForType(LocalDateTime.class, "DATETIME");

		this.setForType(Time.class,               "DATETIME");
		this.setForType(java.sql.Date.class,      "DATE");
		this.setForType(java.sql.Timestamp.class, "DATETIME");

		this.setForType(BigInteger.class, "VARCHAR(MAX)");
		this.setForType(BigDecimal.class, "VARCHAR(MAX)");
	}

	@Override
	public String type(Column column) {
		String type = column.getType();
		return type + (StringUtils.endsWith(type, "VARCHAR") ? "(" + column.getLength() + ")" : "");
	}

	@Override
	public String autoIncrementKeyWord() {
		return " IDENTITY (1,1) ";
	}

	@Override
	public String toSQL(Table table) {
		Collection<String> elements = new ArrayList<>();
		elements.addAll(table.getColumns().stream().sorted().map(Column::toSQL).collect(Collectors.toList()));
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
		elements.addAll(this.toSQLNK(table));

		return MessageFormat.format(
			SQL.CREATE,
			table.qualifiedName(),
			MessageUtil.join(", ", elements)
		);
	}

	@Override
	public String pathSeparator() {
		return "$";
	}

	@Override
	public boolean useBatchInserts() {
		return false;
	}

	@Override
	public String selectAndLockPattern(boolean distinct) {
		String defaultPattern = super.selectPattern(distinct);
		return defaultPattern.replaceFirst(
			Pattern.quote(SQL.PARAM_TABLE_ALIAS),
			SQL.PARAM_TABLE_ALIAS + " WITH (updlock) "
		);
	}
}
