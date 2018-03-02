package org.yop.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoinTable {
    String schema() default "";
    String table();
    String sourceColumn();
    String targetColumn();
    String sourceForeignKey() default "";
    String targetForeignKey() default "";
}
