package org.yop.rest.servlet;

import org.yop.orm.query.serialize.Serialize;
import org.yop.orm.util.ORMUtil;
import org.yop.rest.serialize.Serializers;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * An interface for the output of a REST request.
 * <br>
 * It should not be mandatory for a @Rest method to return a IRestResponse though.
 */
public interface IRestResponse {

	/**
	 * Get the response output.
	 * <br><br>
	 * If yopable or collection of yopables, Yop should try to serialize.
	 * <br>
	 * Else {@link java.util.Objects#toString(Object)} should be used.
	 * @return the response output
	 */
	Object output();

	/**
	 * Get the output response code to set
	 * @return the response code that Yop should set in the output response
	 */
	int statusCode();

	/**
	 * Get the output headers to add to the response, as a collection of {@link Map.Entry}.
	 * @return the output headers to add to the response
	 */
	Collection<Map.Entry<String, String>> headers();

	/**
	 * Add a new output header
	 * @param key   the header key
	 * @param value the header value
	 * @return the current instance
	 */
	IRestResponse header(String key, String value);

	/**
	 * Set the response status code.
	 * @param code the status code
	 * @return the current instance
	 */
	IRestResponse statusCode(int code);

	/**
	 * Serialize the execution {@link #output()}, be it a Yopable, a collection of Yopable or anything else.
	 * <br>
	 * The serializer is retrieved using {@link #serializer(RestRequest)}.
	 * <br>
	 * If the input object is neither a Yopable or a collection, naively use {@link Objects#toString(Object)}.
	 * @param restRequest the incoming rest request.
	 * @return the execution result, serialized into a String
	 */
	@SuppressWarnings("unchecked")
	default <T> String serialize(RestRequest<T> restRequest) {
		Object output = this.output();
		if (ORMUtil.isYopable(output) || output instanceof Collection) {
			Serialize serializer = this.serializer(restRequest);

			if (output instanceof Collection) {
				serializer.onto((Collection) output);
			} else {
				serializer.onto(output);
			}

			if (restRequest.joinAll()) {
				serializer.joinAll();
			}
			serializer.joinProfiles(restRequest.profiles().toArray(new String[0]));
			return serializer.execute();
		}

		return Objects.toString(output);
	}

	/**
	 * Get an output serializer instance for the given request.
	 * @param restRequest the REST request
	 * @param <T> the target type
	 * @return a serializer instance
	 */
	default <T> Serialize serializer(RestRequest<T> restRequest) {
		String outputContentType = restRequest.accept(Serializers.SUPPORTED);
		return Serializers.getFor(restRequest.getRestResource(), outputContentType);
	}
}
