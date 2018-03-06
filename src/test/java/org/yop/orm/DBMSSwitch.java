package org.yop.orm;

import org.junit.Before;
import org.junit.BeforeClass;
import org.yop.orm.gen.Prepare;
import org.yop.orm.simple.SimpleTest;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Extend this class to get a multiple DBMS support in your test case !
 * <br>
 * Use the {@link #DBMS_SWITCH} property to switch DBMS :
 * <ul>
 *     <li>mysql : requires a fresh Mysql database instance on port 3306</li>
 *     <li>postgres : requires a fresh Postgres database instance on port 5432</li>
 *     <li>sqlite : (default) use a temporary file as DB (deleted on exit)</li>
 * </ul>
 */
public abstract class DBMSSwitch {

	private static final String DBMS_SWITCH = "yop.test.dbms";
	private File db;

	protected Connection getConnection() throws SQLException, ClassNotFoundException {
		String dbms = System.getProperties().getProperty(DBMS_SWITCH, "sqlite");
		switch (dbms) {
			case "mysql" :     return Prepare.getMySQLConnection(true);
			case "postgres" :  return Prepare.getPostgresConnection();
			case "sqlite" :
			default: return Prepare.getConnection(this.db);
		}
	}

	@BeforeClass
	public static void init() {
		System.setProperty("yop.show_sql", "true");
	}

	@Before
	public void setUp() throws SQLException, IOException, ClassNotFoundException {
		String packagePrefix = "org.yop.orm";
		String dbms = System.getProperties().getProperty(DBMS_SWITCH, "sqlite");
		switch (dbms) {
			case "mysql" :     Prepare.prepareMySQL(packagePrefix);    break;
			case "postgres" :  Prepare.preparePostgres(packagePrefix); break;
			case "sqlite" :
			default: this.db = Prepare.createSQLiteDatabase(SimpleTest.class.getName(), packagePrefix);
		}
	}
}
