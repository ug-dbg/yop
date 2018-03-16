package org.yop.orm.transform;

import com.google.common.primitives.Primitives;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.Column;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

/**
 * A fall back transformer is used when the {@link java.sql.ResultSet#getObject(int, Class)} fails.
 */
public class FallbackTransformer implements ITransformer<Object> {

	private static final Logger logger = LoggerFactory.getLogger(FallbackTransformer.class);

	/** Singleton instance */
	static final FallbackTransformer INSTANCE = new FallbackTransformer();

	/** Private constructor : please use {@link #INSTANCE} */
	private FallbackTransformer() {}

	/**
	 * Try to transform a given object into a target class, using some strategies.
	 * This mostly is to be used if the JDBC driver does not support {@link java.sql.ResultSet#getObject(int, Class)}.
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
	 * @param fromJDBC incoming object
	 * @param into     expected target type
	 * @return what transformed into... or not
	 */
	@Override
	public Object fromSQL(Object fromJDBC, Class<?> into) {
		Object what = fromJDBC;
		if(what == null || what.getClass().isAssignableFrom(into)) {
			return what;
		}

		if(String.class.equals(into)) {
			return String.valueOf(what);
		}

		if(what instanceof java.sql.Timestamp) {
			LocalDateTime localDateTime = ((Timestamp) what).toLocalDateTime();
			what = localDateTime.toString();
		}

		if(what instanceof java.sql.Date) {
			LocalDate localDate = ((java.sql.Date) what).toLocalDate();
			what = localDate.toString();
		}

		if(what instanceof java.sql.Time) {
			LocalTime localTime = ((java.sql.Time) what).toLocalTime();
			what = localTime.toString();
		}

		if(what instanceof String) {
			if(Boolean.class.isAssignableFrom(Primitives.wrap(into))) {
				return Boolean.valueOf((String) what);
			}
			if (Instant.class.isAssignableFrom(into)) {
				return Instant.parse((CharSequence) what);
			}
			if (LocalDate.class.isAssignableFrom(into)) {
				return LocalDate.parse((CharSequence) what);
			}
			if (LocalDateTime.class.isAssignableFrom(into)) {
				return LocalDateTime.parse((CharSequence) what);
			}
			if (Date.class.isAssignableFrom(into)) {
				return new Date(Instant.parse((CharSequence) what).toEpochMilli());
			}
			if (Calendar.class.isAssignableFrom(into)) {
				Calendar instance = Calendar.getInstance();
				instance.setTime(new Date(Instant.parse((CharSequence) what).toEpochMilli()));
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

		try {
			Constructor<?> constructor = into.getDeclaredConstructor(what.getClass());
			return constructor.newInstance(what);
		} catch (NoSuchMethodException e) {
			logger.trace("Could not find valueOf(String) on [" + into.getName() + "]", e);
		} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
			logger.trace("Could not invoke valueOf(String) on [" + into.getName() + "]", e);
		}

		return what;
	}

	/**
	 * Does nothing. Return 'what' as is.
	 * <br><br>
	 * {@inheritDoc}
	 */
	@Override
	public Object forSQL(Object what, Column column) {
		return what;
	}
}
