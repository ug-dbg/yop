package org.yop.orm.query.json;

import com.google.gson.JsonDeserializer;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Several GSON {@link JsonDeserializer} instances for several types, mostly java.time types.
 */
public class DeSerializers {

	private static final JsonDeserializer<LocalDateTime> LOCAL_DATE_TIME = forFunction(LocalDateTime::parse);
	private static final JsonDeserializer<LocalDate> LOCAL_DATE = forFunction(LocalDate::parse);
	private static final JsonDeserializer<LocalTime> LOCAL_TIME = forFunction(LocalTime::parse);
	private static final JsonDeserializer<Time> SQL_TIME = (json, targetType, c) -> new Time(json.getAsLong());

	private DeSerializers() {}

	/** All the known deserializers in a map where the key is the target class. */
	static final Map<Class, JsonDeserializer> ALL = new HashMap<Class, JsonDeserializer>() {{
		this.put(LocalDateTime.class, LOCAL_DATE_TIME);
		this.put(LocalDate.class,     LOCAL_DATE);
		this.put(LocalTime.class,     LOCAL_TIME);
		this.put(Time.class,          SQL_TIME);
	}};

	/**
	 * Create a GSON deserializer for a given lambda (String → T) function.
	 * @param function the deserialization function
	 * @param <T> the target type
	 * @return a new GSON deserializer instance for the given lambda.
	 */
	private static <T> JsonDeserializer<T> forFunction(Function<String, T> function) {
		return (json, typeOfT, context) -> function.apply(json.getAsString());
	}
}
