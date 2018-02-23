package org.yop.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
    enum LengthStrategy {CUT, EXCEPTION, NONE}
    enum EnumStrategy {NAME, ORDINAL}

    String name();
    int length() default 50;
    LengthStrategy length_stratgey() default LengthStrategy.NONE;
    EnumStrategy enum_strategy() default EnumStrategy.NAME;
}
