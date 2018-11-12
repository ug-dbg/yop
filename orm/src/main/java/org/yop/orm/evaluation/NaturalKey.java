package org.yop.orm.evaluation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.query.Where;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Natural Key Evaluation : give me an object reference and I'll build an SQL evaluation for its natural key fields
 * @param <T> the target type
 */
public class NaturalKey<T extends Yopable> implements Evaluation {

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
	public <U extends Yopable> String toSQL(Context<U> context, Parameters parameters, Config config) {
		List<Field> naturalKeys = ORMUtil.getNaturalKeyFields(this.reference.getClass());
		return Where.toSQL(
			naturalKeys
				.stream()
				.map(field -> this.getFieldRestriction(context, field, parameters, config))
				.collect(Collectors.toList()
		));
	}

	@Override
	public <U extends Yopable> JsonElement toJSON(Context<U> context) {
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
	public <U extends Yopable> void fromJSON(Context<U> context, JsonElement element, Config config) {
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
	 * @param context    the current context (→ column prefix)
	 * @param field      the field on which the restriction must be built
	 * @param parameters the SQL query parameters (will be populated with the field value)
	 * @return the SQL portion for the given context→field restriction
	 */
	private String getFieldRestriction(Context<?> context, Field field, Parameters parameters, Config config) {
		Object ref = ORMUtil.readField(field, this.reference);
		if(ref != null) {
			String name = context.getPath(config) + "#" + field.getName() + " = " + "?";
			parameters.addParameter(name, ref, field);
		}
		return Evaluation.columnName(field, context, config) + "=" + (ref == null ? "" : "?");
	}

}
