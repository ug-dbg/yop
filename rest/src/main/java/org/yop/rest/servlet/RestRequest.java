package org.yop.rest.servlet;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.model.Yopable;
import org.yop.reflection.Reflection;
import org.yop.rest.annotations.Rest;
import org.yop.rest.exception.YopBadContentException;
import org.yop.rest.exception.YopNoResourceException;
import org.yop.rest.serialize.Deserializers;
import org.yop.rest.serialize.MIMEParse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A rest request to a {@link Yopable} with a reference to the HTTP request & response.
 */
class RestRequest {

	private static final Logger logger = LoggerFactory.getLogger(RestRequest.class);

	private HttpServletRequest request;
	private HttpServletResponse response;

	private String requestPath;

	private Class<Yopable> restResource;
	private Path subResource = Paths.get("");
	private String method;
	private String accept;

	private String contentType;
	private String content;

	private MultiValuedMap<String, String> parameters = new ArrayListValuedHashMap<>();
	private List<Header> headers = new ArrayList<>();

	/**
	 * Default constructor.
	 * <br>
	 * Fields will be computed from the HTTP request.
	 * <br>
	 * Both references to HTTP request and response will be kept ({@link #request} and {@link #response}).
	 * @param req          the HTTP request
	 * @param resp         the HTTP response
	 * @param yopablePaths a map of applicable @Rest Yopable whose keys are the {@link Rest#path()}.
	 */
	RestRequest(HttpServletRequest req, HttpServletResponse resp, Yopables yopablePaths) {
		this.method = req.getMethod();
		this.accept = req.getHeader("Accept");
		this.request = req;
		this.response = resp;

		this.requestPath  = req.getRequestURI();
		String servletPath  = req.getServletPath();
		String contextPath = req.getContextPath();
		String resourcePath = StringUtils.removeStart(this.requestPath, contextPath);
		resourcePath = StringUtils.removeStart(resourcePath, servletPath);
		resourcePath = StringUtils.removeStart(resourcePath, "/");
		resourcePath = StringUtils.removeEnd(resourcePath, "/");

		this.contentType = StringUtils.defaultIfBlank(
			MIMEParse.bestMatch(Deserializers.SUPPORTED, req.getHeader("Content-Type")),
			ContentType.APPLICATION_JSON.getMimeType()
		);

		try {
			this.content = IOUtils.toString(req.getInputStream());
		} catch (IOException e) {
			logger.debug("No content for request", e);
		}

		req.getParameterMap().forEach((key, value) -> this.parameters.putAll(key, Arrays.asList(value)));

		for(Enumeration<String> headerNames = req.getHeaderNames(); headerNames.hasMoreElements();) {
			String header = headerNames.nextElement();
			this.headers.add(new BasicHeader(header, req.getHeader(header)));
		}

		this.restResource = findResource(resourcePath, yopablePaths);
		Path path = Paths.get(this.restResource.getAnnotation(Rest.class).path());
		if (yopablePaths.containsKey(resourcePath)) {
			// Exact match → use yopable paths key instead of @Rest#path() : better context management
			path = Paths.get(resourcePath);
		}
		path = Paths.get(StringUtils.removeStart(resourcePath, path.toString()));

		if (path.getNameCount() >= 1) {
			this.subResource = path.isAbsolute() ? Paths.get("/").relativize(path) : path;
		}
	}

	/**
	 * Get the reference to the incoming HTTP request.
	 * @return {@link #request}
	 */
	public HttpServletRequest getRequest() {
		return this.request;
	}

	/**
	 * Get the reference to the outgoing HTTP response.
	 * @return {@link #response}
	 */
	public HttpServletResponse getResponse() {
		return this.response;
	}

	@Override
	public String toString() {
		return "RestRequest{" + "requestPath='" + this.requestPath + '\'' + ", method='" + this.method + '\'' + '}';
	}

	/**
	 * Get the HTTP method of this REST request.
	 * @return {@link #method} that was read from the HTTP request.
	 */
	String getMethod() {
		return this.method;
	}

	/**
	 * Get the full request path.
	 * @return {@link #requestPath} that was read from {@link HttpServletRequest#getRequestURI()}.
	 */
	String getRequestPath() {
		return this.requestPath;
	}

	/**
	 * Build the request path that matched.
	 * (context path, servlet path, rest resource path, method path) → /yop/rest/book/{id}
	 * @return the request path, i.e /yop/rest/book/{id}
	 */
	private String buildRequestPath() {
		String methodPath = "";
		Optional<Method> candidate = this.getCandidate();
		if (candidate.isPresent()) {
			methodPath = candidate.get().getAnnotation(Rest.class).path();
		}
		return Paths.get(
			this.request.getContextPath(),
			this.request.getServletPath(),
			this.getRestResource().getAnnotation(Rest.class).path(),
			methodPath
		).toString();
	}

