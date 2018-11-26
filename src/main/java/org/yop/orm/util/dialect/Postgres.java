package org.yop.orm.util.dialect;

import org.yop.orm.gen.Column;

/**
 * Postgres dialect {@link Dialect} extension.
 * @see <a href="https://www.postgresql.org">https://www.postgresql.org</a>
 */
public class Postgres extends Dialect {

	public static final Dialect INSTANCE = new Postgres();

	/**
	 * Default constructor. Please use singleton {@link #INSTANCE}.
	 */
	private Postgres() {
		super("varchar");
		this.setForType(Boolean.class, "boolean");
	}

	@Override
	public String autoIncrementKeyWord() {
		return " SERIAL ";
	}

	@Override
	public String type(Column column) {
		return column.getPk() == null ? super.type(column) : "";
	}
}
