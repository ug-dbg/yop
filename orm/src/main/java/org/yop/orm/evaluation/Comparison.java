package org.yop.orm.evaluation;

import com.google.gson.JsonElement;
import org.apache.commons.lang3.StringUtils;
import org.yop.orm.model.JsonAble;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.query.IJoin;
import org.yop.orm.sql.Config;
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

	public static final String REF_TYPE = "refType";

	private static final String REF = "ref";

	/** The field getter. */
	private Function<?, ?> getter;

	/** The comparison operator */
	private Operator op;

	/** The comparison value reference */
	private Comparable ref;

	/** The target field. Deduced from {@link #getter} in {@link #toSQL(Context, Parameters, Config)}*/
	private Field field;

	private Comparison(){}

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
	public <T extends Yopable> Comparison(Function<T, ?> getter, Operator op, Comparable ref) {
		this();
		this.getter = getter;
		this.op = op;
		this.ref = ref;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Yopable> JsonElement toJSON(Context<T> context) {
		if (this.field == null) {
			this.field = Reflection.findField(context.getTarget(), (Function<T, ?>) this.getter);
		}
		JsonElement element = Evaluation.super.toJSON(context);
		element.getAsJsonObject().addProperty(FIELD, this.field.getName());
		if (this.ref instanceof Path) {
			element.getAsJsonObject().addProperty(REF_TYPE, "path");
		}
		element.getAsJsonObject().add(REF, JsonAble.jsonValue(context, this.ref));
		return element;
	}

	@Override
	public <T extends Yopable> void fromJSON(Context<T> context, JsonElement element, Config config) {
		Evaluation.super.fromJSON(context, element, config);
		this.field = Reflection.get(context.getTarget(), element.getAsJsonObject().get(FIELD).getAsString());
		this.getter = o -> Reflection.readField(this.field, o);
		JsonElement ref = element.getAsJsonObject().get(REF);

		if (element.getAsJsonObject().has(REF_TYPE)) {
			String refType = element.getAsJsonObject().get(REF_TYPE).getAsString();
			if (Path.PATH_TYPE.equals(refType)) {
				String path = ref.getAsJsonObject().get(Path.PATH_TYPE).getAsString();
				String oldSeparator = ref.getAsJsonObject().get(Path.SEPARATOR).getAsString();
				this.ref = Path.explicit(
					StringUtils.replace(path, oldSeparator, config.sqlSeparator()),
					config.sqlSeparator()
				);
			}
		} else {
			this.ref = (Comparable) JsonAble.fieldValue(context, this.field, ref, config);
		}
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
	public <T extends Yopable> String toSQL(Context<T> context, Parameters parameters, Config config) {
		if (this.field == null) {
			this.field = Reflection.findField(context.getTarget(), (Function<T, ?>) this.getter);
		}

		if(this.ref != null && ! (this.ref instanceof Path)) {
			String name = context.getPath(config) + "#" + this.field.getName() + " " + this.op.toSQL() + "?";
			parameters.addParameter(name, this.ref, this.field);
		}

		return Evaluation.columnName(this.field, context, config) + this.op.toSQL() + refSQL(this.ref, context, config);
	}

	@SuppressWarnings("unchecked")
	private static String refSQL(Comparable ref, Context context, Config config) {
		if (ref == null) {
			return "";
		}
		if (ref instanceof Path) {
			return ((Path) ref).toPath(context.root().getTarget(), config);
		}
		return "?";
	}
}