	/**
	 * What is the best match for the 'Accept' content type among the supported mime types ?
	 * @param supported the supported mime types
	 * @return the supported types' best match, or an empty string.
	 */
	String accept(Collection<String> supported) {
		return MIMEParse.bestMatch(
			supported,
			StringUtils.defaultIfBlank(this.accept, ContentType.APPLICATION_JSON.getMimeType())
		);
	}

	/**
	 * Get the current request parameters as an array of apache commons {@link NameValuePair}.
	 * @return {@link #parameters},
	 *         read from {@link HttpServletRequest#getParameterMap()},
	 *         converted into an array of apache commons {@link NameValuePair}.
	 */
	NameValuePair[] getParameters() {
		List<NameValuePair> out = new ArrayList<>();
		this.parameters.entries().forEach(entry -> out.add(new BasicNameValuePair(entry.getKey(), entry.getValue())));
		return out.toArray(new NameValuePair[0]);
	}

	/**
	 * Get the first value of an HTTP parameter.
	 * @param name the parameter name
	 * @return the first value from {@link #parameters}, read from {@link HttpServletRequest#getParameterMap()}
	 */
	String getParameterFirstValue(String name) {
		Collection<String> values = this.parameters.get(name);
		return values.isEmpty() ? null : values.iterator().next();
	}

	/**
	 * Get the HTTP request headers.
	 * @return {@link #headers}, as an array.
	 */
	Header[] getHeaders() {
		return this.headers.toArray(new Header[0]);
	}

	/**
	 * Get the ID associated to this request, if applicable. Might be null.
	 * <br>
	 * This method actually returns the last path element of the request, as Long, if applicable.
	 * <br>
	 * e.g. /yop/user/1 → 1
	 * @return the id associated to the REST request
	 */
	Long getId() {
		if (this.subResource.getNameCount() >= 0) {
			Path last = this.subResource.getName(this.subResource.getNameCount() - 1);
			if (NumberUtils.isCreatable(last.toString())) {
				return Long.valueOf(last.toString());
			}
		}
		return null;
	}

	/**
	 * Get the {@link Yopable} class associated to the REST request.
	 * @return {@link #restResource}. Might be null if no match.
	 */
	Class<Yopable> getRestResource() {
		return this.restResource;
	}

	/**
	 * Is this request a standard YOP REST request
	 * or is there a custom method on the associated {@link #restResource} that matches the path ?
	 * @return true if this request targets a custom resource.
	 */
	boolean isCustomResource() {
		return Reflection.getMethods(this.restResource).stream().anyMatch(this::matches);
	}

	/**
	 * Does this request has a 'joinAll' directive ?
	 * @return true if there is a 'joinAll' parameter set to true or with no explicit value
	 */
	boolean joinAll() {
		return this.parameters.containsMapping(HttpMethod.PARAM_JOIN_ALL, "true")
			|| this.parameters.containsMapping(HttpMethod.PARAM_JOIN_ALL, null);
	}

	/**
	 * Does this request has a 'checkNaturalID' directive ?
	 * @return true if there is a 'checkNaturalID' parameter set to true or with no explicit value
	 */
	boolean checkNaturalID() {
		return this.parameters.containsMapping(HttpMethod.PARAM_CHECK_NK, "true")
			|| this.parameters.containsMapping(HttpMethod.PARAM_CHECK_NK, null);
	}

	/**
	 * Does this request has a 'count' directive in its headers ?
	 * @return true if there is a {@link HttpMethod#PARAM_COUNT} header set to true
	 */
	boolean count() {
		return this
			.headers
			.stream()
			.filter(h -> HttpMethod.PARAM_COUNT.equals(h.getName()))
			.map(Header::getValue)
			.map(BooleanUtils::toBoolean)
			.findFirst()
			.orElse(false);
	}

	/**
	 * Get all the joinProfile parameter values.
	 * @return all the profile values from the {@link HttpMethod#PARAM_JOIN_PROFILE} parameters.
	 */
	Set<String> profiles() {
		return new HashSet<>(this.parameters.get(HttpMethod.PARAM_JOIN_PROFILE));
	}

	/**
	 * Does this request has a 'paging' directive in its headers ?
	 * @return true if there is either a {@link HttpMethod#PARAM_OFFSET} or a {@link HttpMethod#PARAM_LIMIT} header
	 */
	boolean isPaging() {
		return this
			.headers
			.stream()
			.anyMatch(h -> Arrays.asList(HttpMethod.PARAM_OFFSET, HttpMethod.PARAM_LIMIT).contains(h.getName()));
	}

