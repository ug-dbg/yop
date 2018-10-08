package org.yop.rest.simple.model;

import io.swagger.oas.annotations.Parameter;
import io.swagger.oas.annotations.responses.ApiResponse;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.yop.orm.query.Select;
import org.yop.orm.query.json.JSON;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.annotations.ContentParam;
import org.yop.rest.annotations.PathParam;
import org.yop.rest.annotations.Rest;

/**
 * This POJO simply extends the ORM model {@link Pojo} to add extra REST configuration/behavior.
 */
@Rest(path = "pojo", description = "This a POJO :-)")
public class Pojo extends org.yop.orm.simple.model.Pojo {
	@Rest(path = "search", description = "dummy search method")
	@ApiResponse(description = "The unique result of the the search. This is dummy")
	public static String search(IConnection connection, Header[] headers) {
		Pojo first = Select.from(Pojo.class).uniqueResult(connection);
		return JSON.from(Pojo.class).onto(first).toJSON();
	}

	@Rest(path = "search", methods = "POST")
	@Parameter(name = "test", description = "This is a test")
	public static String search(
		IConnection connection,
		Header[] headers,
		@ContentParam String content,
		@PathParam String path,
		NameValuePair[] parameters) {

		Pojo first = Select.from(Pojo.class).uniqueResult(connection);
		return JSON.from(Pojo.class).onto(first).toJSON();
	}
}