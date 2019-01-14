package org.yop.orm.sql;

import org.yop.orm.query.Paging;
import org.yop.orm.util.dialect.IDialect;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * The SQL configuration details.
 * <br>
 * <ul>
 *     <li>show sql</li>
 *     <li>separator</li>
 *     <li>use sequences</li>
 *     <li>...</li>
 * </ul>
 * This config holds a default reference : {@link #DEFAULT}
 * which is initialized from the system properties or the dialect default values.
 * <br>
 * This config should be provided by the {@link org.yop.orm.sql.adapter.IConnection} connection.
 */
@SuppressWarnings("WeakerAccess")
public class Config {

	public static final Config DEFAULT = new Config().initFromSystemProperties();

	/** Classic SQL dot operator */
	public static final String DOT = ".";

	/** See {@link #defaultSequence()} */
	public static final String SQL_DEFAULT_SEQ_DEFAULT = "→DEFAULT_SEQ←";

	public static final String SHOW_SQL_PROPERTY            = "yop.show_sql";
	public static final String SQL_SEPARATOR_PROPERTY       = "yop.sql.separator";
	public static final String SQL_MAX_LENGTH_PROPERTY      = "yop.alias.max.length";
	public static final String SQL_USE_SEQUENCES_PROPERTY   = "yop.sql.sequences";
	public static final String SQL_MAX_PARAMETERS_PROPERTY  = "yop.sql.max.parameters";
	public static final String SQL_USE_BATCH_INS_PROPERTY   = "yop.sql.batch_inserts";
	public static final String SQL_DEFAULT_SEQ              = "yop.sql.default_sequence";
	public static final String SQL_PAGING_METHOD            = "yop.sql.paging_method";

	private final Map<String, String> config = new HashMap<>();
	private IDialect dialect = IDialect.defaultDialect();

	public Config initFromSystemProperties() {
		this.initFromSystemProperty(SHOW_SQL_PROPERTY, "false");
		this.initFromSystemProperty(SQL_DEFAULT_SEQ,   SQL_DEFAULT_SEQ_DEFAULT);
		return this;
	}

	/**
	 * Get the dialect associated to this config.
	 * @return {@link #dialect}
	 */
	public IDialect getDialect() {
		return this.dialect;
	}

	/**
	 * Set the dialect to use with this config
	 * @param dialect the dialect implementation to use
	 * @return the current config object, for chaining purposes
	 */
	public Config setDialect(IDialect dialect) {
		this.dialect = dialect;
		return this;
	}

	/**
	 * Get the value of a property
	 * @param key          the property key
	 * @param defaultValue the default value to return if the key does not exist in {{@link #config}}
	 * @return the value for given key - might be the default value
	 */
	public String get(String key, String defaultValue) {
		return this.config.getOrDefault(key, defaultValue);
	}

	/**
	 * Set a config property.
	 * @param key   the config key
	 * @param value the config value
	 * @return the current config instance
	 */
	public Config set(String key, String value) {
		this.config.put(key, value);
		return this;
	}

	/** DOT constant. Sue me ! */
	public String dot() {
		return DOT;
	}

	/** Write SQL queries and parameters to the logger */
	public boolean showSQL() {
		return "true".equals(this.config.get(SHOW_SQL_PROPERTY));
	}

	/** alias components separator */
	public String sqlSeparator() {
		return this.config.getOrDefault(SQL_SEPARATOR_PROPERTY, this.dialect.pathSeparator());
	}

	/** The max length allowed for aliasing in SQL */
	public int aliasMaxLength() {
		return this.config.containsKey(SQL_MAX_LENGTH_PROPERTY)
			? Integer.valueOf(this.config.get(SQL_MAX_LENGTH_PROPERTY))
			: this.dialect.aliasMaxLength();
	}

	/** use sequences (Oracle style) */
	public boolean useSequences() {
		return this.config.containsKey(SQL_USE_SEQUENCES_PROPERTY)
			? "true".equals(this.config.get(SQL_USE_SEQUENCES_PROPERTY))
			: this.dialect.useSequences();
	}

	/**
	 * Some SQL drivers does not support {@link Statement#getGeneratedKeys()} with batches.
	 * <br>
	 * Namely :
	 * <ul>
	 *     <li>SQLite → no support, please set to false</li>
	 *     <li>MSSQL → no support, please set to false</li>
	 * </ul>
	 */
	public boolean useBatchInserts() {
		return this.config.containsKey(SQL_USE_BATCH_INS_PROPERTY)
			? "true".equals(this.config.get(SQL_USE_BATCH_INS_PROPERTY))
			: this.dialect.useBatchInserts();
	}

	/** Max number of parameters in a query */
	public Integer maxParams() {
		return this.config.containsKey(SQL_MAX_PARAMETERS_PROPERTY)
			? Integer.valueOf(this.config.get(SQL_MAX_PARAMETERS_PROPERTY))
			: this.dialect.maxParameters();
	}

	/**
	 * If you set a sequence to this constant, the sequence name will be calculated :
	 * <br>
	 * <b>"seq_" + {current_class#getSimpleName()}</b>
	 * <br><br>
	 * see {@link org.yop.orm.util.ORMUtil#readSequence(Field, Config)}
	 * <br><br>
	 * The reason for this mechanism is to let you factorize the id field in an abstract class if you need.
	 */
	public String defaultSequence() {
		return this.config.get(SQL_DEFAULT_SEQ);
	}

	/**
	 * The paging method to use.
	 * Fallback is {@link org.yop.orm.query.Paging.Method#TWO_QUERIES}, which should always work.
	 * @return the paging method to use
	 */
	public Paging.Method getPagingMethod() {
		return this.config.containsKey(SQL_PAGING_METHOD)
			? Paging.Method.byName(this.config.get(SQL_PAGING_METHOD))
			: this.dialect.pagingMethod();
	}

	/**
	 * Read config value from system properties. Use default values if no property set.
	 * @param key          the system property to read
	 * @param defaultValue the default value to use if no property set
	 */
	private void initFromSystemProperty(String key, String defaultValue) {
		this.config.put(key, System.getProperties().getProperty(key, defaultValue));
	}
}
