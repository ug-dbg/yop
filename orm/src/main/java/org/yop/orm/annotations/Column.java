package org.yop.orm.annotations;

import org.yop.orm.transform.ITransformer;
import org.yop.orm.transform.VoidTransformer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Link a field to a {@link Table} column.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
	/**
	 * Strategy when a field value is too long for the column {@link #length()}.
	 */
	enum LengthStrategy {CUT, EXCEPTION, NONE}

	/** How enums are serialized ? {@link Enum#name()} or {@link Enum#ordinal()} */
	enum EnumStrategy {NAME, ORDINAL}

	/** Be careful to avoid picking a name that your DBMS forbids ! */
	String name();

	/** Column max length. For now it is only used when creating tables. Default to 0 → Read value from config. */
	int length() default 0;

	/** If true, a 'NOT NULL' constraint will be added when creating the column. No further check is done. */
	boolean not_null() default false;

	/**
	 * What to do when a field is too long ?
	 * For now, length strategy is only implemented with {@link org.yop.orm.transform.AbbreviateTransformer}
	 */
	LengthStrategy length_strategy() default LengthStrategy.NONE;

	/**If this field is an enum : use {@link Enum#name()} or {@link Enum#ordinal()} to serialize */
	EnumStrategy enum_strategy() default EnumStrategy.NAME;

	/** A transformer that will be used in two ways : Java → SQL and SQL → Java */
	Class<? extends ITransformer> transformer() default VoidTransformer.class;
}
