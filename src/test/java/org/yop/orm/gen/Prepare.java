package org.yop.orm.gen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;
import org.yop.orm.util.ORMTypes;
import org.yop.orm.util.dialect.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Prepare some SQLite database with delete on exit.
 */
public class Prepare {

	private static final Logger logger = LoggerFactory.getLogger(Prepare.class);

	private static final String MYSQL_ADDRESS    = "localhost:3306/yop?useUnicode=true&characterEncoding=utf-8";
	private static final String POSTGRES_ADDRESS = "localhost:5432/yop";
	private static final String ORACLE_ADDRESS   = "localhost:1521/xe";
	private static final String MSSQL_ADDRESS    = "localhost\\master:1401";

	private static final String DBMS_USER = System.getProperty("yop.test.dbms.user", "yop");
	private static final String DBMS_PWD  = System.getProperty("yop.test.dbms.pwd",  "yop");

	/**
	 * Create an SQLite database with the given name and for the given package prefix.
	 * The db will be stored in a temp file with the 'delete on exit' flag.
	 * @param name          the SQLite db name.
	 * @param packagePrefix the package prefix to scan for Yopable objects
	 * @return the db file
	 * @throws IOException Error reading/writing the SQLite database file
	 * @throws SQLException SQL error opening connection
	 * @throws ClassNotFoundException SQLite driver not found in classpath
	 */
	public static File createSQLiteDatabase(
		String name,
		String packagePrefix)
		throws IOException, SQLException, ClassNotFoundException {

		Path dbPath = Files.createTempFile(name, "_temp.db");
		dbPath.toFile().deleteOnExit();

		try (Connection connection = getConnection(dbPath.toFile())) {
			prepare(packagePrefix, connection, SQLite.INSTANCE);
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
	public static Connection getConnection(File db) throws ClassNotFoundException, SQLException {
		String url = "jdbc:sqlite:" + db.getAbsolutePath();
		Class.forName("org.sqlite.JDBC");

		SQLiteConfig config = new SQLiteConfig();
		config.enforceForeignKeys(true);

		return DriverManager.getConnection(url, config.toProperties());
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
	public static Connection getMySQLConnection(boolean foreignKeyChecks) throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		String connectionString = "jdbc:mysql://" + MYSQL_ADDRESS;
		Connection connection = DriverManager.getConnection(connectionString, DBMS_USER, DBMS_PWD);
		connection.prepareStatement("set foreign_key_checks=" + (foreignKeyChecks ? "1" : "0")).executeUpdate();
		return connection;
	}

	/**
	 * Prepare the local MySQL database for tests.
	 * <br><b>⚠⚠⚠  i.e. DROP AND RE-CREATE EVERY TABLE THAT MATCHES THE GIVEN PACKAGE PREFIX! ⚠⚠⚠ </b>
	 * <br>
	 * {@link com.mysql.jdbc.Driver} should be in the classpath.
	 * @param packagePrefix the package prefix to scan for Yopable objects
	 * @throws ClassNotFoundException {@link com.mysql.jdbc.Driver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static void prepareMySQL(String packagePrefix) throws SQLException, ClassNotFoundException {
		try (Connection connection = getMySQLConnection(false)) {
			prepare(packagePrefix, connection, MySQL.INSTANCE);
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
	public static Connection getPostgresConnection() throws ClassNotFoundException, SQLException {
		Class.forName("org.postgresql.Driver");
		String connectionString = "jdbc:postgresql://" + POSTGRES_ADDRESS;
		return DriverManager.getConnection(connectionString, DBMS_USER, DBMS_PWD);
	}

	/**
	 * Prepare the local Postgres database for tests.
	 * <br><b>⚠⚠⚠  i.e. DROP AND RE-CREATE EVERY TABLE THAT MATCHES THE GIVEN PACKAGE PREFIX! ⚠⚠⚠ </b>
	 * <br>
	 * {@link org.postgresql.Driver} should be in the classpath.
	 * @param packagePrefix the package prefix to scan for Yopable objects
	 * @throws ClassNotFoundException {@link org.postgresql.Driver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static void preparePostgres(String packagePrefix) throws SQLException, ClassNotFoundException {
		try (Connection connection = getPostgresConnection()) {
			prepare(packagePrefix, connection, Postgres.INSTANCE);
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
	public static Connection getOracleConnection() throws ClassNotFoundException, SQLException {
		Class.forName("oracle.jdbc.driver.OracleDriver");
		String connectionString = "jdbc:oracle:thin:@//" + ORACLE_ADDRESS;
		return DriverManager.getConnection(connectionString, DBMS_USER, DBMS_PWD);
	}

	/**
	 * Prepare the local Oracle database for tests.
	 * <br><b>⚠⚠⚠  i.e. DROP AND RE-CREATE EVERY TABLE THAT MATCHES THE GIVEN PACKAGE PREFIX! ⚠⚠⚠ </b>
	 * <br>
	 * {@link oracle.jdbc.driver.OracleDriver} should be in the classpath.
	 * @param packagePrefix the package prefix to scan for Yopable objects
	 * @throws ClassNotFoundException {@link oracle.jdbc.driver.OracleDriver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static void prepareOracle(String packagePrefix) throws SQLException, ClassNotFoundException {
		try (Connection connection = getOracleConnection()) {
			prepare(packagePrefix, connection, Oracle.INSTANCE);
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
	public static Connection getMSSQLConnection() throws ClassNotFoundException, SQLException {
		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		String connectionString = "jdbc:sqlserver://" + MSSQL_ADDRESS;
		return DriverManager.getConnection(connectionString, DBMS_USER, DBMS_PWD);
	}

	/**
	 * Prepare the local MSSQL database for tests.
	 * <br><b>⚠⚠⚠  i.e. DROP AND RE-CREATE EVERY TABLE THAT MATCHES THE GIVEN PACKAGE PREFIX! ⚠⚠⚠ </b>
	 * <br>
	 * {@link com.microsoft.sqlserver.jdbc.SQLServerDriver} should be in the classpath.
	 * @param packagePrefix the package prefix to scan for Yopable objects
	 * @throws ClassNotFoundException {@link com.microsoft.sqlserver.jdbc.SQLServerDriver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static void prepareMSSQL(String packagePrefix) throws SQLException, ClassNotFoundException {
		try (Connection connection = getMSSQLConnection()) {
			prepare(packagePrefix, connection, MSSQL.INSTANCE);
		}
	}

	/**
	 * Prepare the target DB using the script from {@link ORMTypes#generateScript(String)}.
	 * <br>
	 * Please use the correct {@link ORMTypes} instance for the given connection ;-)
	 * @param packagePrefix the package prefix (find all Yopables)
	 * @param connection    the DB connection to use
	 * @param dialect       the dialect (one of the singleton from the {@link org.yop.orm.util.dialect} package)
	 * @throws SQLException an error occured running and committing the generation script
	 */
	private static void prepare(String packagePrefix, Connection connection, ORMTypes dialect) throws SQLException {
		connection.setAutoCommit(false);
		for (String line : dialect.generateScript(packagePrefix)) {
			try {
				Executor.executeQuery(connection, new Query(line, new Parameters()));
			} catch (RuntimeException e) {
				logger.warn("Error executing script line [" + line + "]", e);
			}
		}
		connection.commit();
	}
}
