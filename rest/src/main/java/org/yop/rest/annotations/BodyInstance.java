package org.yop.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In a custom {@link Rest} method,
 * mark a String parameter to receive the HTTP request body, parsed as an instance of the yopable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface BodyInstance {}
