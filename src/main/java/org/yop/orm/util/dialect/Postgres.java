package org.yop.orm.util.dialect;

import org.yop.orm.gen.Column;
import org.yop.orm.util.ORMTypes;

/**
 * Postgres dialect {@link ORMTypes} extension.
 * @see <a href="https://www.postgresql.org">https://www.postgresql.org</a>
 */
public class Postgres extends ORMTypes {

	public static final ORMTypes INSTANCE = new Postgres();

	private Postgres() {
		super("varchar");
		this.put(Boolean.class, "boolean");
	}

	@Override
	protected String autoIncrementKeyWord() {
		return " SERIAL ";
	}

	@Override
	protected String type(Column column) {
		return column.getPk() == null ? super.type(column) : "";
	}
}
