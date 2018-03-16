package org.yop.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field as the technical ID.
 * <br>
 * Yop requires an unique Id field of type {@link Long}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Id {

	/**
	 * @return the ID generation sequence. Defaults to empty string (no sequence)
	 */
	String sequence() default "";

	/**
	 * @return true if the ID column is autoincremented (and ID field shoud not be in insert queries) Defaults to true.
	 */
	boolean autoincrement() default true;
}
