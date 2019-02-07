package org.yop.orm.util;

import com.google.common.primitives.Primitives;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.reflection.Reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Utility methods to transform objects into something else, mostly for data mapping purposes.
 */
public class TransformUtil {

	private static final Logger logger = LoggerFactory.getLogger(TransformUtil.class);

	/**
	 * Naive generic method to transform an object into something else, using some strategies.
	 * This mostly is to be used if the JDBC driver does not support {@link java.sql.ResultSet#getObject(int, Class)}.
	 * <br>
	 * Let's say 'what' is the input from the JDBC driver :
	 * <ol>
	 *     <li>what is null or what is already the target type : do nothing</li>
	 *     <li>what is java.sql → convert to string</li>
	 *     <li>target type is String : use {@link String#valueOf(Object)}</li>
	 *     <li>what is string and target type is some java.time class → parse !</li>
	 *     <li>try to cast</li>
	 *     <li>try to use a 'valueOf' method</li>
	 *     <li>try to use a constructor method</li>
	 *     <li>return what as is</li>
	 * </ol>
	 * @param from incoming object
	 * @param into expected target type
	 * @return what transformed into... or not
	 */
	public static Object transform(Object from, Class<?> into) {
		Object what = from;
		if(what == null || what.getClass().isAssignableFrom(into)) {
			return what;
		}

		if (Boolean.class.equals(Primitives.wrap(into))) {
			return BooleanUtils.toBoolean(String.valueOf(what)) || "1".equals(String.valueOf(what));
		}

		if(String.class.equals(into)) {
			return String.valueOf(what);
		}

		if(what instanceof java.sql.Timestamp) {
			if (LocalDateTime.class.isAssignableFrom(into)) {
				return ((Timestamp) what).toLocalDateTime();
			} else {
				what = ((Timestamp) what).toInstant().toString();
			}
		}

		if(what instanceof java.sql.Date) {
			what = ((java.sql.Date) what).toLocalDate().toString();
		}

		if(what instanceof java.sql.Time) {
			what = ((java.sql.Time) what).toLocalTime().toString();
		}

		if(what instanceof String) {
			if (LocalTime.class.isAssignableFrom(into)) {
				return LocalTime.parse((CharSequence) what);
			}
			if (LocalDate.class.isAssignableFrom(into)) {
				return LocalDate.parse((CharSequence) what);
			}
			if (LocalDateTime.class.isAssignableFrom(into)) {
				return LocalDateTime.parse((CharSequence) what);
			}
			if (BigDecimal.class.isAssignableFrom(into)) {
				// Oracle seems to store a BigDecimal (as VARCHAR) with "," instead of "."
				return new BigDecimal(((String) what).replace(",", "."));
			}
		}

		try {
			return into.cast(what);
		} catch (ClassCastException e) {
			logger.trace("Could not cast[" + what + "] into [" + into.getName() + "]", e);
		}

		try {
			Method valueOf = Primitives.wrap(into).getDeclaredMethod("valueOf", String.class);
			return valueOf.invoke(null, String.valueOf(what));
		} catch (NoSuchMethodException e) {
			logger.trace("Could not find valueOf(String) on [" + into.getName() + "]", e);
		} catch (IllegalAccessException | InvocationTargetException e) {
			logger.trace("Could not invoke valueOf(String) on [" + into.getName() + "]", e);
		}

		Constructor<?> constructor = Reflection.getConstructor(into, Primitives.wrap(what.getClass()));
		if(constructor == null) {
			constructor = Reflection.getConstructor(into, Primitives.unwrap(what.getClass()));
		}
		if(constructor != null) {
			try {
				return constructor.newInstance(what);
			} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
				logger.trace(
					"Could not invoke constructor [{}] on param [{}]",
					String.valueOf(constructor),
					String.valueOf(what),
					e
				);
			}
		}

		return from;
	}

}
