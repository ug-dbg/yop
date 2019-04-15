package org.yop.rest.servlet;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.util.ORMUtil;
import org.yop.rest.annotations.Rest;

import java.util.HashMap;
import java.util.Set;

/**
 * A {@link java.util.Map} of yopables where key is the REST path.
 * <br>
 * The Yopable elements should be {@link Rest} annotated.
 */
public class Yopables extends HashMap<String, Class<?>> {

	/**
	 * Populate the current Yopables instance with all the matching @Rest Yopable classes from the classpath.
	 * @param packageNames a comma separated list of packages
	 */
	void fromPackage(String packageNames) {
		if (packageNames == null) {
			return;
		}
		Set<Class> subtypes = ORMUtil.yopables(this.getClass().getClassLoader());
		String[] packages = packageNames.split(",");
		for (Class<?> subtype : subtypes) {
			if (StringUtils.startsWithAny(subtype.getPackage().getName(), packages)
			&& subtype.isAnnotationPresent(Rest.class)) {
				this.put(subtype.getAnnotation(Rest.class).path(), subtype);
			}
		}
	}

}
