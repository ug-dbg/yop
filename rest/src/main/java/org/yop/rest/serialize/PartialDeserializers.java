package org.yop.rest.serialize;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.entity.ContentType;
import org.w3c.dom.Element;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.serialize.json.JSON;
import org.yop.orm.query.serialize.xml.XML;

import java.nio.charset.StandardCharsets;
import java.util.*;
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
		this.put(ContentType.APPLICATION_XML.getMimeType(),  Partial::fromXML);
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
		 * Deserialize the given content as the target class and keep a reference to the keys for each object.
		 * @param target   the target class
		 * @param objects  the serialized objects
		 * @return 'partial' objects : the deserialized target objects and the attributes of each one from the input.
		 */
		List<Partial> deserialize(Class target, String objects);
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

		private static List<Partial> fromJSON(Class<Yopable> target, String content) {
			List<Partial> out = new ArrayList<>();
			JsonArray objects = new JsonParser().parse(content).getAsJsonArray();

			for (JsonElement object : objects) {
				if (! object.isJsonObject()) {
					continue;
				}
				Partial partial = new Partial();
				partial.object = JSON.deserialize(target, (JsonObject) object);
				partial.keys = ((JsonObject) object)
					.entrySet()
					.stream()
					.filter(entry -> entry.getValue().isJsonPrimitive())
					.map(Map.Entry::getKey)
					.collect(Collectors.toSet());
				out.add(partial);
			}
			return out;
		}

		private static List<Partial> fromXML(Class<Yopable> target, String content) {
			List<Partial> out = new ArrayList<>();
			List<Element> elements = XML.getFirstLevelElements(content, StandardCharsets.UTF_8);

			for (Element element : elements) {
				Partial partial = new Partial();
				partial.object = XML.deserialize(element, target);

				for (int i = 0; i < element.getAttributes().getLength(); i++) {
					partial.keys.add(element.getAttributes().item(i).getNodeName());
				}
				out.add(partial);
			}
			return out;
		}
	}
}
