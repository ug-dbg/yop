package org.yop.rest.servlet;

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
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.openapi.OpenAPIUtil;

import java.util.ArrayList;
import java.util.Collection;

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

	@Override
	public Object executeDefault(RestRequest restRequest, IConnection connection) {
		// The yopables to insert (i.e id is null) will have their id set after Upsert#execute.
		Collection<Yopable> yopables = restRequest.contentAsJSONArray();
		org.yop.orm.query.Upsert<Yopable> upsert = org.yop.orm.query.Upsert
			.from(restRequest.getRestResource())
			.onto(yopables);

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
		return yopables;
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
}
