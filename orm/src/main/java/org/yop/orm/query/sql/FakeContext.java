package org.yop.orm.query.sql;

import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;

/**
 * A Fake context, where you can set a predetermined path.
 * <br>
 * I am not very proud of this.
 * @param <T> the target type
 */
class FakeContext<T extends Yopable> extends Context<T> {
	private final String fakePath;

	FakeContext(Context<T> context, String fakePath) {
		super(context.getTarget());
		this.fakePath = fakePath;
	}

	@Override
	public String getPath(Config config) {
		return this.fakePath;
	}
}
