package org.yop.orm.util.dialect;

import org.yop.orm.gen.Column;
import org.yop.orm.gen.Table;
import org.yop.orm.util.ORMTypes;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Oracle DB dialect {@link ORMTypes} extension.
 * <br><br>
 * This has been somehow tested with Oracle XE 11g.
 * @see <a href="https://www.oracle.com/database/index.html">https://www.oracle.com/database/index.html</a>
 */
public class Oracle extends ORMTypes {

	private static final String SEQUENCE_SQL = "CREATE SEQUENCE {0} START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE";
	private static final String DROP_SEQUENCE_SQL = "DROP SEQUENCE {0}";

	public static final ORMTypes INSTANCE = new Oracle();

	/**
	 * Default constructor. Please use singleton {@link #INSTANCE}.
	 */
	private Oracle() {
		super("VARCHAR");
		this.put(String.class,     "VARCHAR");
		this.put(Character.class,  "VARCHAR");

		this.put(Integer.class, "NUMBER");
		this.put(Long.class,    "NUMBER");
		this.put(Short.class,   "NUMBER");
		this.put(Byte.class,    "NUMBER");
	}

	@Override
	protected String autoIncrementKeyWord() {
		return "";
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * Implémentation :
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
