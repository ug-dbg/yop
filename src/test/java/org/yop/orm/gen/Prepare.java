package org.yop.orm.gen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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

		Set<Table> tables = Table.findAllInClassPath(packagePrefix);
		try (Connection connection = getConnection(dbPath.toFile())) {
			for (Table table : tables) {
				try (PreparedStatement statement = connection.prepareStatement(table.toString())) {
					statement.executeUpdate();
				}
			}
		}

		return dbPath.toFile();
	}

	/**
	 * Get a connection on a given SQLite db file.
	 * @param db the db file
	 * @return a JDBC connection
	 */
	public static Connection getConnection(File db) throws ClassNotFoundException, SQLException {
		String url = "jdbc:sqlite:" + db.getAbsolutePath();
		Class.forName("org.sqlite.JDBC");
		return DriverManager.getConnection(url);
	}
}
