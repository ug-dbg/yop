package org.yop.orm.util.dialect;

import org.yop.orm.gen.Column;
import org.yop.orm.gen.Table;

import java.text.MessageFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Oracle DB dialect {@link Dialect} extension.
 * <br><br>
 * This has been somehow tested with Oracle XE 11g.
 * @see <a href="https://www.oracle.com/database/index.html">https://www.oracle.com/database/index.html</a>
 */
public class Oracle extends Dialect {

	private static final String SEQUENCE_SQL = "CREATE SEQUENCE {0} START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE";
	private static final String DROP_SEQUENCE_SQL = "DROP SEQUENCE {0}";

	public static final Dialect INSTANCE = new Oracle();

	/**
	 * Default constructor. Please use singleton {@link #INSTANCE}.
	 */
	private Oracle() {
		super("VARCHAR");
		this.setForType(String.class,     "VARCHAR");
		this.setForType(Character.class,  "VARCHAR");

		this.setForType(Double.class,  "BINARY_DOUBLE");
		this.setForType(Long.class,    "NUMBER");
		this.setForType(Integer.class, "NUMBER");
		this.setForType(Short.class,   "NUMBER");
		this.setForType(Byte.class,    "NUMBER");
		this.setForType(Boolean.class, "NUMBER(1)");

		this.setForType(LocalTime.class,     "TIMESTAMP");
		this.setForType(java.sql.Time.class, "TIMESTAMP");
	}

	@Override
	public String autoIncrementKeyWord() {
		return "";
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * Implementation :
	 * DROP and CREATE the sequences for the given table.
	 */
	@Override
	public List<String> otherSQL(Table table) {
		List<String> sequencesSQL = new ArrayList<>();
		for (Column column : table.getColumns()) {
			for (String sequence : column.getSequences()) {
				sequencesSQL.add(MessageFormat.format(DROP_SEQUENCE_SQL, sequence));
				sequencesSQL.add(MessageFormat.format(SEQUENCE_SQL, sequence));
			}
		}
		return sequencesSQL;
	}
}
