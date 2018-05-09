package org.yop.orm.query.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link Gson} instance builder/holder.
 * <br>
 * You can register serializers and a custom builder then ask for the gson instance.
 * The Gson instance will only be built once.
 */
class GsonInstance {

	/** GSON utility builder. The user can provide his own ! */
	private GsonBuilder builder = new GsonBuilder();

	/** Gson utility. It should have at least a {@link YopableStrategy}. */
	private Gson gson;

	/** Some custom serializers (e.g. for {@link java.time}) */
	private final Map<Type, JsonSerializer> serializers = new HashMap<>();

	/**
	 * Default constructor. Everything is set later.
	 */
	GsonInstance() {}

	void customBuilder(GsonBuilder builder) {
		this.builder = builder == null ? this.builder : builder;
	}

	/**
	 * Register a new serializer for a given type.
	 * See {@link GsonBuilder#registerTypeAdapter(Type, Object)}.
	 * @param type       the data type for which the serializer is registered
	 * @param serializer the serializer implementation
	 */
	void register(Type type, JsonSerializer serializer) {
		this.serializers.put(type, serializer);
	}

	/**
	 * Give me the {@link Gson} instance!
	 * <br>
	 * If already built, the current instance is returned. Else an instance is built and returned.
	 * <br>
	 * It means you cannot register any {@link JsonSerializer} or change the {@link GsonBuilder} after this.
	 * @return the {@link Gson} instance
	 */
	Gson instance() {
		if (this.gson == null) {
			this.configureBuilder();
			this.gson = this.builder.create();
		}
		return this.gson;
	}

	/**
	 * Create a new {@link GsonBuilder} with {@link YopableStrategy} and registered serializers.
	 */
	private void configureBuilder() {
		this.builder.setExclusionStrategies(new YopableStrategy());
		this.serializers.forEach(this.builder::registerTypeAdapter);
	}
}
