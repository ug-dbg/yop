package org.yop.rest.servlet;

import org.apache.http.entity.ContentType;
import org.yop.orm.evaluation.IdIn;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Select;
import org.yop.orm.query.json.JSON;
import org.yop.orm.sql.adapter.IConnection;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

class Get {

	static JSON<Yopable> doGet(
		RestRequest restRequest,
		Class<Yopable> target,
		IConnection connection) {

		Select<Yopable> select = Select.from(target);
		if (restRequest.joinAll || restRequest.joinIDs) {
			select.joinAll();
		}

		JSON<Yopable> json = JSON.from(target);
		if (restRequest.id > 0) {
			select.where(new IdIn(Collections.singletonList(restRequest.id)));
			Yopable foundByID = select.uniqueResult(connection);
			json.onto(foundByID);
		} else {
			Set<Yopable> all = select.execute(connection);
			json.onto(all);
			if (restRequest.joinIDs) {
				json.joinIDsAll();
			}
			if (restRequest.joinAll) {
				json.joinAll();
			}
		}

		if (restRequest.joinIDs) {
			json.joinIDsAll();
		}
		if (restRequest.joinAll) {
			json.joinAll();
		}

		return json;
	}

	static void doGet(
		RestRequest restRequest,
		Class<Yopable> target,
		HttpServletResponse resp,
		IConnection connection)
		throws IOException {

		JSON<Yopable> json = doGet(restRequest, target, connection);
		String content = json.toJSON();
		resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resp.setContentLength(content.getBytes(StandardCharsets.UTF_8).length);
		resp.getWriter().write(content);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

}
