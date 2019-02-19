package org.yop.rest.serialize;

import com.google.gson.JsonObject;
import org.apache.http.entity.ContentType;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.serialize.json.JSON;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A collection of {@link org.yop.orm.query.serialize.Serialize} deserialize methods for the supported content types.
 * <br>
 * For now, only JSON :-(
 * <br>
 * These deserialize methods also returns the attribute fields of the deserialized object.
 * See the {@link Partial} that aggregates a {@link Yopable} object and its keys.
 */
public class PartialDeserializers {

	private static final PartialDeserializers.Deserializer UNSUPPORTED_CONTENT_TYPE = (t, c) -> {
		throw new UnsupportedOperationException("Content type [" + c + "] is unsupported.");
	};

	@SuppressWarnings("unchecked")
	private static final Map<String, Deserializer> DESERIALIZERS = new HashMap<String, Deserializer>() {{
		this.put(ContentType.APPLICATION_JSON.getMimeType(), Partial::fromJSON);
	}};

	/**
	 * Get the deserializer for the given content-type
	 * @param contentType the target content type
	 * @return the appropriate deserializer. {@link #UNSUPPORTED_CONTENT_TYPE} if no match.
	 */
	public static PartialDeserializers.Deserializer getFor(String contentType) {
		return DESERIALIZERS.getOrDefault(contentType, UNSUPPORTED_CONTENT_TYPE);
	}

	@FunctionalInterface
	public interface Deserializer {
		/**
		 * Deserialize the given JSON object as the target class.
		 * @param target  the target class
		 * @param content the JSON object
		 * @return a 'partial' object : the deserialized target object and the attributes from the input json.
		 */
		Partial deserialize(Class target, JsonObject content);
	}

	/**
	 * A simple aggregation of a deserialized object and the attributes from the input JSON object.
	 */
	public static class Partial {
		private Yopable object;
		private Set<String> keys = new HashSet<>();

		public Yopable getObject() {
			return this.object;
		}

		public Set<String> getKeys() {
			return this.keys;
		}

		private static Partial fromJSON(Class<Yopable> target, JsonObject jsonObject) {
			Partial partial = new Partial();
			partial.object = JSON.deserialize(target, jsonObject);
			partial.keys = jsonObject
				.entrySet()
				.stream()
				.filter(entry -> entry.getValue().isJsonPrimitive())
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
			return partial;
		}
	}
}
