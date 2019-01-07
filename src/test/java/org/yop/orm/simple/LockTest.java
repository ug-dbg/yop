package org.yop.orm.simple;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.query.Select;
import org.yop.orm.query.Upsert;
import org.yop.orm.query.Where;
import org.yop.orm.simple.model.*;
import org.yop.orm.sql.adapter.IConnection;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.*;

import static org.yop.orm.Yop.*;

/**
 * Some unit tests for the {@link Select#lock()} feature.
 */
public class LockTest extends DBMSSwitch {

	private static final Logger logger = LoggerFactory.getLogger(LockTest.class);

	@Override
	protected String getPackageNames() {
		return "org.yop.orm.simple.model";
	}

	@Test
	public void testTimeOutLock() throws SQLException, ClassNotFoundException {
		Pojo newPojo = new Pojo();
		newPojo.setVersion(10564337);
		newPojo.setType(Pojo.Type.FOO);
		newPojo.setActive(true);
		Jopo jopo = new Jopo();
		jopo.setName("jopo From code !");
		jopo.setPojo(newPojo);
		newPojo.getJopos().add(jopo);

		Other other = new Other();
		other.setTimestamp(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
		other.setName("other name :)");
		newPojo.getOthers().add(other);

		Extra extra = new Extra();
		extra.setStyle("rad");
		extra.setUserName("roger");
		extra.setOther(other);
		other.setExtra(extra);

		SuperExtra superExtra = new SuperExtra();
		superExtra.setSize(123456789L);
		extra.setSuperExtra(superExtra);

		try (IConnection connection = this.getConnection()) {
			upsert(Pojo.class)
				.onto(newPojo)
				.join(toSet(Pojo::getJopos))
				.join(Pojo::getOthers, Other::getExtra, Extra::getOther)
				.join(Pojo::getOthers, Other::getExtra, Extra::getSuperExtra)
				.checkNaturalID()
				.execute(connection);

			connection.setAutoCommit(false);

			try {
				select(Pojo.class)
				.where(Where.compare(Pojo::getVersion, Operator.EQ, newPojo.getVersion()))
				.lock()
				.joinAll()
				.join(Pojo::getOthers, Other::getExtra, Extra::getOther)
				.join(Pojo::getOthers, Other::getExtra, Extra::getSuperExtra)
				.execute(connection, Select.Strategy.EXISTS);
			} catch (UnsupportedOperationException e) {
				logger.warn("Lock is not supported. Skipping test.");
				return;
			}

			// Do not try to close the connection ! There is a lock and you don't want to wait for DB timeout.
			ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
			Future<?> future = executor.submit(() -> {
				try {
					IConnection otherConnection = LockTest.this.getConnection();
					newPojo.setPassword("badpass");
					Upsert.from(Pojo.class).onto(newPojo).execute(otherConnection);
					Assert.fail("The rows should be locked for update !");
				} catch (SQLException | ClassNotFoundException e) {
					logger.error("Error executing update", e);
				}
			});

			try {
				future.get(3000, TimeUnit.MILLISECONDS);
				Assert.fail("A timeout should have been raised.");
			} catch (InterruptedException | ExecutionException e) {
				logger.error("Error waiting ! ", e);
				future.cancel(true);
				Assert.fail("Error waiting for lock to be release.");
			} catch (TimeoutException e) {
				logger.info("Timeout waiting for locked rows : success.");
			}
		}
	}

}
