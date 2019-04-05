package org.yop.orm.evaluation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.yop.orm.exception.YopSerializableQueryException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.SQLPart;
import org.yop.orm.util.ORMUtil;
import org.yop.reflection.Reflection;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * IN clause evaluation.
 * <br>
 * Something like : '[Context][field] IN (?,?...)'
 * <br>
 * This does not work for @JoinTable fields. Sorry about that.
 */
public class In implements Evaluation {

	public static final String VALUES = "values";

	/** The field getter on which a restriction is set */
	private Function<?, ?> getter;

	/** The restriction values */
	private final Collection<Object> values = new HashSet<>();

	private In() {}

	/**
	 * Default constructor : I need the field getter and the restriction values.
	 * @param getter the field getter
	 * @param values the restriction values
	 * @param <From> the source type (holding the field)
	 * @param <To>   the target type (field type)
	 */
	public <From extends Yopable, To> In(Function<From, To> getter, Collection<To> values) {
		this();
		this.getter = getter;
		this.values.addAll(values);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y extends Yopable> CharSequence toSQL(Context<Y> context, Config config) {
		if(this.values.isEmpty()) {
			return "";
		}

		Field field = Reflection.findField(context.getTarget(), (Function<Y, ?>) this.getter);
		String column = context.getPath(config) + config.dot() + ORMUtil.getColumnName(field);

		List<SQLPart> values = this.values
			.stream()
			.map(value -> SQLPart.parameter(column + "=" + value, value, field, config))
			.collect(Collectors.toList());
		return config.getDialect().in(column, values);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Yopable> JsonElement toJSON(Context<T> context) {
		JsonObject json = Evaluation.super.toJSON(context).getAsJsonObject();
		Field field = Reflection.findField(context.getTarget(), (Function) this.getter);
		json.addProperty(FIELD, field.getName());
		json.add(VALUES, new JsonArray());
		Gson gson = new Gson();
		for (Object value : this.values) {
			json.get(VALUES).getAsJsonArray().add(gson.toJsonTree(value, field.getType()));
		}
		return json;
	}

	@Override
	public <T extends Yopable> void fromJSON(Context<T> context, JsonElement element, Config config) {
		Evaluation.super.fromJSON(context, element, config);
		String fieldName = element.getAsJsonObject().get(FIELD).getAsString();
		Field field = Reflection.get(context.getTarget(), fieldName);
		if (field == null) {
			throw new YopSerializableQueryException(
				"Bad serialized query : Could not find field [" + context.getTarget() + "#" + fieldName + "]",
				null
			);
		}
		this.getter = o -> Reflection.readField(field, o);
		Gson gson = new Gson();
		JsonArray values = element.getAsJsonObject().get(VALUES).getAsJsonArray();
		for (JsonElement value : values) {
			this.values.add(gson.fromJson(value, field.getType()));
		}
	}
}
