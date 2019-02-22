package org.yop.orm.query.serialize.xml.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to mark a field 'transient', for the {@link org.yop.orm.query.serialize.xml.XML} directive.
 * <br>
 * ⚠⚠⚠
 * <b>
 * It simply means that the field will be excluded from XML serialization.
 * </b>
 * ⚠⚠⚠
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface YopXMLTransient {}
