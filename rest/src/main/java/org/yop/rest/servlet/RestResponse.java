package org.yop.rest.servlet;

import java.util.Collection;
import java.util.Map;

/**
 * An interface for the output of a REST request.
 * <br>
 * It should not be mandatory for a @Rest method to return a RestResponse though.
 */
public interface RestResponse {

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
}
