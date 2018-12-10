package org.yop.orm.util.dialect;

import com.google.common.primitives.Primitives;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.JDBCType;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

	@Override
	public Class<?> getForType(int sqlType) {
		String typeName = JDBCType.valueOf(sqlType).getName();
		return this.types.entrySet()
			.stream()
			.filter(e -> typeName.equals(e.getValue()))
			.findFirst()
			.map(Map.Entry::getKey)
			.orElse(Object.class);
	}

	/**
	 * Default constructor, with default type.
	 * Add basic types matching.
	 * <br>
	 * This method uses the generic types from {@link JDBCType}.
	 * @param defaultType the {@link #defaultType} to set.
	 */
	Dialect(String defaultType) {
		this.defaultType = defaultType;
		this.types.put(Boolean.class, JDBCType.BIT.getName());

		this.types.put(String.class,    JDBCType.VARCHAR.getName());
		this.types.put(Character.class, JDBCType.CHAR.getName());

		this.types.put(Integer.class, JDBCType.INTEGER.getName());
		this.types.put(Long.class,    JDBCType.BIGINT.getName());
		this.types.put(Short.class,   JDBCType.SMALLINT.getName());
		this.types.put(Byte.class,    JDBCType.INTEGER.getName());

		this.types.put(Float.class,  JDBCType.REAL.getName());
		this.types.put(Double.class, JDBCType.DOUBLE.getName());

		this.types.put(BigDecimal.class, JDBCType.VARCHAR.getName());
		this.types.put(BigInteger.class, JDBCType.VARCHAR.getName());

		this.types.put(Date.class,          JDBCType.TIMESTAMP.getName());
		this.types.put(Calendar.class,      JDBCType.TIMESTAMP.getName());
		this.types.put(Instant.class,       JDBCType.TIMESTAMP.getName());
		this.types.put(LocalTime.class,     JDBCType.TIME.getName());
		this.types.put(LocalDate.class,     JDBCType.DATE.getName());
		this.types.put(LocalDateTime.class, JDBCType.TIMESTAMP.getName());

		this.types.put(Time.class,               JDBCType.TIME.getName());
		this.types.put(java.sql.Date.class,      JDBCType.DATE.getName());
		this.types.put(java.sql.Timestamp.class, JDBCType.TIMESTAMP.getName());

		this.types.put(Byte[].class, JDBCType.BINARY.getName());
		this.types.put(byte[].class, JDBCType.BINARY.getName());
	}
}
