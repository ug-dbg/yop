package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.media.Content;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.oas.models.responses.ApiResponses;
import org.apache.http.entity.ContentType;
import org.yop.orm.evaluation.IdIn;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.openapi.OpenAPIUtil;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;

import static javax.servlet.http.HttpServletResponse.*;

/**
 * HTTP DELETE method implementation.
 * <br>
 * It simply executes a {@link org.yop.orm.query.Delete} operation.
 */
public class Delete implements HttpMethod {

	static final HttpMethod INSTANCE = new Delete();

	private Delete(){}

	/**
	 * Execute the delete operation using a {@link org.yop.orm.query.Delete} query.
	 * <br>
	 * Read the joinAll & joinIDs parameters. Returns an empty json array as string.
	 * @param restRequest the incoming request
	 * @param connection the JDBC (or other) underlying connection
	 * @return "[]"
	 */
	@Override
	public ExecutionOutput executeDefault(RestRequest restRequest, IConnection connection) {
		org.yop.orm.query.Delete<Yopable> delete = org.yop.orm.query.Delete.from(restRequest.getRestResource());
		if (restRequest.joinAll() || restRequest.joinIDs()) {
			delete.joinAll();
		}

		if (restRequest.getId() != null) {
			delete.where(new IdIn(Collections.singletonList(restRequest.getId())));
		}

		delete.joinProfiles(restRequest.profiles().toArray(new String[0]));
		delete.executeQueries(connection);
		return ExecutionOutput.forOutput("[]");
	}

	@Override
	public Operation openAPIDefaultModel(Class<? extends Yopable> yopable) {
		String resource = OpenAPIUtil.getResourceName(yopable);
		Operation delete = new Operation();
		delete.setSummary("Do delete operation on [" + resource + "]. If not ID provided, delete all entries !");
		delete.setResponses(new ApiResponses());
		delete.setParameters(new ArrayList<>());
		delete.getParameters().add(HttpMethod.joinAllParameter(resource));
		delete.getParameters().add(HttpMethod.joinIDsParameter(resource));
		delete.getParameters().add(HttpMethod.joinProfilesParameter(yopable));

		ApiResponse responseItem = new ApiResponse();
		responseItem.setDescription("Empty json array. To return the deleted objects, please override me.");
		responseItem.setContent(new Content());
		responseItem.getContent().addMediaType(ContentType.APPLICATION_JSON.getMimeType(), new MediaType());
		delete.getResponses().addApiResponse(String.valueOf(HttpServletResponse.SC_OK), responseItem);
		delete.getResponses().addApiResponse(String.valueOf(SC_BAD_REQUEST),           HttpMethod.http400());
		delete.getResponses().addApiResponse(String.valueOf(SC_UNAUTHORIZED),          HttpMethod.http401());
		delete.getResponses().addApiResponse(String.valueOf(SC_FORBIDDEN),             HttpMethod.http403());
		delete.getResponses().addApiResponse(String.valueOf(SC_NOT_FOUND),             HttpMethod.http404());
		delete.getResponses().addApiResponse(String.valueOf(SC_INTERNAL_SERVER_ERROR), HttpMethod.http500());
		return delete;
	}
}
