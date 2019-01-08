package org.yop.orm.gen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.yop.orm.sql.*;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.orm.sql.adapter.jdbc.JDBCConnection;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.dialect.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Prepare a target database (DROP and CREATE tables) and get a connection.
 * <br><br>
 * This was mostly written to prepare databases for unit tests.
 */
public class Prepare {

	private static final Logger logger = LoggerFactory.getLogger(Prepare.class);

	private static final String DBMS_HOST = System.getProperty("yop.test.dbms.host", "localhost");
	private static final String DBMS_PORT = System.getProperty("yop.test.dbms.port", "3306");
	private static final String DBMS_DB   = System.getProperty("yop.test.dbms.db",   "yop");
	private static final String DBMS_USER = System.getProperty("yop.test.dbms.user", "yop");
	private static final String DBMS_PWD  = System.getProperty("yop.test.dbms.pwd",  "yop");

	private static final String MYSQL_ADDRESS    = DBMS_HOST + ":"  + DBMS_PORT + "/" + DBMS_DB + MySQL.CONNECT_PARAMS;
	private static final String POSTGRES_ADDRESS = DBMS_HOST + ":"  + DBMS_PORT + "/" + DBMS_DB;
	private static final String ORACLE_ADDRESS   = DBMS_HOST + ":"  + DBMS_PORT + "/" + DBMS_DB;
	private static final String MSSQL_ADDRESS    = DBMS_HOST + "\\" + DBMS_DB   + ":" + DBMS_PORT;

	/**
	 * Create an SQLite database with the given name and for the given package name.
	 * The db will be stored in a temp file with the 'delete on exit' flag.
	 * @param name          the SQLite db name.
	 * @param packageNames the package names to scan for Yopable objects
	 * @return the db file
	 * @throws IOException Error reading/writing the SQLite database file
	 * @throws SQLException SQL error opening connection
	 * @throws ClassNotFoundException SQLite driver not found in classpath
	 */
	public static File createSQLiteDatabase(
		String name,
		ClassLoader classLoader,
		String... packageNames)
		throws IOException, SQLException, ClassNotFoundException {

		Path dbPath = Files.createTempFile(name, "_temp.db");
		dbPath.toFile().deleteOnExit();

		try (IConnection connection = getConnection(dbPath.toFile())) {
			for (String packageName : packageNames) {
				prepare(packageName, connection, classLoader);
			}
		}
		return dbPath.toFile();
	}

