package org.yop.orm.model;

import com.google.common.primitives.Primitives;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.yop.orm.query.Context;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.List;

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
	 * See {@link #fieldValue(Context, Field, JsonElement)}.
	 * @param context the current context
	 * @param element the JSON element state
	 * @param <T> the context target type
	 */
	@SuppressWarnings("unchecked")
	default <T extends Yopable> void fromJSON(Context<T> context, JsonElement element) {
		if (! (element instanceof JsonObject)) {
			return;
		}
		JsonObject object = (JsonObject) element;
		for (String key : object.keySet()) {
			Field field = Reflection.get(this.getClass(), key);
			if (field == null || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			Object fieldValue = fieldValue(context, field, object.get(key));
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
	 * @return the value that can be set on the field, null if the field type did not match anything.
	 */
	@SuppressWarnings("unchecked")
	static Object fieldValue(Context<? extends Yopable> context, Field field, JsonElement fieldValueJSON) {
		Class fieldType = field.getType();
		if (fieldType.isEnum()) {
			return Enum.valueOf(fieldType, fieldValueJSON.getAsString());
		}
		if (JsonAble.class.isAssignableFrom(fieldType)) {
			JsonAble fieldValue = (JsonAble) Reflection.newInstanceNoArgs(fieldType);
			fieldValue.fromJSON(context, fieldValueJSON);
			return fieldValue;
		}
		if (Boolean.class.isAssignableFrom(Primitives.wrap(fieldType))) {
			return fieldValueJSON.getAsBoolean();
		}
		if (String.class.isAssignableFrom(fieldType)) {
			return fieldValueJSON.getAsString();
		}
		if (Number.class.isAssignableFrom(Primitives.wrap(fieldType))) {
			return fieldValueJSON.getAsDouble();
		}
		return null;
	}
}
