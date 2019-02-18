package org.yop.orm.query.serialize.json;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link Gson} instance builder/holder.
 * <br>
 * You can register serializers and a custom builder then ask for the gson instance.
 * The Gson instance will only be built once.
 */
class GsonInstance {

	private List<ExclusionStrategy> exclusionStrategies = new ArrayList<>();

	/** GSON utility builder. The user can provide his own ! */
	private GsonBuilder builder = new GsonBuilder();

	/** Gson utility. It should have at least a {@link YopableStrategy}. */
	private Gson gson;

	/** Some custom serializers (e.g. for {@link java.time}) */
	private final Map<Type, JsonSerializer> serializers = new HashMap<>();

	/** Some custom deserializers (e.g. for {@link java.time}) */
	private final Map<Type, JsonDeserializer> deserializers = new HashMap<>();

	/**
	 * Default constructor. Everything is set later.
	 */
	GsonInstance() {}

	/**
	 * Use a custom GSON builder for this instance
	 * @param builder the custom builder
	 * @return the current instance, for chaining purposes
	 */
	GsonInstance customBuilder(GsonBuilder builder) {
		this.builder = builder == null ? this.builder : builder;
		return this;
	}

	/**
	 * Use the default exclusion strategy ({@link YopableStrategy}) for this instance.
	 * @return the current instance, for chaining purposes
	 */
	GsonInstance defaultExclusionStrategy() {
		this.exclusionStrategies.add(new YopableStrategy());
		return this;
	}

	/**
	 * Use the default serializers ({@link Serializers#ALL}) for this instance.
	 * @return the current instance, for chaining purposes
	 */
	GsonInstance defaultSerializers() {
		this.serializers.putAll(Serializers.ALL);
		return this;
	}

	/**
	 * Use the default deserializers ({@link DeSerializers#ALL}) for this instance.
	 * @return the current instance, for chaining purposes
	 */
	GsonInstance defaultDeserializers() {
		this.deserializers.putAll(DeSerializers.ALL);
		return this;
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
	 * Register a new deserializer for a given type.
	 * See {@link GsonBuilder#registerTypeAdapter(Type, Object)}.
	 * @param type       the data type for which the serializer is registered
	 * @param serializer the deserializer implementation
	 */
	void register(Type type, JsonDeserializer serializer) {
		this.deserializers.put(type, serializer);
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
	 * Create a new {@link GsonBuilder} with {@link YopableStrategy} and registered serializers/deserializers.
	 */
	private void configureBuilder() {
		this.builder.setExclusionStrategies(
			this.exclusionStrategies.toArray(new ExclusionStrategy[this.exclusionStrategies.size()])
		);
		this.serializers.forEach(this.builder::registerTypeAdapter);
		this.deserializers.forEach(this.builder::registerTypeAdapter);
	}
}