	/**
	 * Get a connection on a given SQLite db file.
	 * <br>
	 * {@link org.sqlite.JDBC} should be in the classpath.
	 * <br>
	 * Foreign keys are activated.
	 * @param db the db file
	 * @return a JDBC connection
	 * @throws ClassNotFoundException {@link org.sqlite.JDBC} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static IConnection getConnection(File db) throws ClassNotFoundException, SQLException {
		String url = "jdbc:sqlite:" + db.getAbsolutePath();
		Class.forName("org.sqlite.JDBC");

		SQLiteConfig sqliteConfig = new SQLiteConfig();
		sqliteConfig.enforceForeignKeys(true);
		Config config = new Config()
			.initFromSystemProperties()
			.set(Config.SQL_USE_BATCH_INS_PROPERTY, "false")
			.setDialect(SQLite.INSTANCE);

		return new JDBCConnection(DriverManager.getConnection(url, sqliteConfig.toProperties())).withConfig(config);
	}

	/**
	 * Get a localhost:3306 MySQL connection to 'yop' database.
	 * <br>
	 * {@link com.mysql.jdbc.Driver} should be in the classpath.
	 * @param foreignKeyChecks true to enable the foreign key checking in the driver.
	 *                         Can be useful to set to 'false' when dropping all tables.
	 *                         You probably will want to set it to true else.
	 * @return the MySQL connection
	 * @throws ClassNotFoundException {@link com.mysql.jdbc.Driver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static IConnection getMySQLConnection(boolean foreignKeyChecks) throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		String connectionString = "jdbc:mysql://" + MYSQL_ADDRESS;
		Connection connection = DriverManager.getConnection(connectionString, DBMS_USER, DBMS_PWD);
		connection.prepareStatement("set foreign_key_checks=" + (foreignKeyChecks ? "1" : "0")).executeUpdate();
		return new JDBCConnection(connection).withConfig(
			new Config().initFromSystemProperties().setDialect(MySQL.INSTANCE)
		);
	}

	/**
	 * Prepare the local MySQL database for tests.
	 * <br><b>⚠⚠⚠  i.e. DROP AND RE-CREATE EVERY TABLE THAT MATCHES THE GIVEN PACKAGE PREFIX! ⚠⚠⚠ </b>
	 * <br>
	 * {@link com.mysql.jdbc.Driver} should be in the classpath.
	 * @param packageNames the package names to scan for Yopable objects
	 * @throws ClassNotFoundException {@link com.mysql.jdbc.Driver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static void prepareMySQL(String... packageNames) throws SQLException, ClassNotFoundException {
		try (IConnection connection = getMySQLConnection(false)) {
			for (String name : packageNames) {
				prepare(name, connection, Prepare.class.getClassLoader());
			}
		}
	}

	/**
	 * Get a {@link #POSTGRES_ADDRESS} Postgres connection to 'yop' database.
	 * <br>
	 * {@link org.postgresql.Driver} should be in the classpath.
	 * @return the PostgresSQL connection
	 * @throws ClassNotFoundException {@link org.postgresql.Driver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static IConnection getPostgresConnection() throws ClassNotFoundException, SQLException {
		Class.forName("org.postgresql.Driver");
		String connectionString = "jdbc:postgresql://" + POSTGRES_ADDRESS;
		return new JDBCConnection(
			DriverManager.getConnection(connectionString, DBMS_USER, DBMS_PWD)
		).withConfig(new Config().initFromSystemProperties().setDialect(Postgres.INSTANCE));
	}

	/**
	 * Prepare the local Postgres database for tests.
	 * <br><b>⚠⚠⚠  i.e. DROP AND RE-CREATE EVERY TABLE THAT MATCHES THE GIVEN PACKAGE PREFIX! ⚠⚠⚠ </b>
	 * <br>
	 * {@link org.postgresql.Driver} should be in the classpath.
	 * @param packageNames the package names to scan for Yopable objects
	 * @throws ClassNotFoundException {@link org.postgresql.Driver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static void preparePostgres(String... packageNames) throws SQLException, ClassNotFoundException {
		try (IConnection connection = getPostgresConnection()) {
			for (String name : packageNames) {
				prepare(name, connection, Prepare.class.getClassLoader());
			}
		}
	}

	/**
	 * Get a {@link #ORACLE_ADDRESS} Oracle connection to 'xe' database.
	 * <br>
	 * {@link oracle.jdbc.driver.OracleDriver} should be in the classpath.
	 * @return the ORacle connection
	 * @throws ClassNotFoundException {@link oracle.jdbc.driver.OracleDriver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static IConnection getOracleConnection() throws ClassNotFoundException, SQLException {
		Class.forName("oracle.jdbc.driver.OracleDriver");
		String connectionString = "jdbc:oracle:thin:@//" + ORACLE_ADDRESS;
		return new JDBCConnection(DriverManager.getConnection(connectionString, DBMS_USER, DBMS_PWD)).withConfig(
			new Config().initFromSystemProperties().setDialect(Oracle.INSTANCE)
		);
	}

	/**
	 * Prepare the local Oracle database for tests.
	 * <br><b>⚠⚠⚠  i.e. DROP AND RE-CREATE EVERY TABLE THAT MATCHES THE GIVEN PACKAGE PREFIX! ⚠⚠⚠ </b>
	 * <br>
	 * {@link oracle.jdbc.driver.OracleDriver} should be in the classpath.
	 * @param packageNames the package names to scan for Yopable objects
	 * @throws ClassNotFoundException {@link oracle.jdbc.driver.OracleDriver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static void prepareOracle(String... packageNames) throws SQLException, ClassNotFoundException {
		try (IConnection connection = getOracleConnection()) {
			for (String name : packageNames) {
				prepare(name, connection, Prepare.class.getClassLoader());
			}
		}
	}

	/**
	 * Get a {@link #MSSQL_ADDRESS} MS-SQL connection to database.
	 * <br>
	 * {@link com.microsoft.sqlserver.jdbc.SQLServerDriver} should be in the classpath.
	 * @return the MSSQL connection
	 * @throws ClassNotFoundException {@link com.microsoft.sqlserver.jdbc.SQLServerDriver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static IConnection getMSSQLConnection() throws ClassNotFoundException, SQLException {
		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		String connectionString = "jdbc:sqlserver://" + MSSQL_ADDRESS;
		return new JDBCConnection(DriverManager.getConnection(connectionString, DBMS_USER, DBMS_PWD)).withConfig(
			new Config().initFromSystemProperties().setDialect(MSSQL.INSTANCE)
		);
	}

	/**
	 * Prepare the local MSSQL database for tests.
	 * <br><b>⚠⚠⚠  i.e. DROP AND RE-CREATE EVERY TABLE THAT MATCHES THE GIVEN PACKAGE PREFIX! ⚠⚠⚠ </b>
	 * <br>
	 * {@link com.microsoft.sqlserver.jdbc.SQLServerDriver} should be in the classpath.
	 * @param packageNames the package names to scan for Yopable objects
	 * @throws ClassNotFoundException {@link com.microsoft.sqlserver.jdbc.SQLServerDriver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static void prepareMSSQL(String... packageNames) throws SQLException, ClassNotFoundException {
		try (IConnection connection = getMSSQLConnection()) {
			for (String name : packageNames) {
				prepare(name, connection, Prepare.class.getClassLoader());
			}
		}
	}

	/**
	 * Generate the DB scripts for a given package for all {@link Dialect}. Do not execute anything.
	 * <br>
	 * Let's face it : this is just to improve code coverage.
	 * @param packageName the package name (find all Yopables)
	 * @param config      the SQL config (sql separator, use batch inserts...)
	 */
	public static void generateScripts(String packageName, Config config) {
		List<IDialect> ormTypes = Arrays.asList(
			SQLite.INSTANCE,
			MySQL.INSTANCE,
			Postgres.INSTANCE,
			MSSQL.INSTANCE,
			Oracle.INSTANCE
		);
		ormTypes.forEach(dialect -> ORMUtil.generateScript(packageName, config, Prepare.class.getClassLoader()));
	}

	/**
	 * Prepare the target DB using the script from {@link ORMUtil#generateScript(String, Config, ClassLoader)}.
	 * <br>
	 * Please use the correct {@link Dialect} instance for the given connection ;-)
	 * @param packageName the package name (find all Yopables)
	 * @param connection    the DB connection to use
	 * @throws SQLException an error occurred running and committing the generation script
	 */
	private static void prepare(String packageName, IConnection connection, ClassLoader classLoader) throws SQLException {
		connection.setAutoCommit(true);
		for (String line : ORMUtil.generateScript(packageName, connection.config(), classLoader)) {
			Query.Type type = Query.Type.guess(line);
			try {
				Executor.executeQuery(
					connection,
					new SimpleQuery(line, type, connection.config())
				);
			} catch (RuntimeException e) {
				if (type == Query.Type.DROP) {
					// When testing, all the 'DROP' requests possibly throw an exception. That's quite normal.
					// We want to avoid log pollution when doing standard tests : don't log the stack trace.
					logger.warn("Error executing script line [{}] : [{}]", line, e.getCause().getMessage());
				} else {
					logger.warn("Error executing script line [{}]]", line, e);
				}
			}
		}
	}
}
