package org.yop.rest.servlet;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.yop.orm.query.AbstractRequest;
import org.yop.orm.query.Context;
import org.yop.orm.query.serialize.Serialize;
import org.yop.orm.sql.adapter.IConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * A simple wrapper for the output of a REST resource execution that implements {@link IRestResponse}.
 * <br>
 * Yop has a strong conventional behavior but you might want to craft the HTTP response after executing a custom method.
 * <br>
 * Instead of returning POJO objects or a serialized output, wrap it (e.g. {@link #wrap(Class, Object)}).
 * <br>
 * Then you can add any output header. The REST servlet will add them into the output response.
 * <br>
 * Use the join methods (e.g. {@link #join(Function)}) to set custom join directives for serialization.
 * <br><br>
 * See a typical usage in {@link Get#executeDefault(RestRequest, IConnection)}
 */
public class RestResponse<T> extends AbstractRequest<RestResponse<T>, T> implements IRestResponse {

	/** Execution output. Might be a (collection of) Yopable or a String. */
	private Object output;

	/** Execution status code. Default is 200. */
	private int statusCode = 200;

	/** The output headers to add to the response. */
	private MultiValuedMap<String, String> outputHeaders = new ArrayListValuedHashMap<>();

	/**
	 * Private constructor. Please use {@link #forOutput(Object)}.
	 * @param target the output target type
	 * @param output the request output.
	 */
	private RestResponse(Class<T> target, Object output) {
		super(Context.root(target));
		this.output = output;
	}

	/**
	 * Wrap an empty list as the output of a REST resource execution.
	 * @param target the target type
	 * @return a wrapper for an empty list
	 */
	public static <T> IRestResponse empty(Class<T> target) {
		return new RestResponse<>(target, new ArrayList<>(0));
	}

	/**
	 * Wrap the output of a REST resource execution.
	 * @param output the output (might be a (collection of) Yopable, a String... or an IResponse)
	 * @return a new wrapper for the output or the output parameter itself if applicable.
	 */
	public static <T> IRestResponse wrap(Class<T> target, Object output) {
		return output instanceof IRestResponse ? (IRestResponse) output : new RestResponse<>(target, output);
	}

	/**
	 * Wrap the output of a REST resource execution as a single T object.
	 * @param target the output target type
	 * @param output the request output.
	 * @return a new wrapper for the output
	 */
	public static <T> RestResponse<T> build(Class<T> target, T of) {
		return new RestResponse<>(target, of);
	}

	/**
	 * Wrap the output of a REST resource execution as a collection of T.
	 * @param target the output target type
	 * @param output the request output.
	 * @return a new wrapper for the output
	 */
	public static <T> RestResponse<T> build(Class<T> target, Collection<T> of) {
		return new RestResponse<>(target, of);
	}

	/**
	 * Add an output header to the wrapper
	 * @param key   the header name
	 * @param value the header value
	 * @return the current wrapper
	 */
	@Override
	public RestResponse<T> header(String key, String value) {
		this.outputHeaders.put(key, value);
		return this;
	}

	@Override
	public IRestResponse statusCode(int code) {
		this.statusCode = code;
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

	@Override
	@SuppressWarnings("unchecked")
	public <U> Serialize serializer(RestRequest<U> restRequest) {
		return IRestResponse.super.serializer(restRequest).join(this.getJoins());
	}
}
