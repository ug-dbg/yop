package org.yop.orm.simple;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.exception.YopJoinCycleException;
import org.yop.orm.query.Select;
import org.yop.orm.simple.model.BadCyclePojo;

public class JoinAllTest extends DBMSSwitch {

	private static final Logger logger = LoggerFactory.getLogger(JoinAllTest.class);

	@Override
	protected String getPackageNames() {
		return "org.yop.orm.simple.model";
	}

	@Test(expected = YopJoinCycleException.class)
	public void testBadCycle() {
		try {
			Select.from(BadCyclePojo.class).joinAll();
		} catch (YopJoinCycleException e) {
			logger.info("Cycle detected in joinAll", e);
			throw e;
		}
	}
}
