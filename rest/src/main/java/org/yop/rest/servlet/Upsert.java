package org.yop.rest.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.exception.YopBadContentException;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
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

	private final Gson gson;

	Upsert() {
		this.gson = new GsonBuilder().registerTypeAdapter(
			LocalDate.class,
			(JsonDeserializer) (json, typeOfT, context) -> LocalDate.parse(json.getAsJsonPrimitive().getAsString())
		).create();
	}

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

	private List<Yopable> readInputJSON(RestRequest restRequest) {
		try {
			JSONArray objects = new JSONArray(restRequest.getContent());
			List<Yopable> out = new ArrayList<>();
			for (Object object : objects) {
				out.add(this.gson.fromJson(object.toString(), restRequest.getRestResource()));
			}
			return out;
		} catch (RuntimeException e) {
			throw new YopBadContentException(
				"Unable to parse JSON array [" + StringUtils.abbreviate(restRequest.getContent(), 50) + "]",
				e
			);
		}
	}

	@Override
	public Operation openAPIDefaultModel(String resource) {
		Operation upsert = new Operation();
		upsert.setSummary("Do upsert operation on [" + resource + "]");
		upsert.setDescription(
			"The UPSERT operation does a YOP UPSERT request. "
			+ "If an object to upsert has an ID → UPDATE"
		);
		upsert.setResponses(new ApiResponses());
		upsert.setParameters(new ArrayList<>());
		upsert.requestBody(new RequestBody().content(new Content().addMediaType(
			ContentType.APPLICATION_JSON.getMimeType(),
			new MediaType()))
		);

		ApiResponse responseItem = new ApiResponse();
		responseItem.setDescription("A set of [" + resource + "] with generated IDs");
		responseItem.setContent(new Content());
		responseItem.getContent().addMediaType(ContentType.APPLICATION_JSON.getMimeType(), new MediaType());
		upsert.getResponses().addApiResponse(String.valueOf(HttpServletResponse.SC_OK), responseItem);
		return upsert;
	}
}
