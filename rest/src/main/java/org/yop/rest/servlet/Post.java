package org.yop.rest.servlet;

import org.yop.orm.sql.adapter.IConnection;

public class Post implements HttpMethod {

	static HttpMethod INSTANCE = new Post();

	private Post(){}

	@Override
	public Object executeDefault(RestRequest restRequest, IConnection connection) {
		throw new UnsupportedOperationException("Not implemented yet !");
	}
}