package org.yop.orm.util.dialect;

import com.google.common.primitives.Primitives;

import java.sql.Time;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * DBMS dialect types.
 * <br>
 * This is a {@link IDialect} with an {@link HashMap} to store the Java type → SQL type mappings.
 * <br>
 * It has a default mapping for basic types. See {@link #Dialect(String)}.
 */
abstract class Dialect implements IDialect {

	/** Java type class → SQL type */
	private Map<Class, String> types = new HashMap<>();

	/** The default SQL type */
	private final String defaultType;

	/**
	 * @return the default SQL type → {@link #defaultType}
	 */
	@Override
	public String getDefault() {
		return this.defaultType;
	}

	@Override
	public void setForType(Class clazz, String type) {
		this.types.put(clazz, type);
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * 2 passes : exact match, then 'assignable from'.
	 * <br>
	 * (java.sql.Date is assignable from java.util.Date)
	 */
	@Override
	public String getForType(Class<?> type) {
		Class<?> wrapped = Primitives.wrap(type);

		// first pass : exact match.
		for (Map.Entry<Class, String> entry : this.types.entrySet()) {
			if(wrapped.equals(entry.getKey())) return entry.getValue();
		}

		// second pass : 'assignable from' is OK.
		for (Map.Entry<Class, String> entry : this.types.entrySet()) {
			if(wrapped.isAssignableFrom(entry.getKey())) return entry.getValue();
		}

		return this.getDefault();
	}

	/**
	 * Default constructor, with default type.
	 * Add basic types matching.
	 * @param defaultType the {@link #defaultType} to set.
	 */
	Dialect(String defaultType) {
		this.defaultType = defaultType;
		this.types.put(String.class,     "VARCHAR");
		this.types.put(Character.class,  "VARCHAR");

		this.types.put(Integer.class, "INTEGER");
		this.types.put(Long.class,    "BIGINT");
		this.types.put(Short.class,   "INTEGER");
		this.types.put(Byte.class,    "INTEGER");

		this.types.put(Float.class,  "REAL");
		this.types.put(Double.class, "REAL");

		this.types.put(Date.class,          "TIMESTAMP");
		this.types.put(Calendar.class,      "TIMESTAMP");
		this.types.put(Instant.class,       "TIMESTAMP");
		this.types.put(LocalTime.class,     "TIME");
		this.types.put(LocalDate.class,     "DATE");
		this.types.put(LocalDateTime.class, "TIMESTAMP");

		this.types.put(Time.class,               "TIME");
		this.types.put(java.sql.Date.class,      "DATE");
		this.types.put(java.sql.Timestamp.class, "TIMESTAMP");
	}
}
