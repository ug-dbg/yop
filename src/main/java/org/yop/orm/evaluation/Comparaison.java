package org.yop.orm.evaluation;

import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.function.Function;

public class Comparaison implements Evaluation {

	private static final String DOT = ".";

	private Function<?, ?> getter;
	private Operator op;
	private Comparable ref;

	private Field field;

	public Comparaison(Function<? extends Yopable, ?> getter, Operator op, Comparable ref) {
		this.getter = getter;
		this.op = op;
		this.ref = ref;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Yopable> String toSQL(Context<T> context, Parameters parameters) {
		if (this.field == null) {
			this.field = Reflection.findField(context.getTarget(), (Function<T, ?>) this.getter);
		}

		if(ref != null) {
			String name = context.getPath() + "#" + this.field.getName() + " " + op.toSQL() + "?";
			parameters.addParameter(name, ref);
		}

		return context.getPath() + DOT + this.columnName(this.field) + op.toSQL() + (ref == null ? "" : "?");
	}
}
