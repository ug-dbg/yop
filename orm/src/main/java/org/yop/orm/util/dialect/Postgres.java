package org.yop.orm.util.dialect;

import org.yop.orm.gen.Column;
import org.yop.orm.sql.SQLPart;

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

	@Override
	public SQLPart select(
		boolean lock,
		CharSequence what,
		CharSequence from,
		CharSequence as,
		CharSequence joinClause,
		CharSequence whereClause,
		CharSequence orderClause,
		CharSequence... extras) {

		SQLPart request = super.select(lock, what, from, as, joinClause, whereClause, orderClause, extras);
		return lock ? request.append("OF " + as) : request;
	}
}
