package org.yop.rest.serialize;

import org.yop.orm.model.Yopable;
import org.yop.orm.query.serialize.Serialize;
import org.yop.orm.query.serialize.json.JSON;

import java.util.Collections;
import java.util.List;

/**
 * A collection of {@link org.yop.orm.query.serialize.Serialize} implementations for the supported content types.
 * <br>
 * For now, only JSON :-(
 */
public class Serializers {

	private static final String JSON_MIME_TYPE = "application/json";

	public static final List<String> SUPPORTED = Collections.singletonList(JSON_MIME_TYPE);

	/**
	 * Get the serializer implementation for the given content-type
	 * @param contentType the target content type
	 * @return the appropriate {@link Serialize} implementation.
	 * @throws UnsupportedOperationException if no serializer implementation for the content type
	 */
	public static <T extends Yopable> Serialize<?, T> getFor(Class<T> target, String contentType) {
		switch (contentType) {
			case JSON_MIME_TYPE : return JSON.from(target);
			default: throw new UnsupportedOperationException("For now, we only support JSON serialization :-(");
		}
	}
}
