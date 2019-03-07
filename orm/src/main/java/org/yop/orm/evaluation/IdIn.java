package org.yop.orm.evaluation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.SQLPart;
import org.yop.orm.util.ORMUtil;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ID restriction on several values. Can be easier to write than OR...
 */
public class IdIn  implements Evaluation {

	public static final String VALUES = "values";

	/** ID value restriction */
	private final Collection<Comparable> values = new HashSet<>();

	private IdIn() {}

	/**
	 * Default constructor : gimme all the values you have !
	 * @param values the ID value restriction. Can be empty. Must not be null.
	 */
	public IdIn(Collection<? extends Comparable> values) {
		this();
		this.values.addAll(values);
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * Simply build a SQL portion : '[Context][ID column] IN (?,?...)' and fill the parameters.
	 */
	@Override
	public <Y extends Yopable> CharSequence toSQL(Context<Y> context, Config config) {
		if(this.values.isEmpty()) {
			return "";
		}

		String idColumn = ORMUtil.getIdColumn(context, config);
		Field idField = ORMUtil.getIdField(context.getTarget());
		List<SQLPart> values = this.values
			.stream()
			.map(value -> SQLPart.parameter(idColumn + "=" + value, value, idField))
			.collect(Collectors.toList());

		return config.getDialect().in(idColumn, values);
	}

	@Override
	public <T extends Yopable> JsonElement toJSON(Context<T> context) {
		JsonObject json = Evaluation.super.toJSON(context).getAsJsonObject();
		json.add(VALUES, new JsonArray());
		for (Comparable value : this.values) {
			if(value instanceof Number) {
				json.get(VALUES).getAsJsonArray().add((Number) value);
			} else if(value instanceof Boolean) {
				json.get(VALUES).getAsJsonArray().add((Boolean) value);
			} else {
				json.get(VALUES).getAsJsonArray().add(String.valueOf(value));
			}
		}
		return json;
	}

	@Override
	public <T extends Yopable> void fromJSON(Context<T> context, JsonElement element, Config config) {
		Evaluation.super.toJSON(context);
		JsonArray values = element.getAsJsonObject().get(VALUES).getAsJsonArray();
		Gson gson = new Gson();
		for (JsonElement value : values) {
			this.values.add((Comparable) gson.fromJson(value, ORMUtil.getIdField(context.getTarget()).getType()));
		}

		element.getAsJsonObject().get(VALUES).getAsJsonArray().forEach(e -> this.values.add(e.getAsLong()));
	}
}
