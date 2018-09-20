package org.yop.orm.evaluation;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.JsonAble;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * An evaluation is an SQL portion that can be used in a WHERE clause.
 * <br>
 * For instance : ExampleEntity→name='foo' OR ExampleEntity='bar'.
 * <br>
 * There is only one method to implement {@link #toSQL(Context, Parameters)}.
 */
public interface Evaluation extends JsonAble {

	String TYPE  = "type";
	String FIELD = "field";

	/**
	 * Build the SQL portion for the evaluation, fill the given parameters with the evaluation value(s)
	 * @param context    the current context for the evaluation
	 * @param parameters the SQL query parameters
	 * @param <T> the target evaluation type
	 * @return the SQL query portion for the evaluation, from the context
	 */
	<T extends Yopable> String toSQL(Context<T> context, Parameters parameters);

	/**
	 * Read the field @Column annotation, or the ID column for a @JoinTable
	 * @param field   the field to read
	 * @param context the context from which the column name must be built. See {@link Context#getPath()}.
	 * @return the column name. If no @Column/@JoinTable annotation, returns the class name in upper case.
	 */
	@SuppressWarnings("unchecked")
	static String columnName(Field field, Context<? extends Yopable> context) {
		if (field.isAnnotationPresent(JoinTable.class)) {
			if(Reflection.isCollection(field)) {
				Class<? extends Yopable> target = Reflection.getCollectionTarget(field);
				Context<? extends Yopable> targetContext = context.to(target, field);
				return ORMUtil.getIdColumn(targetContext);
			} else {
				Class<? extends Yopable> target = (Class<? extends Yopable>) field.getType();
				Context<? extends Yopable> targetContext = context.to(target, field);
				return ORMUtil.getIdColumn(targetContext);
			}
		}
		return context.getPath(field);
	}

	/**
	 * Adds an extra {@link #TYPE} String entry with the class name into the output JSON.
	 * <br><br>
	 * {@inheritDoc}
	 */
	@Override
	default <T extends Yopable> JsonElement toJSON(Context<T> context) {
		JsonElement element = JsonAble.super.toJSON(context);
		element.getAsJsonObject().addProperty(TYPE, this.getClass().getSimpleName());
		return element;
	}

	/**
	 * Return a new Evaluation instance of the given type.
	 * The type must match a class name from {@link org.yop.orm.evaluation} package.
	 * @param type the evaluation type (i.e. the class name).
	 * @param <T> the ne instance type
	 * @return a new Evaluation instance of the given type
	 */
	static <T> T newInstance(String type) {
		try {
			Class<T> target = Reflection.forName(Evaluation.class.getPackage().getName() + "." + type);
			return Reflection.newInstanceNoArgs(target);
		} catch (RuntimeException e) {
			throw new YopRuntimeException("Could not load Evaluation implementation [" + type + "]", e);
		}
	}

	/**
	 * A collection of {@link Evaluation} that is serializable to a JSON array.
	 */
	class Evaluations extends ArrayList<Evaluation> implements JsonAble {
		@Override
		public <T extends Yopable> void fromJSON(Context<T> context, JsonElement element) {
			for (JsonElement evaluationJSON : element.getAsJsonArray()) {
				Evaluation evaluation = Evaluation.newInstance(((JsonObject) evaluationJSON).get(TYPE).getAsString());
				evaluation.fromJSON(context, evaluationJSON);
				this.add(evaluation);
			}
		}

		@Override
		public <T extends Yopable> JsonElement toJSON(Context<T> context) {
			JsonArray evaluations = new JsonArray();
			for (Evaluation evaluation : this) {
				evaluations.add(evaluation.toJSON(context));
			}
			return evaluations;
		}
	}
}
