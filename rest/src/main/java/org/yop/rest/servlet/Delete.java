package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import org.yop.orm.evaluation.IdIn;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.openapi.OpenAPIUtil;
import org.yop.rest.serialize.Serializers;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;

import static javax.servlet.http.HttpServletResponse.*;

/**
 * HTTP DELETE method implementation.
 * <br>
 * It simply executes a {@link org.yop.orm.query.sql.Delete} operation.
 */
public class Delete implements HttpMethod {

	static final HttpMethod INSTANCE = new Delete();

	private Delete(){}

	/**
	 * Execute the delete operation using a {@link org.yop.orm.query.sql.Delete} query.
	 * <br>
	 * Read the joinAll and other parameter. Returns an empty json array as string.
	 * @param restRequest the incoming request
	 * @param connection the JDBC (or other) underlying connection
	 * @return "[]"
	 */
	@Override
	public <T> RestResponse executeDefault(RestRequest<T> restRequest, IConnection connection) {
		org.yop.orm.query.sql.Delete<T> delete = org.yop.orm.query.sql.Delete.from(restRequest.getRestResource());
		if (restRequest.joinAll()) {
			delete.joinAll();
		}

		if (restRequest.getId() != null) {
			delete.where(new IdIn(Collections.singletonList(restRequest.getId())));
		}

		delete.joinProfiles(restRequest.profiles().toArray(new String[0]));
		delete.executeQueries(connection);
		return RestResponse.forOutput("[]");
	}

	@Override
	public Operation openAPIDefaultModel(Class<?> yopable) {
		String resource = OpenAPIUtil.getResourceName(yopable);
		Operation delete = new Operation();
		delete.setSummary("Do delete operation on [" + resource + "]. If not ID provided, delete all entries !");
		delete.setResponses(new ApiResponses());
		delete.setParameters(new ArrayList<>());
		delete.getParameters().add(HttpMethod.joinAllParameter(resource));
		delete.getParameters().add(HttpMethod.joinProfilesParameter(yopable));

		ApiResponse responseItem = new ApiResponse();
		responseItem.setDescription("Empty json array. To return the deleted objects, please override me.");
		responseItem.content(OpenAPIUtil.contentFor(new Schema<>(), Serializers.SUPPORTED));
		delete.getResponses().addApiResponse(String.valueOf(HttpServletResponse.SC_OK), responseItem);
		delete.getResponses().addApiResponse(String.valueOf(SC_BAD_REQUEST),           HttpMethod.http400());
		delete.getResponses().addApiResponse(String.valueOf(SC_UNAUTHORIZED),          HttpMethod.http401());
		delete.getResponses().addApiResponse(String.valueOf(SC_FORBIDDEN),             HttpMethod.http403());
		delete.getResponses().addApiResponse(String.valueOf(SC_NOT_FOUND),             HttpMethod.http404());
		delete.getResponses().addApiResponse(String.valueOf(SC_INTERNAL_SERVER_ERROR), HttpMethod.http500());
		return delete;
	}
}
