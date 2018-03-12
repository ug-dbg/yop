package org.yop.orm.util.dialect;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.gen.Column;
import org.yop.orm.gen.Table;
import org.yop.orm.util.MessageUtil;
import org.yop.orm.util.ORMTypes;

import java.sql.Time;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MS-SQL (a.k.a SQL server) dialect {@link ORMTypes} extension.
 * @see <a href="https://www.microsoft.com/en-us/sql-server">https://www.microsoft.com/en-us/sql-server</a>
 */
public class MSSQL extends ORMTypes {

	public static final ORMTypes INSTANCE = new MSSQL();

	/**
	 * Default constructor. Please use singleton {@link #INSTANCE}.
	 */
	private MSSQL() {
		super("NVARCHAR");
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
}
