package org.yop.rest.servlet;

import org.apache.http.entity.ContentType;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.json.JSON;
import org.yop.orm.sql.adapter.IConnection;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

public class Head {

	static void doHead(
		RestRequest restRequest,
		Class<Yopable> target,
		HttpServletResponse resp,
		IConnection connection) {

		JSON<Yopable> json = Get.doGet(restRequest, target, connection);
		resp.setContentType(ContentType.APPLICATION_JSON.getMimeType());
		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resp.setContentLength(json.toJSON().getBytes(StandardCharsets.UTF_8).length);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

}
