package org.yop.orm.sql;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.exception.YopIncoherentQueryException;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.util.MessageUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An SQL expression that can be evaluated to an SQL string with parameters.
 * <br>
 * This class helps keeping an SQL query coherent (i.e. with the right number of parameters, in the right order)
 * <br>
 * This class is a {@link CharSequence}, i.e. a common interface with {@link String}.
 * <br>
 * You can do some basic operations that should preserve the parameters integrity :
 * <ul>
 *     <li>{@link #subSequence(int, int)}</li>
 *     <li>{@link #append(CharSequence)}</li>
 *     <li>{@link #join(String, CharSequence...)}</li>
 *     <li>{@link #forPattern(String, CharSequence...)}</li>
 * </ul>
 * Use {@link #isCoherent()} to check if the parameters number equals the number of '?' in the query.
 */
public class SQLExpression implements CharSequence {

	/** e.g. {:parameter_name} */
	private static final Pattern REPLACEMENT_PATTERN = Pattern.compile("\\{:[a-z_]+\\}");
	private static final String PARAM = "?";

	protected String sql;
	protected Parameters parameters = new Parameters();

	protected SQLExpression() {}

	/**
	 * Constructor with an sql string and no parameter. See {@link #SQLExpression(String, List)}.
	 * @param sql the sql query.
	 * @throws YopIncoherentQueryException if the query string contains at least one '?'
	 */
	public SQLExpression(String sql) {
		this(sql, new ArrayList<>(0));
	}

	/**
	 * Constructor with an sql string and an explicit parameter.
	 * @param sql            the sql query.
	 * @param parameterName  the query parameter name
	 * @param parameterValue the query parameter value
	 * @param field          the field associated to the query parameter
	 * @param seq            true if this parameters is a sequence. See {@link Parameters.Parameter#isSequence()}
	 * @param config         the SQL config. Might be required to get the default column length.
	 * @throws YopIncoherentQueryException if the query string contains a number of '?' different from 1
	 */
	public SQLExpression(
		String sql,
		String parameterName,
		Object parameterValue,
		Field field,
		boolean seq,
		Config config) {
		this(sql, new Parameters().addParameter(parameterName, parameterValue, field, seq, config));
	}

	/**
	 * Default constructor with an sql string and parameters.
	 * @param sql        the sql query.
	 * @param parameters the query parameters
	 * @throws YopIncoherentQueryException if the query string contains a number of '?' different from parameters size
	 */
	public SQLExpression(String sql, List<Parameters.Parameter> parameters) {
		this();
		this.sql = sql == null ? "" : sql;
		this.parameters.addAll(parameters);

		if (! this.isCoherent()) {
			throw new YopIncoherentQueryException(this);
		}
	}

	/**
	 * Return this SQL query part parameters.
	 * @return a copy of {@link #parameters}
	 */
	public Parameters getParameters() {
		Parameters out = new Parameters();
		out.addAll(this.parameters);
		return out;
	}

	@Override
	public String toString() {
		return this.sql == null ? "" : this.sql;
	}

	@Override
	public int length() {
		return this.sql == null ? 0 : this.sql.length();
	}

	@Override
	public char charAt(int index) {
		return this.sql.charAt(index);
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * This returns a new SQL part for the sub-sequence, preserving the parameters integrity.
	 */
	@Override
	public SQLExpression subSequence(int start, int end) {
		String removeStart = (String) this.sql.subSequence(0, start);
		String keep        = (String) this.sql.subSequence(start, end);

		int parameterStart = StringUtils.countMatches(removeStart, PARAM);
		int parameterKeep  = StringUtils.countMatches(keep, PARAM) + parameterStart;
		Parameters parameters = new Parameters();

		int i = 0;
		for (Parameters.Parameter parameter : this.parameters) {
			if ((i >= parameterStart && i < parameterKeep)) {
				parameters.add(parameter);
			}
			i++;
		}
		return new SQLExpression(keep, parameters);
	}

	/**
	 * Appends another query part to the current SQL part.
	 * @param part the part to join
	 * @return the current instance
	 * @throws YopIncoherentQueryException if the the parameter is a String with at least one '?'.
	 */
	public SQLExpression append(CharSequence part) {
		if (part instanceof String && ((String) part).contains(PARAM)) {
			throw new YopRuntimeException("Incoherent SQL : you cannot join an SQL part with '?' and no parameter.");
		}
		this.sql = MessageUtil.join(" ", this.sql, part.toString());
		if (part instanceof SQLExpression) {
			this.parameters.addAll(((SQLExpression) part).parameters);
		}
		return this;
	}

	/**
	 * Check if the number of '?' occurrences in {@link #sql} is coherent with {@link #parameters}.
	 * @return true if there are as many '?' in {@link #sql} as parameters in {@link #parameters}
	 */
	public boolean isCoherent() {
		return StringUtils.countMatches(this.sql, PARAM) == this.parameters.size();
	}

	/**
	 * Create an SQL part instance that is actually a '?' parameter.
	 * @param name  the parameter name
	 * @param value the parameter value
	 * @param field the field associated to the query parameter
	 * @return a new SQLExpression instance, whose sql is '?' and with a parameter created for the given value.
	 */
	public static SQLExpression parameter(String name, Object value, Field field, Config config) {
		return new SQLExpression(PARAM, name, value, field, false, config);
	}

	/**
	 * Create an SQL part instance that is actually a '?' parameter, for a delayed value.
	 * @param name         the parameter name
	 * @param delayedValue the parameter delayed value
	 * @return a new SQLExpression instance, whose sql is '?' and with a parameter created for the delayed value.
	 */
	public static SQLExpression parameter(String name, Parameters.DelayedValue delayedValue) {
		return new SQLExpression(PARAM, new Parameters().addParameter(name, delayedValue));
	}

	/**
	 * Create a new SQLExpression instance for the given parts, separated with a separator.
	 * @param separator the separator
	 * @param parts     the SQL parts to join
	 * @return a new SQLExpression instance
	 */
	public static SQLExpression join(String separator, CharSequence... parts) {
		return join(separator, Arrays.asList(parts));
	}

	/**
	 * Create a new SQLExpression instance for the given parts, separated with a separator.
	 * @param separator the separator
	 * @param parts     the SQL parts to join
	 * @return a new SQLExpression instance
	 */
	public static SQLExpression join(String separator, Collection<? extends CharSequence> parts) {
		List<String> sql = new ArrayList<>();
		List<Parameters.Parameter> parameters = new ArrayList<>();
		for (CharSequence part : parts) {
			sql.add(part.toString());
			if (part instanceof SQLExpression) {
				parameters.addAll(((SQLExpression) part).parameters);
			}
		}
		return new SQLExpression(MessageUtil.join(separator, sql), parameters);
	}

	/**
	 * Create a new SQLExpression instance for a String pattern, whose parameters are replaced with the given parts.
	 * <br><br>
	 * <b>
	 * Parameters in the pattern must be in the same order as the replacement parts !
	 * <br>
	 * The parameter name in {@link #REPLACEMENT_PATTERN} is just a hint for the reader !
	 * </b>
	 * @param pattern the SQL pattern. Parameters are delimited using {@link #REPLACEMENT_PATTERN}. e.g. {:param_name}
	 * @param parts   the SQL parts that will replace the pattern parameters. <b>Order is important !</b>
	 * @return a new SQLExpression instance for the pattern.
	 */
	public static SQLExpression forPattern(String pattern, CharSequence... parts) {
		Matcher matcher = REPLACEMENT_PATTERN.matcher(pattern);
		StringBuilder buffer = new StringBuilder();
		List<Parameters.Parameter> parameters = new ArrayList<>();
		int i = 0;
		int pos = 0;
		while (matcher.find()) {
			if (i >= parts.length) {
				throw new YopRuntimeException(
					"Invalid pattern replacement in [" + pattern + "]. "
					+ "No part for match index #[" + i + "] : "
					+ "[" + matcher.group(0) + "]"
				);
			}
			buffer.append(pattern, pos, matcher.start());
			buffer.append(parts[i].toString());
			if (parts[i] instanceof SQLExpression) {
				parameters.addAll(((SQLExpression) parts[i]).parameters);
			}
			pos = matcher.end();
			i++;
		}

		if (i != parts.length) {
			throw new YopRuntimeException(
				"Invalid pattern replacement in [" + pattern + "]. Some parts left for match index #[" + i + "]"
			);
		}

		buffer.append(pattern, pos, pattern.length());
		return new SQLExpression(buffer.toString(), parameters);
	}
}
