package org.yop.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to mark a field 'transient', if you cannot use the 'transient' keyword.
 * <br>
 * ⚠⚠⚠
 * <b>
 * 'transient' in Yop does not mean that the field will not be serialized.
 * <br>
 * It simply means it will not be implicitly joined with a 'joinAll' directive
 * but can be explicitly joined using a Join directive.
 * </b>
 * ⚠⚠⚠
 * <br>
 * This can be used in conjunction with {@link JoinProfile}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface YopTransient {}
