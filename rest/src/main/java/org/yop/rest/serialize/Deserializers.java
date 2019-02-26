package org.yop.rest.serialize;

import com.google.gson.JsonParser;
import org.apache.http.entity.ContentType;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.serialize.json.JSON;
import org.yop.orm.query.serialize.xml.XML;

import java.util.*;

/**
 * A collection of {@link org.yop.orm.query.serialize.Serialize} deserialize methods for the supported content types.
 * <br>
 * For now, only JSON :-(
 */
public class Deserializers {

	private static final String JSON_MIME_TYPE = "application/json";
	private static final String XML_MIME_TYPE  = "application/xml";

	public static final List<String> SUPPORTED = Arrays.asList(JSON_MIME_TYPE, XML_MIME_TYPE);

	private static final Deserializer UNSUPPORTED_CONTENT_TYPE = (t, c) -> {
		throw new UnsupportedOperationException("Content type [" + c + "] is unsupported.");
	};

	@SuppressWarnings("unchecked")
	private static final Map<String, Deserializer> DESERIALIZERS = new HashMap<String, Deserializer>() {{
		this.put(ContentType.APPLICATION_JSON.getMimeType(), (t, c) -> JSON.deserialize(t, new JsonParser().parse(c)));
		this.put(ContentType.APPLICATION_XML.getMimeType(),  (t, c) -> XML.deserialize(c, t));
	}};

	/**
	 * Get the deserializer for the given content-type
	 * @param contentType the target content type
	 * @return the appropriate deserializer. {@link #UNSUPPORTED_CONTENT_TYPE} if no match.
	 */
	public static Deserializer getFor(String contentType) {
		return DESERIALIZERS.getOrDefault(contentType, UNSUPPORTED_CONTENT_TYPE);
	}

	@FunctionalInterface
	public interface Deserializer {
		/**
		 * Deserialize the given object as the target class.
		 * @param target  the target class
		 * @param content the serialized objects
		 * @return the deserialized objects
		 */
		Collection<Yopable> deserialize(Class target, String content);
	}
}
