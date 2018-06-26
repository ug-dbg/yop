package org.yop.orm.transform;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.Column;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * This transformer tries to handle some date/time data types that does not play well with some JDBC drivers.
 * <br>
 * It can deal with :
 * <ul>
 *     <li>{@link Date}</li>
 *     <li>{@link Calendar}</li>
 *     <li>{@link Instant}</li>
 * </ul>
 * The idea is :
 * <ul>
 *     <li>{@link #forSQL(Object, Column)} → convert these data types to {@link LocalDateTime} with default TZ</li>
 *     <li>{@link #fromSQL(Object, Class)} → convert to String, parse to Instant and assign</li>
 * </ul>
 * In this class, there are some workarounds for oracle and sqlite that should certainly be in specific transformers.
 * <br>
 * Actually, this transformer should probably be in the 'test' source directory.
 */
public class DateTimeLegacyTransformer implements ITransformer<Object> {

	private static final Logger logger = LoggerFactory.getLogger(DateTimeLegacyTransformer.class);

	/**
	 * Transform any Date/Calendar/Instant input into a {@link LocalDateTime} with local zone id.
	 */
	@Override
	public Object forSQL(Object what, Column column) {
		if (what instanceof Calendar) {
			TimeZone tz = ((Calendar) what).getTimeZone();
			ZoneId zid = tz == null ? ZoneId.systemDefault() : tz.toZoneId();
			return LocalDateTime.ofInstant(((Calendar) what).toInstant(), zid);
		}
		if (what instanceof Date) {
			return LocalDateTime.ofInstant(((Date) what).toInstant(), ZoneId.systemDefault());
		}
		if (what instanceof Instant) {
			return LocalDateTime.ofInstant((Instant) what, ZoneId.systemDefault());
		}
		return what;
	}

	/**
	 * Transform any java.sql data type into a parsable String,
	 * then parse using {@link Instant#parse(CharSequence)},
	 * then assign to the target data type.
	 */
	@Override
	public Object fromSQL(Object fromJDBC, Class<?> into) {
		if (fromJDBC instanceof Calendar || fromJDBC instanceof Instant || fromJDBC.getClass() == Date.class) {
			return fromJDBC;
		}

		if (! Calendar.class.isAssignableFrom(into)
		&& ! Date.class.isAssignableFrom(into)
		&& ! Instant.class.isAssignableFrom(into)) {
			return fromJDBC;
		}

		Object what = fromJDBC;

		if (what.getClass().getPackage().getName().startsWith("oracle")) {
			logger.debug("Super ugly Oracle workaround → invoke toJdbc method.");
			Method toJdbc = Reflection.getMethod(what.getClass(), "toJdbc");
			if (toJdbc != null) {
				try {
					what = toJdbc.invoke(what);
				} catch (ReflectiveOperationException e) {
					logger.warn("Could not invoke toJdbc method on oracle data type [{}]", what, e);
				}
			}
		}

		if(what instanceof java.sql.Timestamp) {
			if (LocalDateTime.class.isAssignableFrom(into)) {
				return ((Timestamp) what).toLocalDateTime();
			} else {
				what = ((Timestamp) what).toInstant().toString();
			}
		} else if(what instanceof java.sql.Date) {
			what = ((java.sql.Date) what).toLocalDate().atStartOfDay(ZoneOffset.systemDefault()).toInstant().toString();
		} else if(what instanceof java.sql.Time) {
			what = ((java.sql.Time) what).toLocalTime().toString();
		} else  if (fromJDBC instanceof String && !StringUtils.endsWith((String) fromJDBC, "Z")) {
			logger.debug("Super ugly SQLite workaround : String → LocalDateTime → @local → Instant → String. Sue me.");
			what = LocalDateTime.parse((CharSequence) fromJDBC).atZone(ZoneId.systemDefault()).toInstant().toString();
		}

		if (Instant.class.isAssignableFrom(into)) {
			return Instant.parse((CharSequence) what);
		}
		if (Date.class.isAssignableFrom(into)) {
			return new Date(Instant.parse((CharSequence) what).toEpochMilli());
		}
		if (Calendar.class.isAssignableFrom(into)) {
			Calendar instance = Calendar.getInstance();
			instance.setTimeInMillis(Instant.parse((CharSequence) what).toEpochMilli());
			return instance;
		}

		return fromJDBC;
	}
}
