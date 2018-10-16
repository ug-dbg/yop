package org.yop.orm;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.LongTest;
import org.yop.orm.gen.Prepare;
import org.yop.orm.simple.SimpleTest;
import org.yop.orm.sql.Constants;
import org.yop.orm.sql.adapter.IConnection;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Extend this class to get a multiple DBMS support in your test case !
 * <br>
 * Use the {@link #DBMS_SWITCH} property to switch DBMS :
 * <ul>
 *     <li>sqlite : (default) use a temporary file as DB (deleted on exit)</li>
 *     <li>mysql : MySQL</li>
 *     <li>postgres : PostgreSQL</li>
 *     <li>oracle : Oracle</li>
 *     <li>mssql : Microsoft SQL-Server</li>
 * </ul>
 * Please supply the connection parameters using system properties. See {@link Prepare}.
 */
public abstract class DBMSSwitch {

	private static final Logger logger = LoggerFactory.getLogger(DBMSSwitch.class);

	private static final String DBMS_SWITCH = "yop.test.dbms";
	private File db;

	/**
	 * Get the package prefixes for which Yopable tables should be created.
	 * <br>
	 * Packages must be separated with any of the these chars :
	 * <ul>
	 *     <li>';'</li>
	 *     <li>','</li>
	 *     <li>'/'</li>
	 *     <li>'|'</li>
	 * </ul>
	 * But not '.', obviously :-D
	 * @return the Yopable data objects package prefix
	 */
	protected abstract String getPackagePrefixes();

	/**
	 * Read a classpath resource and returns its content as a String.
	 * @param path the resource path (in the classpath)
	 * @return the content of the resource
	 */
	public static String classpathResource(String path) {
		try {
			return IOUtils.toString(DBMSSwitch.class.getResourceAsStream(path));
		} catch (IOException | RuntimeException e) {
			logger.error("Could not read classpath resource [{}]", path, e);
			throw new RuntimeException("Could not read classpath resource [" + path + "]", e);
		}
	}

	/**
	 * Get the current DBMS code.
	 * @return the value of the {@link #DBMS_SWITCH} property, or 'sqlite'.
	 */
	protected static String dbms() {
		return System.getProperties().getProperty(DBMS_SWITCH, "sqlite");
	}

	/**
	 * Check if the current test class should be run.
	 * @return true if the test is not a {@link LongTest} or {@link LongTest#RUN_LONG_TESTS} is set to true.
	 */
	protected boolean check() {
		return !this.getClass().isAnnotationPresent(LongTest.class)
			|| "true".equals(System.getProperties().getProperty(LongTest.RUN_LONG_TESTS));
	}

	/**
	 * Get the underlying connection
	 * @return an {@link IConnection} for the current DBMS. See {@link #dbms()}.
	 * @throws SQLException           could not get a connection to the DB
	 * @throws ClassNotFoundException could not load the driver class
	 */
	protected IConnection getConnection() throws SQLException, ClassNotFoundException {
		switch (dbms()) {
			case "mysql" :     return Prepare.getMySQLConnection(true);
			case "postgres" :  return Prepare.getPostgresConnection();
			case "oracle" :    return Prepare.getOracleConnection();
			case "mssql" :     return Prepare.getMSSQLConnection();
			case "sqlite" :
			default: return Prepare.getConnection(this.db);
		}
	}

	@BeforeClass
	public static void init() {
		System.setProperty(Constants.SHOW_SQL_PROPERTY, "true");
	}

	@Before
	public void setUp() throws SQLException, IOException, ClassNotFoundException {
		Assume.assumeTrue(
			"DBMSSwitch.check() method returned false. Test class [" + this.getClass().getName() + "] won't be run.",
			this.check()
		);
		String[] prefixes = StringUtils.split(this.getPackagePrefixes(), " ;,/|");

		// Generate the scripts for every dialect.
		// This (artificially) improves code coverage.
		// And it is not very expensive to ensure it runs with no exception thrown.
		Arrays.stream(prefixes).forEach(Prepare::generateScripts);

		// Generate and execute the preparation scripts for the package and the target database.
		switch (dbms()) {
			case "mysql" :     Prepare.prepareMySQL(prefixes);    break;
			case "postgres" :  Prepare.preparePostgres(prefixes); break;
			case "oracle" :    Prepare.prepareOracle(prefixes);   break;
			case "mssql" :     Prepare.prepareMSSQL(prefixes);    break;
			case "sqlite" :
			default: this.db = Prepare.createSQLiteDatabase(SimpleTest.class.getName(), prefixes);
		}
	}
}
