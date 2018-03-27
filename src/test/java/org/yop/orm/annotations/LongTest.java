package org.yop.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to mark the test that are pretty long to execute
 * and therefore should explicitly be activated : see {@link #RUN_LONG_TESTS}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LongTest {
	String RUN_LONG_TESTS = "yop.test.long";
}
