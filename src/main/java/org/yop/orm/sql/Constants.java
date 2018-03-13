package org.yop.orm.sql;

import java.lang.reflect.Field;

/**
 * SQL constants.
 */
public class Constants {

	/** Classic SQL dot operator */
	public static final String DOT = ".";

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
