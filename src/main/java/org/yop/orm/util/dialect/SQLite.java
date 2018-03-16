package org.yop.orm.util.dialect;

import org.yop.orm.gen.Column;
import org.yop.orm.gen.Table;
import org.yop.orm.util.MessageUtil;
import org.yop.orm.util.ORMTypes;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * SQLite dialect {@link ORMTypes} extension.
 * @see <a href="https://www.sqlite.org">https://www.sqlite.org</a>
 */
public class SQLite extends ORMTypes {

	public static final ORMTypes INSTANCE = new SQLite();

	/**
	 * Default constructor. Please use singleton {@link #INSTANCE}.
	 */
	private SQLite() {
		super("TEXT");
		this.put(String.class,     "TEXT");
		this.put(Character.class,  "TEXT");

		this.put(Integer.class, "INTEGER");
		this.put(Long.class,    "INTEGER");
		this.put(Short.class,   "INTEGER");
		this.put(Byte.class,    "INTEGER");

		this.put(Float.class,  "REAL");
		this.put(Double.class, "REAL");
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
			CREATE,
			table.qualifiedName(),
			MessageUtil.join(", ", elements)
		);
	}
}
