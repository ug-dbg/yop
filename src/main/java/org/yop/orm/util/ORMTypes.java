package org.yop.orm.util;

import com.google.common.primitives.Primitives;

import java.util.HashMap;

/**
 * DBMS specificities.
 * <br>
 * This is pretty raw. This was written to prepare unit tests context.
 */
public class ORMTypes extends HashMap<Class<?>, String> {

	private String defaultType = "";

	public String getDefault() {
		return this.defaultType;
	}

	public String getColumnAttributes(boolean autoincrement, boolean primaryKey, int length) {
		return "";
	}

	public String getForType(Class<?> type) {
		Class<?> wrapped = Primitives.wrap(type);

		for (Entry<Class<?>, String> entry : this.entrySet()) {
			if(wrapped.isAssignableFrom(entry.getKey())) return entry.getValue();
		}

		return this.getDefault();
	}

	public ORMTypes(String defaultType) {
		this.defaultType = defaultType;
	}

	public static final ORMTypes SQLITE = new ORMTypes("TEXT") {
		{
			this.put(String.class,     "TEXT");
			this.put(Character.class,  "TEXT");

			this.put(Integer.class, "INTEGER");
			this.put(Long.class,    "INTEGER");
			this.put(Short.class,   "INTEGER");
			this.put(Byte.class,    "INTEGER");

			this.put(Float.class,  "REAL");
			this.put(Double.class, "REAL");
		}

		@Override
		public String getColumnAttributes(boolean autoincrement, boolean primaryKey, int length) {
			return primaryKey ? " NOT NULL PRIMARY KEY " : (autoincrement ? " AUTOINCREMENT " : "");
		}
	};
}
