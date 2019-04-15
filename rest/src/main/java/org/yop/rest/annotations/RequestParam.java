package org.yop.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In a custom {@link Rest} method,
 * mark a String parameter to the first parameter into the request request parameters with the given {@link #name()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface RequestParam {
	String name();
	String description() default "";
	boolean required() default false;
}
