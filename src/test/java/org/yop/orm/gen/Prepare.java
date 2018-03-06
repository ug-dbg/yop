package org.yop.orm.gen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.yop.orm.sql.Executor;
import org.yop.orm.sql.Parameters;
import org.yop.orm.sql.Query;
import org.yop.orm.util.ORMTypes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Set;

/**
 * Prepare some SQLite database with delete on exit.
 */
public class Prepare {

	private static final Logger logger = LoggerFactory.getLogger(Prepare.class);

	private static final String MYSQL_ADDRESS    = "localhost:3306/yop?useUnicode=true&characterEncoding=utf-8";
	private static final String POSTGRES_ADDRESS = "localhost:5432/yop";

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


		Set<Table> tables = Table.findAllInClassPath(packagePrefix, ORMTypes.SQLITE);
		try (Connection connection = getConnection(dbPath.toFile())) {
			connection.setAutoCommit(false);
			for (Table table : tables) {
				Executor.executeQuery(connection, new Query(table.toString(), new Parameters()));
			}
			connection.commit();
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
		Connection connection = DriverManager.getConnection(connectionString, "root", "root");
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
		Set<Table> tables = Table.findAllInClassPath(packagePrefix, ORMTypes.DEFAULT);
		Connection connection = getMySQLConnection(false);
		connection.setAutoCommit(false);
		for (Table table : tables) {
			try {
				Executor.executeQuery(connection, new Query("DROP TABLE " + table.qualifiedName(), new Parameters()));
			} catch (RuntimeException e) {
				logger.trace("Error dropping table [" + table.qualifiedName() + "]");
			}
			Executor.executeQuery(connection, new Query(table.toString(), new Parameters()));
		}
		connection.commit();
	}

	/**
	 * Get a localhost:5432 Postgres connection to 'yop' database.
	 * <br>
	 * {@link org.postgresql.Driver} should be in the classpath.
	 * @return the PostgresSQL connection
	 * @throws ClassNotFoundException {@link org.postgresql.Driver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static Connection getPostgresConnection() throws ClassNotFoundException, SQLException {
		Class.forName("org.postgresql.Driver");
		String connectionString = "jdbc:postgresql://" + POSTGRES_ADDRESS;
		return DriverManager.getConnection(connectionString, "yop", "yop");
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
		Set<Table> tables = Table.findAllInClassPath(packagePrefix, ORMTypes.POSTGRES);
		Connection connection = getPostgresConnection();
		connection.setAutoCommit(false);
		String query = " DROP TABLE IF EXISTS {0} CASCADE; ";

		for (Table table : tables) {
			try {
				Executor.executeQuery(
					connection,
					new Query(MessageFormat.format(query, table.qualifiedName()), new Parameters())
				);
			} catch (RuntimeException e) {
				logger.trace("Error dropping table [" + table.qualifiedName() + "]");
			}
			Executor.executeQuery(connection, new Query(table.toString(), new Parameters()));
		}

		connection.commit();
	}
}
