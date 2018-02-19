package org.yop.orm.util;

/**
 * Utility class for message formatting. <br>
 * Created by ugz on 10/03/15.
 */
public class MessageUtil {

	/**
	 * Concat strings using an underlying StringBuilder. <br>
	 * @param strings the strings to concat.
	 * @return the resulting string.
	 */
	public static String concat(String... strings){
		StringBuilder builder = new StringBuilder();
		for(String string : strings){
			builder.append(string);
		}
		return builder.toString();
	}

	/**
	 * Concat objects as Strings using an underlying StringBuilder. <br>
	 * @param objects the strings to concat.
	 * @return the resulting string.
	 */
	public static String concat(Object... objects){
		StringBuilder builder = new StringBuilder();
		for(Object o : objects){
			builder.append(String.valueOf(o));
		}
		return builder.toString();
	}
}