	/**
	 * Read the {@link HttpMethod#PARAM_OFFSET} header parameter value.
	 * @return the paging offset header value, or null if this header is not set.
	 */
	Long offset() {
		return this
			.headers
			.stream()
			.filter(h -> HttpMethod.PARAM_OFFSET.equals(h.getName()))
			.map(Header::getValue)
			.filter(NumberUtils::isDigits)
			.map(Long::valueOf)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Read the {@link HttpMethod#PARAM_LIMIT} header parameter value.
	 * @return the paging limit header value, or null if this header is not set.
	 */
	Long limit() {
		return this
			.headers
			.stream()
			.filter(h -> HttpMethod.PARAM_LIMIT.equals(h.getName()))
			.map(Header::getValue)
			.filter(NumberUtils::isDigits)
			.map(Long::valueOf)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Read the {@link HttpMethod#PARAM_PARTIAL} header parameter value.
	 * @return the partial header parameter value, or false if this header is not set.
	 */
	boolean isPartial() {
		return this
			.headers
			.stream()
			.filter(h -> HttpMethod.PARAM_PARTIAL.equals(h.getName()))
			.map(Header::getValue)
			.map(BooleanUtils::toBoolean)
			.findFirst()
			.orElse(false);
	}

	/**
	 * Get the body from the input request.
	 * @return {@link #content} read from {@link HttpServletRequest#getInputStream()}.
	 */
	String getContent() {
		return StringUtils.isEmpty(this.content) ? "" : this.content;
	}

	/**
	 * Get the first custom method from {@link #restResource} that matches the REST request.
	 * @return an optional for the first matching method.
	 */
	Optional<Method> getCandidate() {
		return Reflection.getMethods(this.restResource).stream().filter(this::matches).findFirst();
	}

	/**
	 * Get a parameter from the effective request path ({@link #requestPath}), knowing the path pattern.
	 * @param name     the name of the parameter
	 * @return the parameter value or an empty string if no match
	 */
	String getPathParam(String name) {
		String regex = StringUtils.replace(this.buildRequestPath(), "{" + name + "}", "(.*)");
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(this.getRequestPath());
		if (matcher.find()) {
			String encodedParameter = matcher.group(1);
			String encoding = this.request.getCharacterEncoding();
			encoding = encoding == null ? StandardCharsets.UTF_8.name() : encoding;
			try {
				return URLDecoder.decode(encodedParameter, encoding);
			} catch (UnsupportedEncodingException e) {
				throw new YopBadContentException("Could not decode parameter [" + encodedParameter + "]", e);
			}
		}
		return "";
	}

	/**
	 * Read the input from the request and deserialize it to a collection of {@link Yopable}
	 * using a {@link org.yop.orm.query.serialize.Serialize}.
	 * @return a collection of Yopable from the incoming request
	 * @throws YopBadContentException Could not parse the input content
	 */
	Collection<Yopable> contentAsYopables() {
		try {
			return Deserializers.getFor(this.contentType).deserialize(this.getRestResource(), this.content);
		} catch (RuntimeException e) {
			throw new YopBadContentException(
				"Unable to parse JSON array [" + StringUtils.abbreviate(this.content, 50) + "]",
				e
			);
		}
	}

	/**
	 * Read the input from the request and deserialize it to a {@link Yopable}
	 * using a {@link org.yop.orm.query.serialize.Serialize}.
	 * @return a Yopable instance from the incoming request
	 * @throws YopBadContentException Could not parse the input content as JSON object
	 */
	Yopable contentAsYopable() {
		try {
			return Deserializers
				.getFor(this.contentType)
				.deserialize(this.getRestResource(), this.content)
				.iterator()
				.next();
		} catch (RuntimeException e) {
			throw new YopBadContentException(
				"Unable to parse JSON object [" + StringUtils.abbreviate(this.content, 50) + "]",
				e
			);
		}
	}

	/**
	 * Does this method matches the current REST request ?
	 * @param method the method to check
	 * @return true if the method is a candidate for the REST request
	 */
	private boolean matches(Method method) {
		if (! method.isAnnotationPresent(Rest.class)) {
			return false;
		}
		String methodPath = method.getAnnotation(Rest.class).path();
		String methodPathRegex = methodPath.replaceAll(Pattern.quote("{") + ".*" + Pattern.quote("}"), ".+");
		PathMatcher global = FileSystems.getDefault().getPathMatcher("regex:" + methodPathRegex);
		return method.isAnnotationPresent(Rest.class)
			&& Arrays.stream(method.getAnnotation(Rest.class).methods()).anyMatch(s -> this.method.equals(s))
			&& global.matches(this.subResource);
	}

	/**
	 * Find the yopable resource which {@link Rest#path()} is the closest match to the request path.
	 * @param requestPath  the incoming request path (no servlet context, no leading '/')
	 * @param yopablePaths the Yopable paths map
	 * @return the closest match
	 * @throws YopNoResourceException No match for the request path. This should trigger an HTTP 404
	 */
	@SuppressWarnings("unchecked")
	private static Class<Yopable> findResource(String requestPath, Yopables yopablePaths) {
		TreeSet<String> paths = new TreeSet<>(Comparator.comparing(String::length));
		paths.addAll(yopablePaths.keySet().stream().filter(requestPath::startsWith).collect(Collectors.toSet()));
		if (! paths.isEmpty()) {
			return (Class) yopablePaths.get(paths.last());
		}
		throw new YopNoResourceException("No REST Yopable for request path [" + requestPath + "]");
	}
}
