package org.yop.orm.annotations;

import org.yop.orm.transform.ITransformer;
import org.yop.orm.transform.VoidTransformer;

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
    LengthStrategy length_strategy() default LengthStrategy.NONE;
    EnumStrategy enum_strategy() default EnumStrategy.NAME;
    Class<? extends ITransformer> transformer() default VoidTransformer.class;
}
