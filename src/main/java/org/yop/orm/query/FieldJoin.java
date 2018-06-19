package org.yop.orm.query;

import org.yop.orm.model.Yopable;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * IJoin implementation when the Field is known.
 * <br>
 * This can save some reflection ;-)
 * @param <From> the source type
 * @param <To>   the target type
 */
class FieldJoin<From extends Yopable, To extends Yopable> extends AbstractJoin<From, To> {

	private final Field field;

	/**
	 * Create a Join clause when the field is known.
	 * @param field  the field to use for the join
	 */
	FieldJoin(Field field) {
		this.field = field;
	}

	@Override
	public Field getField(Class<From> from) {
		return this.field;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{"
			+ this.field.getDeclaringClass().getName()
			+ "#"
			+ this.field.getName()
			+ " → "
			+ this.getTarget(field).getName()
		+ "}";
	}


	@Override
	@SuppressWarnings("unchecked")
	public Class<To> getTarget(Field field) {
		if(Reflection.isCollection(field)) {
			return Reflection.getCollectionTarget(field);
		}
		return (Class<To>) field.getType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<To> getTarget(From from) {
		// Here we are asked to return a collection of objects, whatever the cardinality.
		Object target = ORMUtil.readField(this.field, from);

		// target is null → empty list
		// target is collection → target
		// target is Yopable → target, as a singleton list
		return
			target == null ? new ArrayList<>(0)  : (
				target instanceof Collection
				? (Collection<To>) target
				: Collections.singletonList((To) target)
			);
	}

}
