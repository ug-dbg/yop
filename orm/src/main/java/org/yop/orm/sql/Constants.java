package org.yop.orm.sql;

import java.lang.reflect.Field;
import java.sql.Statement;

/**
 * SQL constants.
 */
public class Constants {

	/** Classic SQL dot operator */
	public static final String DOT = ".";

	public static final String SHOW_SQL_PROPERTY = "yop.show_sql";

	/** alias components separator */
	public static final String SQL_SEPARATOR = System.getProperties().getProperty("yop.sql.separator", "→");

	/** The max length allowed for aliasing in SQL */
	public static final int SQL_ALIAS_MAX_LENGTH = Integer.valueOf(
		System.getProperties().getProperty("yop.alias.max.length", "40")
	);

	/** use sequences (Oracle style) : default to false */
	public static final boolean USE_SEQUENCES = Boolean.valueOf(
		System.getProperties().getProperty("yop.sql.sequences", "false")
	);

	/** Max number of parameters in a query */
	public static final Integer MAX_PARAMS = Integer.valueOf(
		System.getProperties().getProperty("yop.sql.max.parameters", "1000")
	);

	/**
	 * Some SQL drivers does not support {@link Statement#getGeneratedKeys()} with batches.
	 * <br>
	 * Namely :
	 * <ul>
	 *     <li>SQLite → no support, please set to false</li>
	 *     <li>MSSQL → no support, please set to false</li>
	 * </ul>
	 */
	public static final boolean USE_BATCH_INSERTS = Boolean.valueOf(
		System.getProperties().getProperty("yop.sql.batch_inserts", "true")
	);

	/**
	 * If you set a sequence to this constant, the sequence name will be calculated :
	 * <br>
	 * <b>"seq_" + {current_class#getSimpleName()}</b>
	 * <br><br>
	 * see {@link org.yop.orm.util.ORMUtil#readSequence(Field)}
	 * <br><br>
	 * The reason for this mechanism is to let you factorize the id field in an abstract class if you need.
	 */
	public static final String DEFAULT_SEQ = "→DEFAULT_SEQ←";
}
