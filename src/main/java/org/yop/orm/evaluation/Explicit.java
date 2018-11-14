package org.yop.orm.evaluation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.yop.orm.model.JsonAble;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.Parameters;

import java.util.*;

/**
 * An evaluation where you can explicitly set the expression and parameters value.
 * <br>
 * This was originally added to make the paging development easier.
 * <br>
 * Use with caution !
 */
public class Explicit implements Evaluation {

	private static final String PARAMETERS = "parameters";

	/** The evaluation expression */
	private String expression;

	/** expression parameters map */
	private Map<String, Object> parameters = new LinkedHashMap<>();

	/**
	 * Default constructor. Provide with the expression then use the 'setParameter' methods in correct order.
	 * <br>
	 * Use '?' to indicate SQL parameters.
	 * @param expression the explicit evaluation expression
	 */
	public Explicit(String expression) {
		this.expression = expression;
	}

	/**
	 * Set a parameter value.
	 * @param key   parameter key (for logging purpose)
	 * @param value parameter value
	 * @return the current evaluation for chaining purpose
	 */
	public Explicit setParameter(String key, String value) {
		this.parameters.put(key, value);
		return this;
	}

	/**
	 * Set a parameter value.
	 * @param key   parameter key (for logging purpose)
	 * @param value parameter value
	 * @return the current evaluation for chaining purpose
	 */
	public Explicit setParameter(String key, Boolean value) {
		this.parameters.put(key, value);
		return this;
	}

	/**
	 * Set a parameter value.
	 * @param key   parameter key (for logging purpose)
	 * @param value parameter value
	 * @return the current evaluation for chaining purpose
	 */
	public Explicit setParameter(String key, Number value) {
		this.parameters.put(key, value);
		return this;
	}

	@Override
	public <T extends Yopable> String toSQL(Context<T> context, Parameters parameters, Config config) {
		this.parameters.forEach((k, v) -> parameters.addParameter(k, v, null));
		return this.expression;
	}

	@Override
	public <T extends Yopable> void fromJSON(Context<T> context, JsonElement element, Config config) {
		Evaluation.super.fromJSON(context, element, config);
		element.getAsJsonObject().getAsJsonObject(PARAMETERS).entrySet().forEach(
			e -> this.parameters.put(e.getKey(), fromJSON(e.getValue().getAsJsonPrimitive()))
		);
	}

	@Override
	public <T extends Yopable> JsonElement toJSON(Context<T> context) {
		JsonElement out = Evaluation.super.toJSON(context);
		JsonObject parametersAsJSON = new JsonObject();
		this.parameters.forEach((k, v) -> parametersAsJSON.add(k, JsonAble.jsonValue(context, v)));
		out.getAsJsonObject().add(PARAMETERS, parametersAsJSON);
		return out;
	}

	/**
	 * Get the value of a parameter (for JDBC) from the JSON primitive.
	 * @param primitive the JSON primitive.
	 * @return the parameter value
	 */
	private static Object fromJSON(JsonPrimitive primitive) {
		if (primitive.isBoolean()) {
			return primitive.getAsBoolean();
		}
		if (primitive.isNumber()) {
			return primitive.getAsDouble();
		}
		return primitive.getAsString();
	}
}
