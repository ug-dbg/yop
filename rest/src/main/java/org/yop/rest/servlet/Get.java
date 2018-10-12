package org.yop.rest.servlet;

import io.swagger.oas.models.Operation;
import io.swagger.oas.models.responses.ApiResponses;
import org.yop.orm.evaluation.IdIn;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Select;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.exception.YopNoResultException;
import org.yop.rest.openapi.OpenAPIUtil;

import java.util.ArrayList;
import java.util.Collections;

import static javax.servlet.http.HttpServletResponse.*;

class Get implements HttpMethod {

	static final HttpMethod INSTANCE = new Get();

	Get(){}

	@Override
	public Object executeDefault(RestRequest restRequest, IConnection connection) {
		Select<Yopable> select = Select.from(restRequest.getRestResource());
		if (restRequest.joinAll() || restRequest.joinIDs()) {
			select.joinAll();
		}

		if (restRequest.getId() > 0) {
			select.where(new IdIn(Collections.singletonList(restRequest.getId())));
			Yopable uniqueResult = select.uniqueResult(connection);
			if (uniqueResult == null) {
				throw new YopNoResultException(
					"No element [" + restRequest.getRestResource().getName() + "] for ID [" + restRequest.getId() + "]"
				);
			}
			return uniqueResult;
		} else {
			return select.execute(connection);
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
		get.getParameters().add(HttpMethod.joinIDsParameter(resource));

		get.getResponses().addApiResponse(String.valueOf(SC_OK),                    HttpMethod.http200(yopable));
		get.getResponses().addApiResponse(String.valueOf(SC_BAD_REQUEST),           HttpMethod.http400());
		get.getResponses().addApiResponse(String.valueOf(SC_UNAUTHORIZED),          HttpMethod.http401());
		get.getResponses().addApiResponse(String.valueOf(SC_FORBIDDEN),             HttpMethod.http403());
		get.getResponses().addApiResponse(String.valueOf(SC_NOT_FOUND),             HttpMethod.http404());
		get.getResponses().addApiResponse(String.valueOf(SC_INTERNAL_SERVER_ERROR), HttpMethod.http500());
		return get;
	}
}
