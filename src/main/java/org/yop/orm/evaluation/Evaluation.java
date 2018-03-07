package org.yop.orm.evaluation;


import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Constants;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.Collection;

public interface Evaluation {
	<T extends Yopable> String toSQL(Context<T> context, Parameters parameters);

	/**
	 * Read the field @Column annotation, or the ID column for a @JoinTable
	 * @param field the field to read
	 * @return the column name. If no @Column/@Jointable annotation, returns the class name in upper case.
	 */
	@SuppressWarnings("unchecked")
	default String columnName(Field field, Context<? extends Yopable> context) {
		if(field.isAnnotationPresent(Column.class)) {
			return context.getPath() + Constants.DOT + field.getAnnotation(Column.class).name();
		} else if (field.isAnnotationPresent(JoinTable.class)) {
			if(Collection.class.isAssignableFrom(field.getType())) {
				Class<? extends Yopable> target = Reflection.getCollectionTarget(field);
				Context<? extends Yopable> targetContext = context.to(target, field);
				return ORMUtil.getIdColumn(targetContext);
			} else {
				Class<? extends Yopable> target = (Class<? extends Yopable>) field.getType();
				Context<? extends Yopable> targetContext = context.to(target, field);
				return ORMUtil.getIdColumn(targetContext);
			}
		}
		return context.getPath() + Constants.DOT + field.getName().toUpperCase();
	}
}
