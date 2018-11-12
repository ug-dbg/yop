package org.yop.orm.sql.adapter.jdbc;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.sql.BatchQuery;
import org.yop.orm.sql.Query;
import org.yop.orm.sql.adapter.IRequest;
import org.yop.orm.sql.adapter.IResultCursor;
import org.yop.orm.util.ORMUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A JDBC request.
 * <br>
 * It is basically a {@link PreparedStatement}.
 * <br>
 * This object also keeps a reference to the {@link Query} object.
 */
public class JDBCRequest implements IRequest {

	private static final Logger logger = LoggerFactory.getLogger(JDBCRequest.class);

	/** JDBC prepared statement */
	private final PreparedStatement statement;

	/** The query {@link #statement} comes from */
	private final Query query;

	/**
	 * Default constructor. Please give me the JDBC statement and the original query.
	 * <br>
	 * Of course, the prepared statement should have been prepared from the query.
	 * @param statement the JDBC Prepared Statement
	 * @param query     the original query
	 */
	JDBCRequest(PreparedStatement statement, Query query) {
		this.statement = statement;
		this.query = query;
	}

	@Override
	public Query getQuery() {
		return this.query;
	}

	@Override
	public void close() throws SQLException {
		this.statement.close();
	}

	@Override
	public IResultCursor execute() {
		try {
			return new JDBCCursor(this.statement.executeQuery(), this.statement, this.query);
		} catch (SQLException e) {
			throw new YopSQLException(this.query, e);
		}
	}

	@Override
	public void executeUpdate() {
		try {
			if(this.query instanceof BatchQuery) {
				this.statement.executeBatch();
			} else {
				this.statement.executeUpdate();
			}

			this.readGeneratedKey(this.statement);
		} catch (SQLException e) {
			throw new YopSQLException(this.query, e);
		}

		if (this.query.getConfig().showSQL()) {
			logger.info("Query generated IDs : {}", this.query.getGeneratedIds());
		}
	}

	/**
	 * Read the generated keys from the executed statement.
	 * <br>
	 * We rely on {@link Statement#getGeneratedKeys()}.
	 * <br>
	 * {@link Query#getIdColumn()} should be given to {@link java.sql.Connection#prepareStatement(String, String[])}
	 * <br><br>
	 * If the ID column is not correctly set :
	 * <br>
	 * Generally, the generated key is in the column #1.
	 * <br>
	 * But, Postgres for instance, put the whole row in the generated key ResultSet.
	 * <br>
	 * So we need either :
	 * <ul>
	 *     <li>Put the ID column first ALWAYS</li>
	 *     <li>Keep a reference to the {@link Query#target} class to read the right column </li>
	 * </ul>
	 * @param statement the statement that was executed
	 * @throws SQLException an SQL error occurred reading the resultset
	 */
	private void readGeneratedKey(Statement statement) throws SQLException {
		if(this.query.askGeneratedKeys()) {
			ResultSet generatedKeys = statement.getGeneratedKeys();
			int idIndex = 1;

			try {
				if (this.query.getTarget() != null) {
					String idColumn = ORMUtil.getIdColumn(this.query.getTarget());
					for (int i = 1; i <= generatedKeys.getMetaData().getColumnCount(); i++) {
						if (StringUtils.equals(idColumn, generatedKeys.getMetaData().getColumnLabel(i))) {
							idIndex = i;
							break;
						}
					}
				}
			} catch (RuntimeException e) {
				logger.debug("Error reading metadata for generated indexes. Column #[{}] will be used", idIndex, e);
			}

			while (generatedKeys.next()) {
				this.query.getGeneratedIds().add(generatedKeys.getLong(idIndex));
			}
		}
	}
}
