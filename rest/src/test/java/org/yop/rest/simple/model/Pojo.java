package org.yop.rest.simple.model;

import io.swagger.oas.annotations.Parameter;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.serialize.json.JSON;
import org.yop.orm.query.sql.Select;
import org.yop.orm.query.sql.Where;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.annotations.PathParam;
import org.yop.rest.annotations.Rest;

/**
 * This POJO simply extends the ORM model {@link Pojo} to add extra REST configuration/behavior.
 */
@Rest(path = "pojo", description = "This a POJO :-)")
public class Pojo extends org.yop.orm.simple.model.Pojo implements Yopable {
	@Rest(path = "search/{search_string}", methods = "POST")
	@Parameter(name = "test", description = "This is a test")
	public static String search(IConnection connection, @PathParam(name = "search_string") String searchString) {
		Pojo first = Select
			.from(Pojo.class)
			.where(Where.compare(Pojo::getStringColumn, Operator.LIKE, "%" + searchString + "%"))
			.uniqueResult(connection);
		return JSON.from(Pojo.class).onto(first).toJSON();
	}
}
