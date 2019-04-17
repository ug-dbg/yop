package org.yop.rest.servlet;

import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.yop.reflection.Reflection;
import org.yop.rest.annotations.Rest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link java.util.Map} of yopables where key is the REST path.
 * <br>
 * The Yopable elements should be {@link Rest} annotated.
 */
public class Yopables extends HashMap<String, Class<?>> {

	private final ClassLoader classLoader;

	/**
	 * Default constructor. {@link #classLoader} is set to the classloader of this class.
	 */
	public Yopables() {
		this.classLoader = this.getClass().getClassLoader();
	}

	/**
	 * Set a custom class loader.
	 * @param classLoader the class loader to use.
	 */
	public Yopables(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Populate the current Yopables instance with all the matching @Rest Yopable classes from the classpath.
	 * @param packageNames a comma separated list of packages. If null : do nothing. If empty : no package filter.
	 * @return the current instance
	 */
	public Yopables register(String packageNames) {
		if (packageNames == null) {
			return this;
		}
		this.register(StringUtils.split(packageNames, ","));
		return this;
	}

	/**
	 * Populate the current Yopables instance with all the matching @Rest Yopable classes from the classpath.
	 * @param packages an array of packages. If null or empty : do nothing.
	 * @return the current instance
	 */
	public Yopables register(String... packages) {
		yopables(this.classLoader, packages).forEach(this::register);
		return this;
	}

	/**
	 * Populate the current Yopables instance with the given class.
	 * @param classes @Rest classes to register
	 * @return the current instance
	 */
	public Yopables register(Class<?>... classes) {
		Arrays
			.stream(classes)
			.filter(c -> c.isAnnotationPresent(Rest.class))
			.forEach(c -> this.put(StringUtils.removeStart(c.getAnnotation(Rest.class).path(), "/"), c));
		return this;
	}

	/**
	 * Find all the {@link Rest} annotated classes using the classloader of this class.
	 * @param classLoader the class loader to use
	 * @param packages    package filter : the packages the @Rest class must start with. If empty : no package filter.
	 * @return a set of @Rest classes
	 */
	private static Set<Class<?>> yopables(ClassLoader classLoader, String... packages) {
		if (packages == null) {
			return new HashSet<>(0);
		}
		Reflections reflections = new Reflections("", classLoader);
		Set<Class<?>> candidates = reflections.getTypesAnnotatedWith(Rest.class);
		if (packages.length > 0) {
			candidates.removeIf(c -> ! StringUtils.startsWithAny(Reflection.packageName(c), packages));
		}
		return candidates;
	}
}
