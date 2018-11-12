package org.yop.orm.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopSQLException;
import org.yop.orm.map.FirstLevelCache;
import org.yop.orm.map.Mapper;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.sql.adapter.IRequest;

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
	 * @param <T> the Yopable type the query searches for.
	 * @return the request execution ResultSet
	 * @throws YopSQLException an SQL error occurred.
	 */
	public static <T extends Yopable> Set<T> executeSelectQuery(IConnection connection, Query query, Class<T> target) {
		return executeSelectQuery(connection, query, target, new FirstLevelCache(connection.config()));
	}

	/**
	 * Execute the given SQL SELECT query and map results.
	 * <br>
	 * If the <b>yop.show_sql</b> system property is set, the SQL request is logged.
	 * <br>
	 * This method handles the too long aliases that might be present in the SQL query. At least I hope so :)
	 * @param connection the SQL connection to use
	 * @param query      the SQL query
	 * @param target     the target class on which the results of the query will be mapped
	 * @param cache      first level cache to use when mapping objects. The idea is to share it among requests.
	 * @param <T> the Yopable type the query searches for.
	 * @return the request execution ResultSet
	 * @throws YopSQLException an SQL error occurred.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Yopable> Set<T> executeSelectQuery(
		IConnection connection,
		Query query,
		Class<T> target,
		FirstLevelCache cache) {
		return executeQuery(connection, query, results -> Mapper.map(results, target, cache));
	}

	/**
	 * Execute the given SQL query. Whether the query did return something or not,
	 * nothing is done after the execution.
	 * <br>
	 * Any generated ID should however be pushed back to the query {@link Query#pushGeneratedIds()}.
	 * <br>
	 * If the <b>yop.show_sql</b> system property is set, the SQL request is logged.
	 * <br>
	 * This method handles the too long aliases that might be present in the SQL query. At least I hope so :)
	 * @param connection the SQL connection to use
	 * @param query      the SQL query
	 * @throws YopSQLException an SQL error occurred.
	 */
	public static void executeQuery(IConnection connection, Query query) {
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
	 * @param action     what to do with results. <br>
	 *                   If null → {@link IRequest#executeUpdate()} is called. <br>
	 *                   If not  → {@link IRequest#execute()} is called and then {@link Action#perform(Results)} <br>
	 * @return the return of {@link Action#perform(Results)}, or null if action is null.
	 * @throws YopSQLException an SQL error occurred.
	 */
	public static <T> T executeQuery(
		IConnection connection,
		Query query,
		Action<T> action) {

		if(connection.config().showSQL()) {
			logger.info("Executing SQL query [{}]", query);
		}

		try (IRequest request = connection.prepare(query)) {
			if(action == null) {
				request.executeUpdate();
				request.getQuery().pushGeneratedIds();
				return null;
			}

			return action.perform(new Results(request.execute(), query));
		} catch (SQLException e) {
			throw new YopSQLException(query, e);
		}
	}

	/**
	 * What to do on query {@link Results} ?
	 */
	public interface Action<T> {
		T perform(Results results);
	}
}
