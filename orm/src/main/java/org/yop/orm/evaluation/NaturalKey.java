package org.yop.orm.evaluation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.SQLExpression;
import org.yop.orm.util.ORMUtil;
import org.yop.reflection.Reflection;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Natural Key Evaluation : give me an object reference and I'll build an SQL evaluation for its natural key fields
 * @param <T> the target type
 */
public class NaturalKey<T> implements Evaluation {

	/** JSON query serialization : the reference object will be serialized as a JSON object for that key. */
	public static final String REFERENCE = "reference";

	/** The object from which is taken the Natural ID */
	private T reference;

	private NaturalKey() {}

	/**
	 * Default constructor : give me an object reference so I can read the natural ID !
	 * @param reference the target object reference
	 */
	public NaturalKey(T reference) {
		this();
		if(reference == null) {
			throw new YopRuntimeException("Natural key reference cannot be null !");
		}
		this.reference = reference;
	}

	@Override
	public <U> SQLExpression toSQL(Context<U> context, Config config) {
		List<Field> naturalKeys = ORMUtil.getNaturalKeyFields(this.reference.getClass());
		return config.getDialect().where(
			naturalKeys
				.stream()
				.map(field -> this.getFieldRestriction(context, field, config))
				.collect(Collectors.toList()
		));
	}

	@Override
	public <U> JsonElement toJSON(Context<U> context) {
		List<Field> naturalKeys = ORMUtil.getNaturalKeyFields(this.reference.getClass());
		JsonObject out = new JsonObject();
		out.addProperty(TYPE, this.getClass().getSimpleName());
		JsonObject ref = new JsonObject();
		out.add(REFERENCE, ref);
		Gson gson = new Gson();
		for (Field naturalKeyField : naturalKeys) {
			ref.add(naturalKeyField.getName(), gson.toJsonTree(Reflection.readField(naturalKeyField, this.reference)));
		}
		return out;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <U> void fromJSON(Context<U> context, JsonElement element, Config config) {
		Class target = context.getTarget();
		this.reference = (T) Reflection.newInstanceNoArgs(target);
		JsonObject ref = element.getAsJsonObject().get(REFERENCE).getAsJsonObject();
		List<Field> naturalKeys = ORMUtil.getNaturalKeyFields(target);
		Gson gson = new Gson();
		for (Field naturalKeyField : naturalKeys) {
			JsonElement fieldJSON = ref.get(naturalKeyField.getName());
			Reflection.set(naturalKeyField, this.reference, gson.fromJson(fieldJSON, naturalKeyField.getType()));
		}
	}

	/**
	 * Build a restriction for a field of the natural ID.
	 * @param context the current context (→ column prefix)
	 * @param field   the field on which the restriction must be built
	 * @param config  the SQL config. Required to get the default column length.
	 * @return the SQL portion for the given context→field restriction
	 */
	private SQLExpression getFieldRestriction(Context<?> context, Field field, Config config) {
		Object ref = Reflection.readField(field, this.reference);
		Parameters parameters = new Parameters();
		if(ref != null) {
			String name = context.getPath(config) + "#" + field.getName() + " = " + "?";
			parameters.addParameter(name, ref, field, false, config);
		}
		return new SQLExpression(
			Evaluation.columnName(field, context, config) + (ref == null ? " IS NULL " : "=?"),
			parameters
		);
	}

}
