package org.yop.orm.evaluation;

import org.yop.orm.annotations.Column;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * A compsarison is the most basic {@link Evaluation}.
 * <br>
 * The generated SQL is '[context]→[field] [operator] [value].
 * For instance 'EntityA→relation→EntityB→name='foo'.
 */
public class Comparison implements Evaluation {

	/** The field getter. */
	private Function<?, ?> getter;

	/** The comparison operator */
	private Operator op;

	/** The comparison value reference */
	private Comparable ref;

	/** The target field. Deduced from {@link #getter} in {@link #toSQL(Context, Parameters)}*/
	private Field field;

	/**
	 * Default constructor : gimme all I need !
	 * @param getter the field getter. {@link Reflection#findField(Class, Function)} will be used to find {@link #field}
	 * @param op     the comparison operator
	 * @param ref    the comparison reference value. Will be set as a JDBC query parameter.
	 */
	public Comparison(Function<? extends Yopable, ?> getter, Operator op, Comparable ref) {
		this.getter = getter;
		this.op = op;
		this.ref = ref;
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * Implementation details :
	 * <br>
	 * The generated SQL is '[context]→[field] [operator] ?.
	 * <br>
	 * For instance 'EntityA→relation→EntityB→name=?.
	 * <br>
	 * {@link #ref} is added to the parameters.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends Yopable> String toSQL(Context<T> context, Parameters parameters) {
		if (this.field == null) {
			this.field = Reflection.findField(context.getTarget(), (Function<T, ?>) this.getter);
		}

		if(ref != null) {
			Object refValue = ref;
			if(ref.getClass().isEnum()) {
				switch (field.getAnnotation(Column.class).enum_strategy()) {
					case NAME:    refValue = ((Enum) ref).name(); break ;
					case ORDINAL: refValue = ((Enum) ref).ordinal(); break;
					default: break;
				}
			}
			String name = context.getPath() + "#" + this.field.getName() + " " + op.toSQL() + "?";
			parameters.addParameter(name, refValue);
		}

		return this.columnName(this.field, context) + op.toSQL() + (ref == null ? "" : "?");
	}
}
