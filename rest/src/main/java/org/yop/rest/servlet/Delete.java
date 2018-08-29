package org.yop.rest.servlet;

import org.yop.orm.sql.adapter.IConnection;

public class Delete implements HttpMethod {
	@Override
	public Object executeDefault(RestRequest restRequest, IConnection connection) {
		throw new UnsupportedOperationException("Not implemented yet !");
	}
}
