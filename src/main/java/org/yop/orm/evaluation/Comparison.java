package org.yop.orm.evaluation;

import org.yop.orm.annotations.Column;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.query.IJoin;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * A comparison is the most basic {@link Evaluation}.
 * <br>
 * The generated SQL is '[context]→[field] [operator] [value].
 * For instance 'EntityA→relation→EntityB→name='foo'.
 * <br>
 * The comparison reference can be a {@link Path},
 * as long as it matches a declared {@link org.yop.orm.query.Select#join(IJoin)} clause.
 */
public class Comparison implements Evaluation {

	/** The field getter. */
	private final Function<?, ?> getter;

	/** The comparison operator */
	private final Operator op;

	/** The comparison value reference */
	private final Comparable ref;

	/** The target field. Deduced from {@link #getter} in {@link #toSQL(Context, Parameters)}*/
	private Field field;

	/**
	 * Default constructor : gimme all I need !
	 * <br><br>
	 * The reference can be a {@link Path},
	 * as long as it matches a declared {@link org.yop.orm.query.Select#join(IJoin)} clause.
	 * <br>
	 * @param getter the field getter. {@link Reflection#findField(Class, Function)} will be used to find {@link #field}
	 * @param op     the comparison operator
	 * @param ref    the comparison reference value. Will be set as a JDBC query parameter if applicable.
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

		if(this.ref != null && ! (this.ref instanceof Path)) {
			Object refValue = this.ref.getClass().isEnum() ? enumValue(this.field, (Enum) this.ref) : this.ref;
			String name = context.getPath() + "#" + this.field.getName() + " " + this.op.toSQL() + "?";
			parameters.addParameter(name, refValue);
		}

		return Evaluation.columnName(this.field, context) + this.op.toSQL() + refSQL(this.ref, context);
	}

	@SuppressWarnings("unchecked")
	private static String refSQL(Comparable ref, Context context) {
		if (ref == null) {
			return "";
		}
		if (ref instanceof Path) {
			return ((Path) ref).toPath(context.root().getTarget());
		}
		return "?";
	}

	private static Object enumValue(Field field, Enum ref) {
		switch (field.getAnnotation(Column.class).enum_strategy()) {
			case NAME:    return ref.name();
			case ORDINAL: return ref.ordinal();
			default: return ref;
		}
	}
}
