package org.yop.rest.servlet;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.exception.YopBadContentException;

import java.util.ArrayList;
import java.util.List;

/**
 * Specific 'UPSERT' HTTP method.
 * <br>
 * It does an {@link org.yop.orm.query.Upsert} on the request entities.
 */
public class Upsert implements HttpMethod {

	private static final Logger logger = LoggerFactory.getLogger(Upsert.class);

	/** The HTTP method name : UPSERT */
	public static final String UPSERT = "UPSERT";

	static final HttpMethod INSTANCE = new Upsert();

	@Override
	public Object executeDefault(RestRequest restRequest, IConnection connection) {
		org.yop.orm.query.Upsert<Yopable> upsert = org.yop.orm.query.Upsert
			.from(restRequest.getRestResource())
			.onto(readInputJSON(restRequest));

		if (restRequest.joinAll()) {
			upsert.joinAll();
		}
		if (restRequest.checkNaturalID()) {
			upsert.checkNaturalID();
		}
		if(restRequest.joinIDs()) {
			logger.warn("Should check related IDs to join! Not implemented yet!");
		}
		upsert.execute(connection);
		return "";
	}

	private static List<Yopable> readInputJSON(RestRequest restRequest) {
		try {
			JSONArray objects = new JSONArray(restRequest.getContent());
			List<Yopable> out = new ArrayList<>();
			for (Object object : objects) {
				out.add(new Gson().fromJson(object.toString(), restRequest.getRestResource()));
			}
			return out;
		} catch (RuntimeException e) {
			throw new YopBadContentException(
				"Unable to parse JSON array [" + StringUtils.abbreviate(restRequest.getContent(), 50) + "]",
				e
			);
		}
	}
}
