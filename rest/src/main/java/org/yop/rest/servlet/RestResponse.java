package org.yop.rest.servlet;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.yop.orm.sql.adapter.IConnection;

import java.util.Collection;
import java.util.Map;

/**
 * A simple wrapper for the output of a REST resource execution that implements {@link IRestResponse}.
 * <br>
 * You might want to add some extra output headers after executing a custom method. This wrapper is for you !
 * <br>
 * Instead of returning the POJO objects or the serialized output, just wrap it with {@link #forOutput(Object)}.
 * <br>
 * Then you can add any output header. The REST servlet will add them into the output response.
 * <br><br>
 * N.B. This wrapper was added to handle paging mechanisms.
 * See an implementation in {@link Get#executeDefault(RestRequest, IConnection)}
 */
public class RestResponse implements IRestResponse {

	/** Execution output. Might be a (collection of) Yopable or a String. */
	private Object output;

	/** Execution status code. Default is 200. */
	private int statusCode = 200;

	/** The output headers to add to the response. */
	private MultiValuedMap<String, String> outputHeaders = new ArrayListValuedHashMap<>();

	/**
	 * Private constructor. Please use {@link #forOutput(Object)}.
	 * @param output the request output.
	 */
	private RestResponse(Object output) {
		this.output = output;
	}

	/**
	 * Wrap the output of a REST resource execution.
	 * @param output the output (might be a (collection of) Yopable or a String)
	 * @return a wrapper for your output
	 */
	static RestResponse forOutput(Object output) {
		return output instanceof RestResponse ? (RestResponse) output : new RestResponse(output);
	}

	/**
	 * Wrap the output of a REST resource execution.
	 * @param output     the output (might be a (collection of) Yopable or a String)
	 * @param statusCode the execution output status code to set in the response
	 * @return a wrapper for your output
	 */
	static RestResponse forOutput(Object output, int statusCode) {
		RestResponse out = output instanceof RestResponse ? (RestResponse) output : new RestResponse(output);
		out.statusCode = statusCode;
		return out;
	}

	/**
	 * Add an output header to the wrapper
	 * @param key   the header name
	 * @param value the header value
	 * @return the current wrapper
	 */
	RestResponse addHeader(String key, String value) {
		this.outputHeaders.put(key, value);
		return this;
	}

	@Override
	public Object output() {
		return this.output;
	}

	@Override
	public int statusCode() {
		return this.statusCode;
	}

	@Override
	public Collection<Map.Entry<String, String>> headers() {
		return this.outputHeaders.entries();
	}
}
