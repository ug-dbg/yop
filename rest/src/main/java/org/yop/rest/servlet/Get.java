package org.yop.rest.servlet;

import org.yop.orm.evaluation.IdIn;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Select;
import org.yop.orm.sql.adapter.IConnection;

import java.util.Collections;

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
			return select.uniqueResult(connection);
		} else {
			return  select.execute(connection);
		}
	}
}
