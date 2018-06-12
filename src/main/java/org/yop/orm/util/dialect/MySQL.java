package org.yop.orm.util.dialect;

import org.yop.orm.gen.Column;
import org.yop.orm.util.ORMTypes;

/**
 * MySQL dialect {@link ORMTypes} extension.
 * @see <a href="https://www.mysql.com/">https://www.mysql.com/</a>
 */
public class MySQL extends ORMTypes {

	public static final ORMTypes INSTANCE = new MySQL();
	public static final String CONNECT_PARAMS = "?useUnicode=true&characterEncoding=utf-8&rewriteBatchedStatements=true";

	/**
	 * Default constructor. Please use singleton {@link #INSTANCE}.
	 */
	private MySQL() {
		super("VARCHAR");
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * Deal with MySQL strict mode timestamp situation.
	 */
	@Override
	public String toSQL(Column column) {
		boolean autoincrement = column.getPk() != null && column.getPk().isAutoincrement();

		return column.getName()
			+ " " + this.type(column)
			+ this.nullable(column)
			+ (autoincrement ? this.autoIncrementKeyWord() : "");
	}

	/**
	 * Is this column nullable ?
	 * With MySQL, for now, there is a trick with timestamps (in strict mode).
	 * We set a timestamp to default 'NULL' if not a natural key or marked explicitly as 'not null'
	 * @param column the column to check.
	 * @return NULL / NOT NULL / ∅
	 */
	private String nullable(Column column) {
		if(column.isNotNull() || column.isNaturalKey()) {
			return " NOT NULL ";
		}
		if("TIMESTAMP".equals(column.getType())) {
			return " NULL ";
		}
		return " ";
	}
}
