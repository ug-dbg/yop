package org.yop.orm.query.serialize.json;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Several GSON {@link JsonSerializer} instances for several types, mostly java.time types.
 */
public class Serializers {

	private static final JsonSerializer<?> TO_STRING = (src, targetType, c) -> new JsonPrimitive(src.toString());
	private static final JsonSerializer<Time> SQL_TIME = (src, targetType, c) -> new JsonPrimitive(src.getTime());

	private Serializers() {}

	/** All the known serializers in a map where the key is the source class. */
	static final Map<Class, JsonSerializer> ALL = new HashMap<Class, JsonSerializer>() {{
		this.put(LocalDateTime.class, TO_STRING);
		this.put(LocalDate.class,     TO_STRING);
		this.put(LocalTime.class,     TO_STRING);
		this.put(Time.class,          SQL_TIME);
	}};
}
