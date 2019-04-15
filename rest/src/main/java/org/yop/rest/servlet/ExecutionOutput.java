package org.yop.rest.servlet;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.yop.orm.sql.adapter.IConnection;

import java.util.Collection;
import java.util.Map;

/**
 * Wrapper for the output of a REST resource execution.
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
class ExecutionOutput {

	/** Execution output. Might be a (collection of) Yopable or a String. */
	private Object output;

	/** The output headers to add to the response. */
	private MultiValuedMap<String, String> outputHeaders = new ArrayListValuedHashMap<>();

	/**
	 * Private constructor. Please use {@link #forOutput(Object)}.
	 * @param output the request output.
	 */
	private ExecutionOutput(Object output) {
		this.output = output;
	}

	/**
	 * Wrap the output of a REST resource execution.
	 * @param output the output (might be a (collection of) Yopable or a String)
	 * @return a wrapper for your output
	 */
	static ExecutionOutput forOutput(Object output) {
		return output instanceof ExecutionOutput ? (ExecutionOutput) output : new ExecutionOutput(output);
	}

	/**
	 * Add an output header to the wrapper
	 * @param key   the header name
	 * @param value the header value
	 * @return the current wrapper
	 */
	ExecutionOutput addHeader(String key, String value) {
		this.outputHeaders.put(key, value);
		return this;
	}

	/**
	 * Get the wrapped output.
	 * @return the wrapped {@link #output}
	 */
	Object getOutput() {
		return this.output;
	}

	/**
	 * Get the output headers to add to the response, as a collection of {@link Map.Entry}.
	 * @return the output headers to add to the response
	 */
	Collection<Map.Entry<String, String>> getHeaders() {
		return this.outputHeaders.entries();
	}
}
