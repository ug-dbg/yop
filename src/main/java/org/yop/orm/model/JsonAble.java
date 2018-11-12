package org.yop.orm.model;

import com.google.common.primitives.Primitives;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

/**
 * This is the YOP interface to mark JSON query elements (Select, Join, Evaluation...) as JSON serializable.
 * <br>
 * <b>Using Yop does not require implementing this interface.</b>
 */
public interface JsonAble {

	/**
	 * Set the current instance state from a JSON representation.
	 * <br>
	 * Default implementation :
	 * set state from any non-static field of the class whose name matches a JSON representation key.
	 * <br>
	 * See {@link #fieldValue(Context, Field, JsonElement, Config)}.
	 * @param context the current context
	 * @param element the JSON element state
	 * @param config  the SQL config. Needed for the sql separator to use - for some very specific cases.
	 * @param <T> the context target type
	 */
	@SuppressWarnings("unchecked")
	default <T extends Yopable> void fromJSON(Context<T> context, JsonElement element, Config config) {
		if (! (element instanceof JsonObject)) {
			return;
		}
		JsonObject object = (JsonObject) element;
		for (String key : object.keySet()) {
			Field field = Reflection.get(this.getClass(), key);
			if (field == null || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			Object fieldValue = fieldValue(context, field, object.get(key), config);
			if (fieldValue != null) {
				Reflection.set(field, this, fieldValue);
			}
		}
	}

	/**
	 * Create a JSON representation for the current state.
	 * <br>
	 * Default implementation :
	 * serialize instance to a new JSON object and serialize any non static field that is of type :
	 * <ul>
	 *     <li>{@link Enum}</li>
	 *     <li>{@link JsonAble}</li>
	 *     <li>{@link String}</li>
	 *     <li>{@link Boolean}</li>
	 *     <li>{@link Number}</li>
	 * </ul>
	 * @param context the current context
	 * @param <T> the context target type
	 * @return a new JSON object serializing the current instance state
	 */
	default <T extends Yopable> JsonElement toJSON(Context<T> context) {
		JsonObject out = new JsonObject();
		List<Field> fields = Reflection.getFields(this.getClass(), true);

		for (Field field : fields) {
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			Object fieldValue = Reflection.readField(field, this);

			if (fieldValue == null ){
				continue;
			}
			if (field.getType().isEnum()) {
				out.addProperty(field.getName(), ((Enum)fieldValue).name());
			}
			if (fieldValue instanceof JsonAble) {
				out.add(field.getName(), ((JsonAble) fieldValue).toJSON(context));
			}
			if (fieldValue instanceof Boolean) {
				out.addProperty(field.getName(), (Boolean) fieldValue);
			}
			if (fieldValue instanceof String) {
				out.addProperty(field.getName(), (String) fieldValue);
			}
			if (fieldValue instanceof Number) {
				out.addProperty(field.getName(), (Number) fieldValue);
			}
		}

		return out;
	}

	/**
	 * Create the value for a field from its JSON state.
	 * Works with field types :
	 * <ul>
	 *     <li>{@link Enum}</li>
	 *     <li>{@link JsonAble}</li>
	 *     <li>{@link String}</li>
	 *     <li>{@link Boolean}</li>
	 *     <li>{@link Number}</li>
	 * </ul>
	 * @param context        the current context
	 * @param field          the target field
	 * @param fieldValueJSON the field JSON state
	 * @param config         the SQL config. Needed for the sql separator to use - for some very specific cases
	 * @return the value that can be set on the field, null if the field type did not match anything.
	 */
	@SuppressWarnings("unchecked")
	static Object fieldValue(
		Context<? extends Yopable> context,
		Field field,
		JsonElement fieldValueJSON,
		Config config) {

		Class fieldType = Primitives.wrap(field.getType());
		if (fieldType.isEnum()) {
			return Enum.valueOf(fieldType, fieldValueJSON.getAsString());
		}
		if (JsonAble.class.isAssignableFrom(fieldType)) {
			JsonAble fieldValue = (JsonAble) Reflection.newInstanceNoArgs(fieldType);
			fieldValue.fromJSON(context, fieldValueJSON, config);
			return fieldValue;
		}
		if (Boolean.class.isAssignableFrom(fieldType)) {
			return fieldValueJSON.getAsBoolean();
		}
		if (String.class.isAssignableFrom(fieldType)) {
			return fieldValueJSON.getAsString();
		}
		if (Number.class.isAssignableFrom(fieldType)) {
			if (Integer.class.isAssignableFrom(fieldType)) {
				return fieldValueJSON.getAsInt();
			}
			if (Long.class.isAssignableFrom(fieldType)) {
				return fieldValueJSON.getAsLong();
			}
			if (Float.class.isAssignableFrom(fieldType)) {
				return fieldValueJSON.getAsFloat();
			}
			if (Double.class.isAssignableFrom(fieldType)) {
				return fieldValueJSON.getAsDouble();
			}
			if (BigDecimal.class.isAssignableFrom(fieldType)) {
				return fieldValueJSON.getAsBigDecimal();
			}
			if (BigInteger.class.isAssignableFrom(fieldType)) {
				return fieldValueJSON.getAsBigInteger();
			}
		}
		return null;
	}

	/**
	 * Very rough method that turns a field value into JSON.
	 * <ul>
	 *     <li>object is {@link JsonAble} → {@link #toJSON(Context)} (very unlikely)</li>
	 *     <li>else → new {@link JsonPrimitive} from {@link Objects#toString(Object)}</li>
	 * </ul>
	 * @param context the current context
	 * @param o       the object to serialize
	 * @return a GSON JSON element from the given value
	 */
	static JsonElement jsonValue(Context<? extends Yopable> context, Object o) {
		if (o instanceof JsonAble) {
			return ((JsonAble) o).toJSON(context);
		}
		return new JsonPrimitive(Objects.toString(o));
	}
}
