package org.yop.orm.model.json;

import com.google.gson.*;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;

public interface YopableJson extends Yopable, JsonSerializer<Yopable> {

	String JSON_TRANSIENT_ID_SUFFIX = "Id";
	Long JSON_TRANSIENT_NONE_ID = -1L;

	@Override
	default JsonElement serialize(Yopable src, Type typeOfSrc, JsonSerializationContext context) {
		JsonElement serialized = new Gson().toJsonTree(src, typeOfSrc);

		for (Field transientColumnField : src.getTransientRelations()) {
			try {
				Object value = transientColumnField.get(this);

				if(value == null) {
					((JsonObject) serialized).addProperty(
						transientColumnField.getName() + JSON_TRANSIENT_ID_SUFFIX,
						JSON_TRANSIENT_NONE_ID
					);
				}

				if(value instanceof Yopable) {
					Long id = ((Yopable) value).getId();
					((JsonObject) serialized).addProperty(
						transientColumnField.getName() + JSON_TRANSIENT_ID_SUFFIX,
						id
					);
				}

				if(value instanceof Collection) {
					throw new UnsupportedOperationException("transient collection relations not supported yet !");
				}

			} catch (IllegalAccessException e) {
				throw new YopRuntimeException(
					"Could not read [" + this.getClass().getName() + "#" + transientColumnField.getName() + "]"
				);
			}
		}

		return serialized;
	}

	default String toJson() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(this.getClass(), this);
		return builder.create().toJson(this);
	}

}
