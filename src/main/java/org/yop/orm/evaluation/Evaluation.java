package org.yop.orm.evaluation;


import org.yop.orm.annotations.Column;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;

import java.lang.reflect.Field;

public interface Evaluation {
	<T extends Yopable> String toSQL(Context<T> context);

	/**
	 * Read the field @Column annotation
	 * @param field the field to read
	 * @return the column name. If no @Column annotation, returns the class name in upper case.
	 */
	default String columnName(Field field) {
		if(field.isAnnotationPresent(Column.class)) {
			return field.getAnnotation(Column.class).name();
		}
		return field.getName().toUpperCase();
	}
}
