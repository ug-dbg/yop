package org.yop.orm.query.serialize.json;

import com.google.gson.*;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.IJoin;
import org.yop.orm.query.serialize.Serialize;
import org.yop.orm.util.JoinUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JSON : serialize a {@link Yopable} to JSON and manage the relation to serialize at runtime.
 * <br><br>
 * Its API aims at being similar to the {@link org.yop.orm.query} APIs.
 * <br>
 * This directive's default setting is to skip {@link JoinTable} annotated relations. (see {@link YopableStrategy}).
 * <br>
 * You can set the join relations you want to be serialized : {@link #join(IJoin)}.
 * <br>
 * Example :
 * <pre>
 * {@code
 * JSON
 *   .from(Pojo.class)
 *   .joinAll()
 *   .join(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo)))
 *   .join(JoinSet.to(Pojo::getOthers).join(JoinSet.to(Other::getPojos)))
 *   .onto(pojo)
 *   .toJSON();
 * }
 * </pre>
 * This API relies on google's {@link Gson}.
 * <br>
 * You can set some custom {@link JsonSerializer} using {@link #register(Type, JsonSerializer)}.
 * @param <T> the target Yopable type to serialize to JSON.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class JSON<T extends Yopable> implements Serialize<JSON, T> {

	private final GsonInstance gson = new GsonInstance();

	/** The target Yopable class to serialize to JSON */
	private Class<T> target;

	/** The Yopable elements to serialize */
	private List<T> elements = new ArrayList<>();

	/** Join clauses */
	final Collection<IJoin<T, ? extends Yopable>> joins = new ArrayList<>();

	final Map<IJoin, Field> fieldCache = new HashMap<>();

	/**
	 * Private constructor. Set the target and default serializers for {@link java.time} types.
	 * @param target the target class to json-ify
	 */
	private JSON(Class<T> target) {
		this.target = target;
		this.gson.defaultExclusionStrategy().defaultSerializers().register(YopableForJSON.class, new Serializer());
	}

	/**
	 * Create a new {@link JSON} serialization directive, for the given target
	 * @param target the target Yopable class
	 * @param <T> the target Yopable type
	 * @return a new {@link JSON} directive
	 */
	public static <T extends Yopable> JSON<T> from(Class<T> target) {
		return new JSON<>(target);
	}

	/**
	 * Deserialize a JSON array of Yopables.
	 * @param target   the target type of the Yopables
	 * @param elements the JSON array of serialized Yopables
	 * @param <T> the target type
	 * @return a collection of T which are the deserialized elements
	 */
	public static <T extends Yopable> Collection<T> deserialize(Class<T> target, JsonArray elements) {
		GsonInstance instance = new GsonInstance().defaultDeserializers();
		instance.customBuilder(new GsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC));
		Gson gson = instance.instance();
		Collection<T> out = new ArrayList<>();
		for (JsonElement element : elements) {
			out.add(gson.fromJson(element, target));
		}
		return out;
	}

	/**
	 * Deserialize a JSON object of a serialized Yopable.
	 * @param target   the target type of the Yopable
	 * @param element  the JSON object serialized Yopable
	 * @param <T> the target type
	 * @return an instance of T that is the element, deserialized as T.
	 */
	public static <T extends Yopable> T deserialize(Class<T> target, JsonObject element) {
		GsonInstance instance = new GsonInstance()
			.defaultDeserializers()
			.customBuilder(new GsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC));
		Gson gson = instance.instance();
		return gson.fromJson(element, target);
	}

	/**
	 * Deserialize a JSON element of a serialized Yopable.
	 * @param target   the target type of the Yopable
	 * @param element  the JSON object/array serialized Yopable
	 * @param <T> the target type
	 * @return  a collection of T which are the deserialized elements
	 */
	public static <T extends Yopable> Collection<T> deserialize(Class<T> target, JsonElement element) {
		if (element.isJsonArray()) {
			return JSON.deserialize(target, element.getAsJsonArray());
		} else {
			return Collections.singletonList(JSON.deserialize(target, element.getAsJsonObject()));
		}
	}

	/**
	 * Provide the JSON directive with your own GSON builder.
	 * <br>
	 * {@link YopableStrategy} and custom serializers will be set on this builder first.
	 * @param builder the GSON builder you want to use
	 * @return the current JSON directive, for chaining purposes
	 */
	public JSON<T> withBuilder(GsonBuilder builder) {
		this.gson.customBuilder(builder);
		return this;
	}

	@Override
	public JSON<T> onto(T element) {
		this.elements.add(element);
		return this;
	}

	@Override
	public JSON<T> onto(Collection<T> elements) {
		this.elements.addAll(elements);
		return this;
	}

	@Override
	public <R extends Yopable> JSON<T> join(IJoin<T, R> join) {
		this.joins.add(join);
		return this;
	}

	@Override
	public JSON<T> join(Collection<IJoin<T, ?>> joins) {
		this.joins.addAll(joins);
		return this;
	}

	@Override
	public JSON<T> joinAll() {
		this.joins.clear();
		JoinUtil.joinAll(this.target, this.joins);
		return this;
	}

	@Override
	public JSON<T> joinProfiles(String... profiles) {
		if (profiles.length > 0) {
			JoinUtil.joinProfiles(this.target, this.joins, profiles);
		}
		return this;
	}

	/**
	 * Register a new serializer for a given type.
	 * See {@link GsonBuilder#registerTypeAdapter(Type, Object)}.
	 * @param type       the data type for which the serializer is registered
	 * @param serializer the serializer implementation
	 * @return the current JSON directive, for chaining purpose
	 */
	public JSON<T> register(Type type, JsonSerializer serializer) {
		this.gson.register(type, serializer);
		return this;
	}

	/**
	 * Execute the directive : serialize to JSON, as String.
	 * <br>
	 * <b>⚠ {@link #elements} are ignored ! ⚠</b>
	 * @param singleElement the single Yopable element to serialize
	 * @return a JSON string (singleElement, serialized as JSON Object)
	 */
	public String toJSON(Yopable singleElement) {
		return this.gson.instance().toJson(this.toJSONTree(singleElement));
	}

	/**
	 * Execute the directive : serialize {@link #elements} to JSON, as String
	 * @return a JSON string ({@link #elements}, serialized as JSON Array)
	 */
	public String toJSON() {
		return this.gson.instance().toJson(this.toJSONTree());
	}

	/**
	 * Execute the directive : serialize the given to JSON, as GSON {@link JsonElement}.
	 * <br>
	 * <b>⚠ {@link #elements} are ignored ! ⚠</b>
	 * @return a GSON JSON element (JSON object)
	 */
	private JsonElement toJSONTree(Yopable singleElement) {
		return this.gson.instance().toJsonTree(YopableForJSON.create(singleElement, this));
	}

	/**
	 * Execute the directive : serialize {@link #elements} to JSON, as GSON {@link JsonElement}
	 * @return a GSON JSON element (JSON array)
	 */
	public JsonElement toJSONTree() {
		return this.gson.instance().toJsonTree(
			this.elements.stream().map(e -> YopableForJSON.create(e, this)).collect(Collectors.toList())
		);
	}

	@Override
	public String execute() {
		return this.toJSON();
	}

	/**
	 * A custom serializer that can follow the {@link #joins} directives.
	 * <br>
	 * Use with {@link YopableStrategy}.
	 */
	private class Serializer implements JsonSerializer<YopableForJSON> {

		/**
		 * {@inheritDoc}
		 * <br>
		 * <b>Implementation details : </b>
		 * <br>
		 * Serialize the current object {@link YopableForJSON#subject} using {@link Gson} and {@link YopableStrategy}.
		 * No {@link JoinTable} field gets serialized.
		 * <br>
		 * For each join directive in {@link #joins} :
		 * <ul>
		 *     <li>find the target field, type and values</li>
		 *     <li>append a new property with the field name</li>
		 *     <li>create a new {@link YopableForJSON} for the target elements</li>
		 *     <li>use the context to serialize the next target elements</li>
		 * </ul>
		 */
		@Override
		@SuppressWarnings("unchecked")
		public JsonElement serialize(YopableForJSON src, Type typeOfSrc, JsonSerializationContext context) {
			// Serialize the given element to JSON. The YopableStrategy excludes @JoinTable properties
			JsonElement element = JSON.this.gson.instance().toJsonTree(src.getSubject());

			// For each join IJoin relation, serialize using the context.
			// This should recursively call the current method
			for (IJoin join : src.getJoins()) {
				Field field = src.getField(join);
				element.getAsJsonObject().add(field.getName(), context.serialize(src.next(join)));
			}

			return element;
		}
	}
}