package org.yop.rest.servlet;

import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.yop.orm.model.Yopable;
import org.yop.rest.annotations.Rest;

import java.util.HashMap;
import java.util.Set;

/**
 * A {@link java.util.Map} of {@link Yopable} where key is the REST path.
 * <br>
 * The Yopable elements should be {@link Rest} annotated.
 */
public class Yopables extends HashMap<String, Class<? extends Yopable>> {

	/**
	 * Populate the current Yopables instance with all the matching @Rest Yopable classes from the classpath.
	 * @param packageNames a comma separated list of packages
	 */
	public void fromPackage(String packageNames) {
		Set<Class<? extends Yopable>> subtypes = new Reflections("").getSubTypesOf(Yopable.class);
		String[] packages = packageNames.split(",");
		for (Class<? extends Yopable> subtype : subtypes) {
			if (StringUtils.startsWithAny(subtype.getPackage().getName(), packages)
			&& subtype.isAnnotationPresent(Rest.class)) {
				this.put(subtype.getAnnotation(Rest.class).path(), subtype);
			}
		}
	}

}
