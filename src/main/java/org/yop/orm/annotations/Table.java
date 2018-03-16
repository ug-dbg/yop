package org.yop.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Link a Yopable object to an SQL table.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {
	/** Table name.  Be careful to avoid picking a name that your DBMS forbids ! */
	String name();

	/** Schema name. Does not really work for now. Please do not use. */
	String schema() default "";
}
