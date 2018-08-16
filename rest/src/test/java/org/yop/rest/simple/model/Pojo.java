package org.yop.rest.simple.model;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.yop.orm.query.Select;
import org.yop.orm.query.json.JSON;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.annotations.Rest;

@Rest(path = "pojo")
public class Pojo extends org.yop.orm.simple.model.Pojo {
	@Rest(path = "search")
	public static String search(IConnection connection, Header[] headers) {
		Pojo first = Select.from(Pojo.class).uniqueResult(connection);
		return JSON.from(Pojo.class).onto(first).toJSON();
	}

	@Rest(path = "search", methods = "POST")
	public static String search(IConnection connection, Header[] headers, String content, NameValuePair[] parameters) {
		Pojo first = Select.from(Pojo.class).uniqueResult(connection);
		return JSON.from(Pojo.class).onto(first).toJSON();
	}
}
