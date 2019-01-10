package org.yop.rest.servlet;

import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.annotations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * When defining a custom Rest method on a Yopable ({@link HttpMethod#executeCustom(RestRequest, IConnection)}),
 * a parameter mapping must be done.
 * <br>
 * To prevent the code from becoming an if/else mess, we want to use functional interfaces.
 * <br>
 * The behavior (lambda) for each possible parameter annotation is put in a map ({@link #ACTIONS}).
 * <br>
 * Then a static method simply reads the parameter annotations, ask the map for the associated lambda
 * and execute it ({@link #computeValue(RestRequest, Parameter)}).
 * <br><br>
 * If this becomes as unreadable as an if/else muddle, this should be challenged as well.
 */
public interface AnnotationToParameter {

	/** For every applicable Yop REST parameter annotation, add a lambda that can compute the actual parameter value. */
	Map<Class, AnnotationToParameter> ACTIONS = new HashMap<Class, AnnotationToParameter>() {{
		this.put(Content.class,       (r, p) -> r.getContent());
		this.put(RequestPath.class,   (r, p) -> r.getRequestPath());
		this.put(RequestParam.class,  (r, p) -> r.getParameterFirstValue(p.getAnnotation(RequestParam.class).name()));
		this.put(PathParam.class,     (r, p) -> r.getPathParam(p.getAnnotation(PathParam.class).name()));
		this.put(Header.class,        (r, p) -> r.getRequest().getHeader(p.getAnnotation(Header.class).name()));
		this.put(BodyInstance.class,  (r, p) -> r.contentAsYopable());
		this.put(BodyInstances.class, (r, p) -> r.contentAsYopables());
	}};

	/**
	 * Compute the parameter value, by asking {@link #ACTIONS} for a lambda associated to its annotation.
	 * @param request   the REST request
	 * @param parameter the method parameter
	 * @return the value for the parameter, given the actual REST request. null if nothing matched {@link #ACTIONS}.
	 */
	static Object get(RestRequest request, Parameter parameter) {
		for (Annotation annotation : parameter.getAnnotations()) {
			if (ACTIONS.containsKey(annotation.annotationType())) {
				return ACTIONS.get(annotation.annotationType()).computeValue(request, parameter);
			}
		}
		return null;
	}

	/**
	 * Specific behavior to compute the parameter of a custom REST method, given a REST request.
	 * <br>
	 * Every behavior for every applicable annotation should be put into {@link #ACTIONS}.
	 * @param request   the REST request
	 * @param parameter the method parameter
	 * @return the value for the parameter, given the actual REST request.
	 */
	Object computeValue(RestRequest request, Parameter parameter);
}
