package org.yop.rest.serialize;

import org.yop.orm.model.Yopable;
import org.yop.orm.query.serialize.Serialize;
import org.yop.orm.query.serialize.json.JSON;
import org.yop.orm.query.serialize.xml.XML;

import java.util.Arrays;
import java.util.List;

/**
 * A collection of {@link org.yop.orm.query.serialize.Serialize} implementations for the supported content types.
 * <br>
 * For now, only JSON :-(
 */
public class Serializers {

	private static final String JSON_MIME_TYPE = "application/json";
	private static final String XML_MIME_TYPE  = "application/xml";

	public static final List<String> SUPPORTED = Arrays.asList(JSON_MIME_TYPE, XML_MIME_TYPE);

	/**
	 * Get the serializer implementation for the given content-type
	 * @param contentType the target content type
	 * @return the appropriate {@link Serialize} implementation.
	 * @throws UnsupportedOperationException if no serializer implementation for the content type
	 */
	public static <T extends Yopable> Serialize<?, T> getFor(Class<T> target, String contentType) {
		switch (contentType) {
			case JSON_MIME_TYPE : return JSON.from(target);
			case XML_MIME_TYPE :  return XML.from(target);
			default: throw new UnsupportedOperationException("For now, we only support JSON and XML serialization :-(");
		}
	}
}
