package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.responses.ApiResponses;
import org.yop.orm.evaluation.IdIn;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.sql.Select;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.exception.YopNoResultException;
import org.yop.rest.openapi.OpenAPIUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import static javax.servlet.http.HttpServletResponse.*;

/**
 * HTTP GET method implementation.
 * <br>
 * It simply executes a {@link org.yop.orm.query.sql.Select} operation.
 */
class Get implements HttpMethod {

	static final HttpMethod INSTANCE = new Get();

	Get(){}

	/**
	 * Execute the "get" operation using a {@link org.yop.orm.query.sql.Select} query.
	 * <br>
	 * Read the joinAll and other parameters.
	 * @param restRequest the incoming request
	 * @param connection the JDBC (or other) underlying connection
	 * @return a wrapped set of {@link Yopable} or a wrapped {@link Yopable} (if {@link RestRequest#getId()} is set).
	 * @throws YopNoResultException if asked for a single element by ID and no result.
	 */
	@Override
	public ExecutionOutput executeDefault(RestRequest restRequest, IConnection connection) {
		Select<Yopable> select = Select.from(restRequest.getRestResource());
		if (restRequest.joinAll()) {
			select.joinAll();
		}

		if (restRequest.isPaging()) {
			select.page(restRequest.offset(), restRequest.limit());
		}

		select.joinProfiles(restRequest.profiles().toArray(new String[0]));

		if (restRequest.getId() != null) {
			select.where(new IdIn(Collections.singletonList(restRequest.getId())));
			Yopable uniqueResult = select.uniqueResult(connection);
			if (uniqueResult == null) {
				throw new YopNoResultException(
					"No element [" + restRequest.getRestResource().getName() + "] for ID [" + restRequest.getId() + "]"
				);
			}
			ExecutionOutput output = ExecutionOutput.forOutput(uniqueResult);
			if (restRequest.count()) {
				output.addHeader(PARAM_COUNT, "1");
			}
			return output;
		} else {
			Set<Yopable> results = select.execute(connection);
			ExecutionOutput output = ExecutionOutput.forOutput(results);
			if (restRequest.count()) {
				output.addHeader(PARAM_COUNT, String.valueOf(select.count(connection)));
			}
			return output;
		}
	}

	@Override
	public Operation openAPIDefaultModel(Class<? extends Yopable> yopable) {
		String resource = OpenAPIUtil.getResourceName(yopable);
		Operation get = new Operation();
		get.setSummary("Get all [" + resource + "] or one single object by ID");
		get.setResponses(new ApiResponses());
		get.setParameters(new ArrayList<>());
		get.getParameters().add(HttpMethod.joinAllParameter(resource));
		get.getParameters().add(HttpMethod.joinProfilesParameter(yopable));
		get.getParameters().add(HttpMethod.countParameter(resource));
		get.getParameters().add(HttpMethod.pagingOffsetParameter(resource));
		get.getParameters().add(HttpMethod.pagingLimitParameter(resource));

		get.getResponses().addApiResponse(String.valueOf(SC_OK),                    HttpMethod.http200(yopable));
		get.getResponses().addApiResponse(String.valueOf(SC_BAD_REQUEST),           HttpMethod.http400());
		get.getResponses().addApiResponse(String.valueOf(SC_UNAUTHORIZED),          HttpMethod.http401());
		get.getResponses().addApiResponse(String.valueOf(SC_FORBIDDEN),             HttpMethod.http403());
		get.getResponses().addApiResponse(String.valueOf(SC_NOT_FOUND),             HttpMethod.http404());
		get.getResponses().addApiResponse(String.valueOf(SC_INTERNAL_SERVER_ERROR), HttpMethod.http500());
		return get;
	}
}
