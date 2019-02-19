package org.yop.orm.model;

import com.google.gson.JsonElement;
import org.yop.orm.query.Context;
import org.yop.orm.query.IJoin;
import org.yop.orm.query.serialize.json.JSON;
import org.yop.orm.sql.Config;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A collection of Yopable that is easily serializable to/from JSON.
 * <br>
 * We use the {@link JSON} API to serialize/deserialize given potential join clauses.
 * @param <T> the Yopable type of the collection.
 */
public class Yopables<T extends Yopable> extends ArrayList<T> implements JsonAble {

	/** Join clauses (JSON query serialization) */
	protected IJoin.Joins<T> joins;

	private Yopables() {}

	public Yopables(IJoin.Joins<T> serializationJoins) {
		this();
		this.joins = serializationJoins == null ? new IJoin.Joins<>() : serializationJoins;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <U extends Yopable> void fromJSON(Context<U> context, JsonElement elements, Config config) {
		this.addAll(JSON.deserialize((Class) context.getTarget(), elements.getAsJsonArray()));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <U extends Yopable> JsonElement toJSON(Context<U> context) {
		return JSON.from(context.getTarget()).onto((Collection) this).join(this.joins).toJSONTree();
	}
}
