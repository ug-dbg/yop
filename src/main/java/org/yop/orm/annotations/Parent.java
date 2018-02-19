package org.yop.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate a parent.
 * <br>
 * This can be useful when there is a cycle on a single Yopable type parent ↔ child(ren).
 * <br>
 * The {@link org.yop.model.interfaces.Yopable#getParent()} uses this annotation.
 * <br>
 * There must be at most one parent field !
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Parent {}
