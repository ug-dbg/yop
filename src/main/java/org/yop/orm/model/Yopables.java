package org.yop.orm.model;

import com.google.gson.*;
import org.yop.orm.query.Context;
import org.yop.orm.query.IJoin;
import org.yop.orm.query.JsonAble;
import org.yop.orm.query.json.JSON;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A collection of Yopable that is easily serializable to/from JSON.
 * @param <T> the Yopable type of the collection.
 */
public class Yopables<T extends Yopable> extends ArrayList<T> implements JsonAble {

	/** Join clauses (JSON query serialization) */
	protected IJoin.Joins<T> joins;

	private Yopables() {}

	public Yopables(IJoin.Joins<T> serializationJoins) {
		this();
		this.joins = serializationJoins == null ? new IJoin.Joins<>() : serializationJoins;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <U extends Yopable> void fromJSON(Context<U> context, JsonElement elements) {
		GsonBuilder builder = new GsonBuilder().addDeserializationExclusionStrategy(new ExclusionStrategy() {
			@Override
			public boolean shouldSkipField(FieldAttributes f) {
				return false;
			}

			@Override
			public boolean shouldSkipClass(Class<?> clazz) {
				return false;
			}
		});

		builder.excludeFieldsWithModifiers(Modifier.STATIC);
		register(builder, LocalDateTime.class, (json, targetType, c) -> LocalDateTime.parse(json.getAsString()));
		register(builder, LocalDate.class,     (json, targetType, c) -> LocalDate.parse(json.getAsString()));
		register(builder, LocalTime.class,     (json, targetType, c) -> LocalTime.parse(json.getAsString()));
		register(builder, Time.class,          (json, targetType, c) -> new Time(json.getAsLong()));

		Gson gson = builder.create();
		for (JsonElement element : elements.getAsJsonArray()) {
			this.add(gson.fromJson(element, (Class<T>) context.getTarget()));
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <U extends Yopable> JsonElement toJSON(Context<U> context) {
		return JSON.from(context.getTarget()).onto((Collection) this).join(this.joins).toJSONTree();
	}

	private static void register(GsonBuilder builder, Class<?> clazz, JsonDeserializer deserializer) {
		builder.registerTypeAdapter(clazz, deserializer);
	}
}
