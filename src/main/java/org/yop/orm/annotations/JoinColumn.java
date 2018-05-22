package org.yop.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A relation to another Yopable object.
 * <br>
 * You don't have to explicitly set the cardinality. Yop simply reads the relation field :
 * <ul>
 *     <li>Yopable → Collection of Yopables : one to many</li>
 *     <li>Yopable → Yopable : one to one</li>
 *     <li>Collection of Yopables → Yopable : many to one</li>
 *     <li>Anything else → error</li>
 * </ul>
 *
 * <br><br>
 * The relation can either be :
 * <ul>
 *     <li>unidirectional → local or remote is set</li>
 *     <li>bidirectional → local and remote are set</li>
 * </ul>
 * <b>
 *     There is no fallback when neither {@link #local()} nor {@link #remote()} is set !
 *     Please either set one of them or both !
 * </b>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoinColumn {
	/** The name of the join column on the local table (1 → 1 or N → 1) */
	String local()  default "";

	/** The name of the join column on the target table (1 → 1 or 1 → N) */
	String remote() default "";
}
