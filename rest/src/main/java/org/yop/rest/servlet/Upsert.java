package org.yop.rest.servlet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponses;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.json.JSON;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.util.Reflection;
import org.yop.rest.openapi.OpenAPIUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.*;

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

	/**
	 * Execute the "upsert" operation using a {@link org.yop.orm.query.Upsert} query.
	 * <br>
	 * Read the joinAll & joinIDs parameters.
	 * @param restRequest the incoming request
	 * @param connection the JDBC (or other) underlying connection
	 * @return the incoming yopables (see {@link RestRequest#contentAsYopables()}) with their IDs set.
	 */
	@Override
	public ExecutionOutput executeDefault(RestRequest restRequest, IConnection connection) {
		// The yopables to insert (i.e id is null) will have their id set after Upsert#execute.
		Collection<Yopable> output = new ArrayList<>();
		Class<Yopable> target = restRequest.getRestResource();
		Collection<org.yop.orm.query.Upsert<Yopable>> upserts = new ArrayList<>();

		if (restRequest.isPartial()) {
			for (JsonElement element : new JsonParser().parse(restRequest.getContent()).getAsJsonArray()) {
				org.yop.orm.query.Upsert<Yopable> upsert = org.yop.orm.query.Upsert.from(target);
				partial(upsert, element.getAsJsonObject());
				Yopable yopable = JSON.from(target, element.getAsJsonObject());
				upsert.onto(yopable);
				output.add(yopable);
				upserts.add(upsert);
			}
		} else {
			output.addAll(restRequest.contentAsYopables());
			upserts.add(org.yop.orm.query.Upsert.from(target).onto(output));
		}

		for (org.yop.orm.query.Upsert<Yopable> upsert : upserts) {
			if (restRequest.joinAll()) {
				upsert.joinAll();
			}
			if (restRequest.checkNaturalID()) {
				upsert.checkNaturalID();
			}
			if (restRequest.joinIDs()) {
				logger.warn("Should check related IDs to join! Not implemented yet!");
			}
			upsert.execute(connection);
		}
		return ExecutionOutput.forOutput(output);
	}

	@Override
	public Operation openAPIDefaultModel(Class<? extends Yopable> yopable) {
		String resource = OpenAPIUtil.getResourceName(yopable);
		Operation upsert = new Operation();
		upsert.setSummary("Do upsert operation on [" + resource + "]");
		upsert.setDescription(
			"The UPSERT operation does a YOP UPSERT request. "
			+ "If an object to upsert has an ID → UPDATE"
		);
		upsert.setResponses(new ApiResponses());
		upsert.setParameters(new ArrayList<>());
		upsert.getParameters().add(HttpMethod.joinAllParameter(resource));
		upsert.getParameters().add(HttpMethod.joinIDsParameter(resource));
		upsert.getParameters().add(HttpMethod.checkNaturalIDParameter(resource));
		upsert.getParameters().add(HttpMethod.partialParameter(resource));
		upsert.requestBody(new RequestBody().content(new Content().addMediaType(
			ContentType.APPLICATION_JSON.getMimeType(),
			new MediaType().schema(new ArraySchema().items(OpenAPIUtil.refSchema(yopable)))))
		);

		upsert.getResponses().addApiResponse(String.valueOf(SC_OK),                    HttpMethod.http200(yopable));
		upsert.getResponses().addApiResponse(String.valueOf(SC_BAD_REQUEST),           HttpMethod.http400());
		upsert.getResponses().addApiResponse(String.valueOf(SC_UNAUTHORIZED),          HttpMethod.http401());
		upsert.getResponses().addApiResponse(String.valueOf(SC_FORBIDDEN),             HttpMethod.http403());
		upsert.getResponses().addApiResponse(String.valueOf(SC_NOT_FOUND),             HttpMethod.http404());
		upsert.getResponses().addApiResponse(String.valueOf(SC_INTERNAL_SERVER_ERROR), HttpMethod.http500());
		return upsert;
	}

	/**
	 * Set the fields to be updated in the {@link org.yop.orm.query.Upsert} query.
	 * @param upsert the upsert query
	 * @param source the source object. Its attributes will be used to find the target fields for update.
	 */
	private static void partial(org.yop.orm.query.Upsert<Yopable> upsert, JsonObject source) {
		for (Map.Entry<String, JsonElement> field : source.entrySet()) {
			if (source.get(field.getKey()).isJsonPrimitive()) {
				upsert.onFields(t -> Reflection.readField(field.getKey(), t));
			}
		}
	}
}
