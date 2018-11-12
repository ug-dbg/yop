package org.yop.orm.evaluation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.ORMUtil;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * ID restriction on several values. Can be easier to write than OR...
 */
public class IdIn  implements Evaluation {

	private static final String IN = " {0} IN ({1}) ";

	private static final String VALUES = "values";

	/** ID value restriction */
	private final Collection<Long> values = new HashSet<>();

	private IdIn() {}

	/**
	 * Default constructor : gimme all the values you have !
	 * @param values the ID value restriction. Can be empty. Must not be null.
	 */
	public IdIn(Collection<Long> values) {
		this();
		this.values.addAll(values);
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * Simply build a SQL portion : '[Context][ID column] IN (?,?...)' and fill the parameters.
	 */
	@Override
	public <Y extends Yopable> String toSQL(Context<Y> context, Parameters parameters, Config config) {
		if(this.values.isEmpty()) {
			return "";
		}

		String idColumn = ORMUtil.getIdColumn(context, config);
		Field idField = ORMUtil.getIdField(context.getTarget());

		return MessageFormat.format(
			IN,
			idColumn,
			this.values
				.stream()
				.map(value -> {parameters.addParameter(idColumn + "=" + value, value, idField); return "?";})
				.collect(Collectors.joining(","))
		);
	}

	@Override
	public <T extends Yopable> JsonElement toJSON(Context<T> context) {
		JsonObject json = Evaluation.super.toJSON(context).getAsJsonObject();
		json.add(VALUES, new JsonArray());
		this.values.forEach(json.get(VALUES).getAsJsonArray()::add);
		return json;
	}

	@Override
	public <T extends Yopable> void fromJSON(Context<T> context, JsonElement element, Config config) {
		Evaluation.super.toJSON(context);
		element.getAsJsonObject().get(VALUES).getAsJsonArray().forEach(e -> this.values.add(e.getAsLong()));
	}
}
