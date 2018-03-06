package org.yop.orm.sql;

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
}
