package org.yop.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A relation to another Yopable object.
 * <br>
 * Yop makes no difference between One-to-One, One-to-Many and Many-to-Many. You can always use a Join Table.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoinTable {
	String schema() default "";

	/** join table name */
	String table();

	/** join table source column (representing this class) */
	String sourceColumn();

	/** join table target column (representing the relation target class) */
	String targetColumn();

	/** join table foreign key to this class name (used when generating tables) */
	String sourceForeignKey() default "";

	/** join table foreign key to the target class name (used when generating tables) */
	String targetForeignKey() default "";
}
