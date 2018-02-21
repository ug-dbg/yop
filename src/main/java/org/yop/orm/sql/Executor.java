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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
	 * Execute the given SQL query and map results.
	 * <br>
	 * If the <b>yop.show_sql</b> system property is set, the SQL request is logged.
	 * <br>
	 * This method handles the too long aliases that might be present in the SQL query. At least I hope so :)
	 * @param connection the SQL connection to use
	 * @param sql        the SQL query
	 * @param parameters the SQL parameters
	 * @return the request execution ResultSet
	 * @throws YopSQLException an SQL error occured.
	 */
	public static <T extends Yopable> Set<T> executeQuery(
		Connection connection,
		String sql,
		Parameters parameters,
		Class<T> target) {

		// Search table/column aliases that are to long for SQL
		Set<String> tooLongAliases = new HashSet<>();
		for (String word : sql.split(" ")) {
			// if the word is not too long, that's OK
			// if the word contains a "." this is not an alias
			if(word.length() <= Constants.SQL_ALIAS_MAX_LENGTH || word.contains(Constants.DOT)) {
				continue;
			}
			tooLongAliases.add(word);
		}

		String safeAliasSQL = sql;
		for (String tooLongAlias : tooLongAliases) {
			String shortened = uniqueShortened(tooLongAlias);
			parameters.addTooLongAlias(tooLongAlias, shortened);
			safeAliasSQL = safeAliasSQL.replace(tooLongAlias, shortened);
		}

		if(StringUtils.equals("true", System.getProperty("yop.show_sql"))) {
			logger.info(
				"Executing SELECT SQL query [{}] with safe aliases [{}] and parameters [{}]",
				sql,
				StringUtils.equals(safeAliasSQL, sql) ? "N/A" : safeAliasSQL,
				parameters
			);
		}

		try (PreparedStatement statement = connection.prepareStatement(safeAliasSQL)) {
			for(int i = 0; i < parameters.size(); i++) {
				Parameters.Parameter parameter = parameters.get(i);
				statement.setObject(i + 1, parameter.getValue());
			}
			return Mapper.map(
				new Results(statement.executeQuery(), parameters, sql, safeAliasSQL),
				target
			);
		} catch (SQLException e) {
			throw new YopSQLException(sql, safeAliasSQL, parameters, e);
		}
	}

	/**
	 * Generate an unique shortened alias for the given one
	 * @param alias the alias that is too long
	 * @return a unique alias, generated from the shortened parameter + genererated UUID
	 */
	private static String uniqueShortened(String alias) {
		String shortened = StringUtils.substringAfterLast(alias, Constants.SQL_SEPARATOR);
		shortened = StringUtils.substring(shortened, 0, Constants.SQL_ALIAS_MAX_LENGTH - 37);
		return shortened + UUID.randomUUID();
	}
}
