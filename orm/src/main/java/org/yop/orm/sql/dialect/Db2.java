package org.yop.orm.sql.dialect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.query.Paging;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.adapter.IConnection;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;

/**
 * IBM Db2 dialect {@link Dialect} extension.
 * <br>
 * This dialect has some specific configuration since I don't know the product at all.
 * @see <a href="https://www.ibm.com/analytics/us/en/db2/">https://www.ibm.com/analytics/us/en/db2/</a>
 */
@SuppressWarnings("unused")
public class Db2 extends Dialect {

	public static final Dialect INSTANCE = new Db2();

	private static final Logger logger = LoggerFactory.getLogger(Db2.class);

	/**
	 * Db2 seems to throw exceptions on close when there is a pending transaction.
	 * <br><br>
	 * Actually, {@link Connection#close()} states that :
	 * 'It is strongly recommended that an application explicitly commits or rolls back an active transaction
	 * prior to calling the close method.
	 * If the close method is called and there is an active transaction, the results are implementation-defined.'
	 * <br><br>
	 * So here it is : use this parameter to enable/disable the commit on close. Default is true.
	 */
	private boolean commitOnClose = true;

	/**
	 * Db2 seems not to support locking when joins are involved.
	 * <br>
	 * I cannot figure out whether this is a limitation or a misunderstanding on my part.
	 * <br>
	 * So here it is : default setting is disable locking for Db2.
	 * Use this parameter to turn it on.
	 * <br>
	 */
	private boolean enableLocking = false;

	/**
	 * Default type for DB2 :
	 */
	private Db2() {
		super("VARCHAR");
		this.setForType(Boolean.class, "SMALLINT");
		this.setForType(boolean.class, "SMALLINT");
		this.setForType(BigInteger.class, "BIGINT");
		this.setForType(BigDecimal.class, "DECIMAL(31, 26)");
	}

	/**
	 * See {@link #commitOnClose}
	 */
	public boolean isCommitOnClose() {
		return this.commitOnClose;
	}

	/**
	 * See {@link #commitOnClose}
	 */
	public void setCommitOnClose(boolean commitOnClose) {
		this.commitOnClose = commitOnClose;
	}

	/**
	 * See {@link #enableLocking}
	 */
	public boolean isEnableLocking() {
		return this.enableLocking;
	}

	/**
	 * See {@link #enableLocking}
	 */
	public void setEnableLocking(boolean enableLocking) {
		this.enableLocking = enableLocking;
	}

	@Override
	public String autoIncrementKeyWord() {
		return "GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1)";
	}

	@Override
	public boolean nullInNK() {
		return false;
	}

	@Override
	public Paging.Method pagingMethod() {
		return Paging.Method.LIMIT;
	}

	/**
	 * Db2 implementation : if {@link #commitOnClose}, try to commit any pending transaction before closing.
	 * <p>
	 * {@inheritDoc}
	 * </p>
	 */
	@Override
	public void onClose(IConnection connection) {
		if (this.commitOnClose) {
			try {
				connection.commit();
			} catch (SQLException | RuntimeException commitException) {
				try {
					logger.warn("DB2 : Error committing on close. Rollback...");
					connection.rollback();
				} catch (SQLException rollbackException) {
					logger.warn("DB2 : Error committing on close → Error rollbacking !");
				}
			}
		}
		super.onClose(connection);
	}

	@Override
	public boolean useBatchInserts() {
		return false;
	}

	/**
	 * Db2 implementation : if {@link #enableLocking} is false, an exception is thrown.
	 * <p>
	 * {@inheritDoc}
	 * </p>
	 * @throws UnsupportedOperationException if {@link #enableLocking} is set to false.
	 */
	@Override
	public String selectAndLockPattern(boolean distinct) {
		if (! this.enableLocking) {
			throw new UnsupportedOperationException("This Db2 dialect does not support locking.");
		}
		return super.selectAndLockPattern(distinct);
	}

	/**
	 * There seems to be issues using {@link PreparedStatement#setObject(int, Object)}
	 * on java.time objects with Db2 JDBC driver.
	 * <br>
	 * So here it is : I try to deal with it using toString methods or converting to java.sql objects.
	 * <br>
	 * But this is very patchy !
	 * <p>
	 * {@inheritDoc}
	 * </p>
	 */
	@Override
	public void setParameter(
		PreparedStatement statement,
		int index,
		Parameters.Parameter parameter)
		throws SQLException {

		if (parameter.getValue() instanceof LocalDateTime || parameter.getValue() instanceof LocalDate) {
			statement.setObject(index, parameter.getValue().toString());
		} else if (parameter.getValue() instanceof LocalTime) {
			statement.setObject(index, new Time(((LocalTime) parameter.getValue()).getLong(ChronoField.MILLI_OF_DAY)));
		} else {
			super.setParameter(statement, index, parameter);
		}
	}
}
