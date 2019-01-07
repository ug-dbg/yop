package org.yop.orm.util.dialect;

import org.yop.orm.gen.Column;
import org.yop.orm.gen.Table;
import org.yop.orm.util.MessageUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * SQLite dialect {@link Dialect} extension.
 * @see <a href="https://www.sqlite.org">https://www.sqlite.org</a>
 */
public class SQLite extends Dialect {

	public static final Dialect INSTANCE = new SQLite();

	/**
	 * Default constructor. Please use singleton {@link #INSTANCE}.
	 */
	private SQLite() {
		super("TEXT");
		this.setForType(String.class,     "TEXT");
		this.setForType(Character.class,  "TEXT");

		this.setForType(Integer.class, "INTEGER");
		this.setForType(Long.class,    "INTEGER");
		this.setForType(Short.class,   "INTEGER");
		this.setForType(Byte.class,    "INTEGER");

		this.setForType(Float.class,  "REAL");
		this.setForType(Double.class, "REAL");
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
		elements.addAll(table.getColumns().stream().sorted().map(Column::toSQL).collect(Collectors.toList()));
		elements.addAll(table.getColumns().stream().map(this::toSQLFK).collect(Collectors.toList()));
		elements.addAll(this.toSQLNK(table));

		return MessageFormat.format(
			SQL.CREATE,
			table.qualifiedName(),
			MessageUtil.join(", ", elements)
		);
	}

	@Override
	public String selectAndLockPattern(boolean distinct) {
		throw new UnsupportedOperationException("SQLite does not support locking.");
	}
}
