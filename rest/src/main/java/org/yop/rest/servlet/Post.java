package org.yop.rest.servlet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Upsert;
import org.yop.orm.sql.adapter.IConnection;

import java.lang.reflect.Type;
import java.util.List;

public class Post implements HttpMethod {

	static HttpMethod INSTANCE = new Post();

	private Post(){}

	@Override
	public Object executeDefault(RestRequest restRequest, IConnection connection) {
		if (restRequest.getId() != null) {
			Yopable yopable = new Gson().fromJson(restRequest.getContent(), restRequest.getRestResource());
			Upsert<Yopable> upsert = Upsert.from(restRequest.getRestResource()).onto(yopable);
			if (restRequest.joinAll()) {
				upsert.joinAll();
			}
			upsert.execute(connection);
		} else {
			Type listType = new TypeToken<List<Yopable>>(){}.getType();
			List<Yopable> yopables = new Gson().fromJson(restRequest.getContent(), listType);
			Upsert<Yopable> upsert = Upsert.from(restRequest.getRestResource()).onto(yopables);
			if (restRequest.joinAll()) {
				upsert.joinAll();
			}
			upsert.execute(connection);
		}

		return "";
	}
}
