package org.yop.orm.sql;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.map.Mapper;
import org.yop.orm.model.Yopable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

/**
 * SQL query executor.
 * <br>
 * Hide the Checked exceptions from the user.
 * <br>
 * YOP mostly uses specific runtime exceptions, with as much context as possible.
 */
public class Executor {

	private static final Logger logger = LoggerFactory.getLogger(Executor.class);

	/**
	 * Execute the given SQL SELECT query and map results.
	 * <br>
	 * If the <b>yop.show_sql</b> system property is set, the SQL request is logged.
	 * <br>
	 * This method handles the too long aliases that might be present in the SQL query. At least I hope so :)
	 * @param connection the SQL connection to use
	 * @param query      the SQL query
	 * @param target     the target class on which the results of the query will be mapped
	 * @return the request execution ResultSet
	 * @throws YopSQLException an SQL error occured.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Yopable> Set<T> executeSelectQuery(Connection connection, Query query, Class<T> target) {
		return (Set<T>) executeQuery(connection, query, results -> Mapper.map(results, target));
	}

	/**
	 * Execute the given SQL SELECT query and map results.
	 * <br>
	 * If the <b>yop.show_sql</b> system property is set, the SQL request is logged.
	 * <br>
	 * This method handles the too long aliases that might be present in the SQL query. At least I hope so :)
	 * @param connection the SQL connection to use
	 * @param query      the SQL query
	 * @throws YopSQLException an SQL error occured.
	 */
	@SuppressWarnings("unchecked")
	public static void executeQuery(Connection connection, Query query) {
		executeQuery(connection, query, null);
	}

	/**
	 * Execute the given SQL query.
	 * <br>
	 * If the <b>yop.show_sql</b> system property is set, the SQL request is logged.
	 * <br>
	 * This method handles the too long aliases that might be present in the SQL query. At least I hope so :)
	 * @param connection the SQL connection to use
	 * @param query      the SQL query
	 * @param action     what to do with results
	 * @return the return of {@link Action#perform(Results)}
	 * @throws YopSQLException an SQL error occured.
	 */
	private static Object executeQuery(
		Connection connection,
		Query query,
		Action action) {

		Parameters parameters = query.getParameters();
		if(showSQL()) {
			logger.info("Executing SQL query [{}]", query);
		}

		try (PreparedStatement statement = connection.prepareStatement(query.getSql(), query.generatedKeyCommand())) {
			for(int i = 0; i < parameters.size(); i++) {
				Parameters.Parameter parameter = parameters.get(i);
				statement.setObject(i + 1, parameter.getValue());
			}

			if(action == null) {
				statement.executeUpdate();
				query.readGeneratedKey(statement);
				if(showSQL()) {
					logger.info("Query generated IDs : {}", query.getGeneratedIds());
				}
				return null;
			}

			return action.perform(new Results(statement.executeQuery(), query));
		} catch (SQLException e) {
			throw new YopSQLException(query, e);
		}
	}

	private static boolean showSQL() {
		return StringUtils.equals("true", System.getProperty("yop.show_sql"));
	}

	private interface Action {
		Object perform(Results results);
	}
}
