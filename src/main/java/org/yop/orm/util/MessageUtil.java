package org.yop.orm.util;

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

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

	/**
	 * Concat objects as Strings using a separator.
	 * <br>
	 * Skip null or blanks
	 * @param strings the strings to concat.
	 * @return the resulting string.
	 */
	public static String join(String separator, String... strings){
		return join(separator, Arrays.asList(strings));
	}

	/**
	 * Concat objects as Strings using a separator.
	 * <br>
	 * Skip null or blanks
	 * @param strings the strings to concat.
	 * @return the resulting string.
	 */
	public static String join(String separator, Collection<String> strings){
		return Joiner.on(separator).skipNulls().join(
			strings.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList()
		));
	}
}
