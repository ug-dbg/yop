package org.yop.orm.query.json;

import com.google.gson.*;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.IJoin;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
 * You can also add {@link #ID_SUFFIX} suffixed keys in the generated JSON
 * for the related objects IDs : {@link #joinIDs(IJoin)}.
 * <br>
 * Example :
 * <pre>
 * {@code
 * JSON
 *   .from(Pojo.class)
 *   .joinAll()
 *   .joinIDs(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo)))
 *   .joinIDs(JoinSet.to(Pojo::getOthers).join(JoinSet.to(Other::getPojos)))
 *   .onto(pojo)
 *   .toJSON();
 * }
 * </pre>
 * This API relies on google's {@link Gson}.
 * <br>
 * You can set some custom {@link JsonSerializer} using {@link #register(Type, JsonSerializer)}.
 * @param <T> the target Yopable type to serialize to JSON.
 */
public class JSON<T extends Yopable> {

	/** When adding related object ids ({@link #joinIDs(IJoin)}), the property name is suffixed using this constant */
	private static final String ID_SUFFIX = "#id";

	/** Join clauses */
	private Collection<IJoin<T, ? extends Yopable>> joins = new ArrayList<>();

	/** Join IDs clauses */
	private Collection<IJoin<T, ? extends Yopable>> joinIDs = new ArrayList<>();

	/** The target Yopable class to serialize to JSON */
	private Class<T> target;

	/** The Yopable elements to serialize */
	private List<T> elements = new ArrayList<>();

	/** Some custom serializers (e.g. for {@link java.time}) */
	private final Map<Type, JsonSerializer> serializers = new HashMap<>();

	/**
	 * Private constructor. Set the target and default serializers for {@link java.time} types.
	 * @param target the target class to json-ify
	 */
	private JSON(Class<T> target) {
		this.target = target;
		this.serializers.put(LocalDateTime.class, (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()));
		this.serializers.put(LocalDate.class,     (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()));
		this.serializers.put(LocalTime.class,     (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()));
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
	 * Add an element to be serialized
	 * @param element the element to be serialized
	 * @return the current JSON directive, for chaining purposes
	 */
	public JSON<T> onto(T element) {
		this.elements.add(element);
		return this;
	}

	/**
	 * Add several elements to be serialized
	 * @param elements the elements to be serialized
	 * @return the current JSON directive, for chaining purposes
	 */
	public JSON<T> onto(Collection<T> elements) {
		this.elements.addAll(elements);
		return this;
	}

	/**
	 * Add a relation - to another Yopable type - to be serialized.
	 * @param join the join clause
	 * @param <R> the target join type
	 * @return the current SELECT request, for chaining purpose
	 */
	public <R extends Yopable> JSON<T> join(IJoin<T, R> join) {
		this.joins.add(join);
		return this;
	}

	/**
	 * Serialize the whole data graph. Stop on transient fields.
	 * <br>
	 * <b>⚠⚠⚠ There must be no cycle in the data graph model ! ⚠⚠⚠</b>
	 * <br><br>
	 * <b>⚠⚠⚠ Any join previously set is cleared ! Please add join clause on transient relation after this ! ⚠⚠⚠</b>
	 * @return the current JSON directive, for chaining purpose
	 */
	public JSON<T> joinAll() {
		this.joins.clear();
		IJoin.joinAll(this.target, this.joins);
		return this;
	}

	/**
	 * Add a relation - to another Yopable type whose IDs are set to be serialized.
	 * @param join the join clause
	 * @param <R> the target join type
	 * @return the current SELECT request, for chaining purpose
	 */
	public <R extends Yopable> JSON<T> joinIDs(IJoin<T, R> join) {
		this.joinIDs.add(join);
		return this;
	}

	/**
	 * Serialize IDs from relations on the whole data graph. Stop on transient fields.
	 * <br>
	 * <b>⚠⚠⚠ There must be no cycle in the data graph model ! ⚠⚠⚠</b>
	 * <br><br>
	 * <b>⚠⚠⚠ Any join previously set is cleared ! Please add join clause on transient relation after this ! ⚠⚠⚠</b>
	 * @return the current JSON directive, for chaining purpose
	 */
	public JSON<T> joinIDsAll() {
		this.joinIDs.clear();
		IJoin.joinAll(this.target, this.joinIDs);
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
		this.serializers.put(type, serializer);
		return this;
	}

	/**
	 * Execute the directive : serialize to JSON, as String
	 * @return a JSON string ({@link #elements}, serialized as JSON)
	 */
	public String toJSON() {
		return this.toJSONTree().toString();
	}

	/**
	 * Execute the directive : serialize to JSON, as GSON {@link JsonElement}
	 * @return a GSON JSON element
	 */
	private JsonElement toJSONTree() {
		return this.builder().registerTypeAdapter(this.target, new Serializer()).create().toJsonTree(this.elements);
	}

	/**
	 * Create a new {@link GsonBuilder} with {@link YopableStrategy} and registered {@link #serializers}.
	 * @return the GSON builder instance, for serialization.
	 */
	private GsonBuilder builder() {
		GsonBuilder builder = new GsonBuilder();
		builder.setExclusionStrategies(new YopableStrategy());
		this.serializers.forEach(builder::registerTypeAdapter);
		return builder;
	}

	/**
	 * A custom serializer that can follow the {@link #joins} and {@link #joinIDs} directives.
	 * <br>
	 * Use with {@link YopableStrategy}.
	 */
	private class Serializer implements JsonSerializer<Yopable> {

		/**
		 * {@inheritDoc}
		 * <br>
		 * <b>Implementation details : </b>
		 * <br>
		 * Serialize the current object using {@link Gson} and {@link YopableStrategy}.
		 * No {@link JoinTable} field gets serialized.
		 * <br>
		 * For each joinID directive in {@link #joinIDs} :
		 * <ul>
		 *     <li>find the target field and values</li>
		 *     <li>append a new property with {@link #ID_SUFFIX} suffix</li>
		 *     <li>Fill this property with the IDs of the target field values</li>
		 * </ul>
		 * For each join directive in {@link #joins} :
		 * <ul>
		 *     <li>find the target field, type and values</li>
		 *     <li>append a new property with the field name</li>
		 *     <li>create a new {@link JSON} for the target type, onto the values with the sub-joins of the join</li>
		 *     <li>fill the property with the result of {@link JSON#toJSONTree()}</li>
		 * </ul>
		 */
		@Override
		@SuppressWarnings("unchecked")
		public JsonElement serialize(Yopable src, Type typeOfSrc, JsonSerializationContext context) {
			// Serialize the given element to JSON. The YopableStrategy excludes @JoinTable properties
			Gson gson = JSON.this.builder().create();
			JsonElement element = gson.toJsonTree(src, typeOfSrc);

			// For each joinIDs IJoin relation, get the related objects IDs and add them as a new [property]#ids property.
			Map<Field, IJoin> joinIDsByField = new HashMap<>();
			for (IJoin<T, ? extends Yopable> join : JSON.this.joinIDs) {
				Field field = join.getField(JSON.this.target);
				joinIDsByField.put(field, join);
				Collection<? extends Yopable> next = join.getTarget((T) src);

				element.getAsJsonObject().add(
					field.getName() + ID_SUFFIX,
					gson.toJsonTree(next.stream().map(Yopable::getId).filter(Objects::nonNull).collect(Collectors.toSet()))
				);
			}

			// For each join IJoin relation, serialize using a new 'JSON.from' request
			for (IJoin<T, ? extends Yopable> join : JSON.this.joins) {
				Field field = join.getField(JSON.this.target);
				Collection newElements = join.getTarget((T) src);

				JSON<Yopable> next = JSON.from(join.getTarget(field)).onto(newElements);
				JSON.this.serializers.forEach(next::register);
				next.joins.addAll((Collection) join.getJoins());
				if (joinIDsByField.containsKey(field)) {
					next.joinIDs.addAll(joinIDsByField.get(field).getJoins());
				}

				element.getAsJsonObject().add(field.getName(), next.toJSONTree());
			}

			return element;
		}
	}

	/**
	 * Custom strategy for {@link Yopable} : do not serialize {@link JoinTable} annotated fields.
	 * <br>
	 * These fields will be serialized using {@link JSON.Serializer#serialize(Yopable, Type, JsonSerializationContext)}.
	 */
	private static class YopableStrategy implements ExclusionStrategy {
		@Override
		public boolean shouldSkipField(FieldAttributes f) {
			return f.getAnnotation(JoinTable.class) != null;
		}

		@Override
		public boolean shouldSkipClass(Class<?> clazz) {
			return false;
		}
	}
}
