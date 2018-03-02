package org.yop.orm.gen;

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
import java.util.Set;

/**
 * Prepare some SQLite database with delete on exit.
 */
public class Prepare {

	/**
	 * Create an SQLite database with the given name and for the given package prefix.
	 * The db will be stored in a temp file with the 'delete on exit' flag.
	 * @param name          the SQLite db name.
	 * @param packagePrefix the package prefix to scan for Yopable objects
	 * @return the db file
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
	 * @return the MySQL connection
	 * @throws ClassNotFoundException {@link com.mysql.jdbc.Driver} not found
	 * @throws SQLException SQL error opening connection
	 */
	public static Connection getMySQLConnection() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		String connectionString = "jdbc:mysql://localhost:3306/yop?useUnicode=true&characterEncoding=utf-8";
		Connection connection = DriverManager.getConnection(connectionString, "root", "root");
		connection.prepareStatement("set foreign_key_checks=0").executeUpdate();
		return connection;
	}
}
