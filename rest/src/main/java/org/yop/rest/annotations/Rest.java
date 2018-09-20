package org.yop.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a {@link org.yop.orm.model.Yopable} to answer to HTTP request on a given path/method.
 * <br>
 * This annotation can also be used on a {@link org.yop.orm.model.Yopable} method to add extra behavior.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Rest {
	String path();
	String[] methods() default "GET";
	String description() default "";
	String summary() default "";
}

