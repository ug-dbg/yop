package org.yop.orm.sql.adapter.jdbc;

import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.sql.adapter.IRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A JDBC connection.
 * <br>
 * Read the reason for this in the {@link IConnection} interface documentation.
 */
public class JDBCConnection implements IConnection {

	/** The underlying JDBC connection */
	private Connection connection;

	/**
	 * Default constructor : please give me the JDBC connection !
	 * @param connection the JDBC connection to use
	 */
	public JDBCConnection(Connection connection) {
		this.connection = connection;
	}

	/**
	 * {@inheritDoc}
	 * <br>
	 * See {@link Connection#close()}
	 */
	@Override
	public void close() throws SQLException {
		this.connection.close();
	}

	/**
	 * {@inheritDoc}
	 * <br>
	 * <u>JDBC Implémentation</u> :
	 * <br>
	 * Prepare the statement to be executed using the query.
	 * <br>
	 * The safe alias SQL query is used and parameters are set.
	 * <br>
	 * You are ready to go :-)
	 * @throws YopSQLException an Error occured preparing the statement with the given connection
	 */
	@Override
	public IRequest prepare(Query query) {
		String[] idColumns = query.getIdColumn();
		PreparedStatement statement;

		try {
			if (idColumns.length > 0) {
				statement = connection.prepareStatement(query.getSafeSql(), idColumns);
			} else {
				int autoGeneratedKeys =
					query.askGeneratedKeys()
					? Statement.RETURN_GENERATED_KEYS
					: Statement.NO_GENERATED_KEYS;
				statement = connection.prepareStatement(query.getSafeSql(), autoGeneratedKeys);
			}

			while (query.nextBatch()) {
				for (int i = 0; i < query.getParameters().size(); i++) {
					Parameters.Parameter parameter = query.getParameters().get(i);
					if (parameter.isSequence()) {
						throw new YopRuntimeException(
							"Parameter [" + parameter + "] is a sequence !"
							+ "It should not be here. This is probably a bug !"
						);
					}
					statement.setObject(i + 1, parameter.getValue());
				}
				statement.addBatch();
			}
		} catch (SQLException e) {
			throw new YopSQLException("Exception preparing statement for query [" + query + "]", e);
		}

		return new JDBCRequest(statement, query);
	}

	/**
	 * {@inheritDoc}
	 * <br>
	 * See {@link Connection#setAutoCommit(boolean)} ()}
	 */
	@Override
	public void setAutoCommit(boolean autocommit) throws SQLException {
		this.connection.setAutoCommit(autocommit);
	}

	/**
	 * {@inheritDoc}
	 * <br>
	 * See {@link Connection#commit()} ()}
	 */
	@Override
	public void commit() throws SQLException {
		this.connection.commit();
	}
}
