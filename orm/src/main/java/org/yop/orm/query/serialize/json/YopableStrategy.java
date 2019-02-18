package org.yop.orm.query.serialize.json;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.JsonSerializationContext;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.serialize.json.annotations.YopJSONTransient;

import java.lang.reflect.Type;

/**
 * Custom strategy for {@link Yopable} : do not serialize {@link JoinTable} and {@link YopJSONTransient} fields.
 * <br>
 * Those will be serialized using {@link JSON.Serializer#serialize(YopableForJSON, Type, JsonSerializationContext)}.
 */
class YopableStrategy implements ExclusionStrategy {
	@Override
	public boolean shouldSkipField(FieldAttributes f) {
		return f.getAnnotation(JoinTable.class) != null || f.getAnnotation(YopJSONTransient.class) != null;
	}

	@Override
	public boolean shouldSkipClass(Class<?> clazz) {
		return false;
	}
}
