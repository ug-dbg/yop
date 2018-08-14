package org.yop.orm.query.json;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.JsonSerializationContext;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.model.Yopable;

import java.lang.reflect.Type;

/**
 * Custom strategy for {@link Yopable} : do not serialize {@link JoinTable} annotated fields.
 * <br>
 * Those will be serialized using {@link JSON.Serializer#serialize(YopableForJSON, Type, JsonSerializationContext)}.
 */
class YopableStrategy implements ExclusionStrategy {
	@Override
	public boolean shouldSkipField(FieldAttributes f) {
		return f.getAnnotation(JoinTable.class) != null;
	}

	@Override
	public boolean shouldSkipClass(Class<?> clazz) {
		return false;
	}
}
