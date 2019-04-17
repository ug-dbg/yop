package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.responses.ApiResponses;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.reflection.Reflection;
import org.yop.rest.openapi.OpenAPIUtil;
import org.yop.rest.serialize.Deserializers;
import org.yop.rest.serialize.PartialDeserializers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static javax.servlet.http.HttpServletResponse.*;

/**
 * Specific 'UPSERT' HTTP method.
 * <br>
 * It does an {@link org.yop.orm.query.sql.Upsert} on the request entities.
 */
public class Upsert implements HttpMethod {

	/** The HTTP method name : UPSERT */
	public static final String UPSERT = "UPSERT";

	static final HttpMethod INSTANCE = new Upsert();

	/**
	 * Execute the "upsert" operation using a {@link org.yop.orm.query.sql.Upsert} query.
	 * <br>
	 * Read the joinAll and other parameters.
	 * @param restRequest the incoming request
	 * @param connection the JDBC (or other) underlying connection
	 * @return the incoming yopables (see {@link RestRequest#contentAsYopables()}) with their IDs set.
	 */
	@Override
	public <T> RestResponse executeDefault(RestRequest<T> restRequest, IConnection connection) {
		// The yopables to insert (i.e. id is null) will have their id set after Upsert#execute.
		Collection<T> output = new ArrayList<>();
		Class<T> target = restRequest.getRestResource();
		Collection<org.yop.orm.query.sql.Upsert<T>> upserts = new ArrayList<>();

		if (restRequest.isPartial()) {
			String contentType = restRequest.accept(Deserializers.SUPPORTED);
			PartialDeserializers.Deserializer<T> deserializer = PartialDeserializers.getFor(contentType);
			List<PartialDeserializers.Partial<T>> objects = deserializer.deserialize(target, restRequest.getContent());

			for (PartialDeserializers.Partial<T> partial : objects) {
				org.yop.orm.query.sql.Upsert<T> upsert = org.yop.orm.query.sql.Upsert.from(target);
				partial(upsert, partial.getKeys());
				upsert.onto(partial.getObject());
				output.add(partial.getObject());
				upserts.add(upsert);
			}
		} else {
			output.addAll(restRequest.contentAsYopables());
			upserts.add(org.yop.orm.query.sql.Upsert.from(target).onto(output));
		}

		for (org.yop.orm.query.sql.Upsert<T> upsert : upserts) {
			if (restRequest.joinAll()) {
				upsert.joinAll();
			}
			if (restRequest.checkNaturalID()) {
				upsert.checkNaturalID();
			}
			upsert.joinProfiles(restRequest.profiles().toArray(new String[0]));
			upsert.execute(connection);
		}
		return RestResponse.forOutput(output);
	}

	@Override
	public Operation openAPIDefaultModel(Class<?> yopable) {
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
		upsert.getParameters().add(HttpMethod.joinProfilesParameter(yopable));
		upsert.getParameters().add(HttpMethod.checkNaturalIDParameter(resource));
		upsert.getParameters().add(HttpMethod.partialParameter(resource));

		upsert.requestBody(HttpMethod.requestBody(yopable));

		upsert.getResponses().addApiResponse(String.valueOf(SC_OK),                    HttpMethod.http200(yopable));
		upsert.getResponses().addApiResponse(String.valueOf(SC_BAD_REQUEST),           HttpMethod.http400());
		upsert.getResponses().addApiResponse(String.valueOf(SC_UNAUTHORIZED),          HttpMethod.http401());
		upsert.getResponses().addApiResponse(String.valueOf(SC_FORBIDDEN),             HttpMethod.http403());
		upsert.getResponses().addApiResponse(String.valueOf(SC_NOT_FOUND),             HttpMethod.http404());
		upsert.getResponses().addApiResponse(String.valueOf(SC_INTERNAL_SERVER_ERROR), HttpMethod.http500());
		return upsert;
	}

	/**
	 * Set the fields to be updated in the {@link org.yop.orm.query.sql.Upsert} query.
	 * @param upsert the upsert query
	 * @param keys   The 'partial' attributes. Will be used to find the target fields for update.
	 */
	private static <T> void partial(org.yop.orm.query.sql.Upsert<T> upsert, Set<String> keys) {
		keys.forEach(attribute -> upsert.onFields(t -> Reflection.readField(attribute, t)));
	}
}
