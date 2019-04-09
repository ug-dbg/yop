package org.yop.orm.sql.dialect;

import org.yop.orm.gen.Column;
import org.yop.orm.sql.SQLExpression;

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
		return this.autoincrement(column) ? "" : super.type(column);
	}

	@Override
	public String toSQL(Column column) {
		return column.getName()
			+ " " + this.type(column) + " "
			+ (this.autoincrement(column) ? this.autoIncrementKeyWord() : "")
			+ (column.isNotNull() || (column.isNaturalKey() && ! this.nullInNK()) ? " NOT NULL " : "");
	}

	/**
	 * Is this column an autoincrement column ?
	 * @param column the column to check
	 * @return true if {@link Column#isPK()} AND {@link org.yop.orm.gen.PrimaryKey#autoincrement}
	 */
	private boolean autoincrement(Column column) {
		return column.isPK() && column.getPk().isAutoincrement();
	}

	@Override
	public SQLExpression select(
		boolean lock,
		CharSequence what,
		CharSequence from,
		CharSequence as,
		CharSequence joinClause,
		CharSequence whereClause,
		CharSequence orderClause,
		CharSequence... extras) {

		SQLExpression request = super.select(lock, what, from, as, joinClause, whereClause, orderClause, extras);
		return lock ? request.append("OF " + as) : request;
	}
}
