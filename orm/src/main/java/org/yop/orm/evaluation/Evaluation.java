package org.yop.orm.evaluation;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.JsonAble;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;
import org.yop.orm.util.ORMUtil;
import org.yop.reflection.Reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * An evaluation is an SQL portion that can be used in a WHERE clause.
 * <br>
 * For instance : ExampleEntity→name='foo' OR ExampleEntity='bar'.
 * <br>
 * There is only one method to implement {@link #toSQL(Context, Config)}.
 */
public interface Evaluation extends JsonAble {

	String TYPE  = "type";
	String FIELD = "field";

	/**
	 * Build the SQL portion for the evaluation, fill the given parameters with the evaluation value(s)
	 * @param context the current context for the evaluation
	 * @param config  the SQL config. Required to get the default column length.
	 * @param <T> the target evaluation type
	 * @return the SQL query portion for the evaluation, from the context
	 */
	<T> CharSequence toSQL(Context<T> context, Config config);

	/**
	 * Read the field @Column annotation, or the ID column for a @JoinTable
	 * @param field   the field to read
	 * @param context the context from which the column name must be built.
	 *                See {@link Context#getPath(org.yop.orm.sql.Config)}.
	 * @param config  the SQL config. Needed for the sql separator to use.
	 * @return the column name. If no @Column/@JoinTable annotation, returns the class name in upper case.
	 */
	@SuppressWarnings("unchecked")
	static String columnName(Field field, Context<?> context, Config config) {
		if (field.isAnnotationPresent(JoinTable.class)) {
			if(ORMUtil.isCollection(field)) {
				Class<? extends Yopable> target = Reflection.getCollectionTarget(field);
				Context<? extends Yopable> targetContext = context.to(target, field);
				return ORMUtil.getIdColumn(targetContext, config);
			} else {
				Class<? extends Yopable> target = (Class<? extends Yopable>) field.getType();
				Context<? extends Yopable> targetContext = context.to(target, field);
				return ORMUtil.getIdColumn(targetContext, config);
			}
		}
		return context.getPath(field, config);
	}

	/**
	 * Adds an extra {@link #TYPE} String entry with the class name into the output JSON.
	 * <br><br>
	 * {@inheritDoc}
	 */
	@Override
	default <T> JsonElement toJSON(Context<T> context) {
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
		public <T> void fromJSON(Context<T> context, JsonElement element, Config config) {
			for (JsonElement evaluationJSON : element.getAsJsonArray()) {
				Evaluation evaluation = Evaluation.newInstance(((JsonObject) evaluationJSON).get(TYPE).getAsString());
				evaluation.fromJSON(context, evaluationJSON, config);
				this.add(evaluation);
			}
		}

		@Override
		public <T> JsonElement toJSON(Context<T> context) {
			JsonArray evaluations = new JsonArray();
			for (Evaluation evaluation : this) {
				evaluations.add(evaluation.toJSON(context));
			}
			return evaluations;
		}
	}
}
