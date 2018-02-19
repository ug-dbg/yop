package org.yop.orm.model.json;

import com.google.gson.*;
import org.yop.orm.model.Yopable;

import java.lang.reflect.Type;

public interface YopableJson extends Yopable, JsonSerializer<Yopable> {

	@Override
	default JsonElement serialize(Yopable src, Type typeOfSrc, JsonSerializationContext context) {
		JsonElement serialized = new Gson().toJsonTree(src, typeOfSrc);
		if(src.hasParent() && serialized.isJsonObject()) {
			((JsonObject) serialized).addProperty(PARENT_ID, src.getParentId());
		}
		return serialized;
	}

	default String toJson() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(this.getClass(), this);
		return builder.create().toJson(this);
	}

}
