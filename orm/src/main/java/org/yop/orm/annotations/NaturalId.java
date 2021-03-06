package org.yop.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Natural ID : one or several fields on a class can be marked as an SQL unique constraint.
 * <br>
 * This annotation is then used at runtime when the user explicitly needs it.
 * For instance see {@link org.yop.orm.query.sql.Where#naturalID(Object)}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NaturalId {}
