package org.yop.orm.sql;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.sql.adapter.IRequest;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.TransformUtil;

import java.util.*;

/**
 * An SQL query, and everything it needs to be executed on a Connection.
 * <br>
 * It also has a generated ID set, that should be filled by {@link IRequest#executeUpdate()}.
 * <br><br>
 * A query can either be :
 * <ul>
 *     <li>Simple : see {@link SimpleQuery}</li>
 *     <li>Batch : see {@link BatchQuery}</li>
 * </ul>
 * Query generation in Yop is very rough : SQL portions are generated from different classes and concatenated.
 * <br>
 * In this class we try to take care of the SQL query so it can be safely executed. Here is what we do :
 * <ul>
 *     <li>Split the query into words</li>
 *     <li>Identify aliases whose length is {@literal >} {@link Config#aliasMaxLength()}</li>
 *     <li>Shorten these aliases using {@link ORMUtil#uniqueShortened(String, Config)}</li>
 *     <li>Keep track of the alias → shorten alias conversions</li>
 *     <li>Keep track of the original query so it can be logged when required</li>
 * </ul>
 * This is highly questionable and query generation should use an SQL grammar tool.
 */
public abstract class Query {

	private static final Logger logger = LoggerFactory.getLogger(Query.class);

	/** Query type enum, with an 'unknown' value */
	public enum Type {
		CREATE, DROP, SELECT, INSERT, UPDATE, DELETE, UNKNOWN;

		/**
		 * Guess the query type from the SQL.
		 * @param sql the SQL request
		 * @return the query type. {@link #UNKNOWN} if no match.
		 */
		public static Type guess(String sql) {
			String guess = StringUtils.trim(StringUtils.substringBefore(StringUtils.trim(sql), " ")).toUpperCase();
			try {
				return valueOf(guess);
			} catch (RuntimeException e) {
				logger.debug("No query type match for [{}]. Guess was [{}]", sql, guess);
				return UNKNOWN;
			}
		}
	}

	/** The regex used to split the query into words. Yes, it is dubious. */
	private static final String SQL_WORD_SPLIT_PATTERN = " ,;\"";

	private static final Comparator<String> ALIAS_COMPARATOR = Comparator
		.comparing(String::length)
		.thenComparing(String::compareTo)
		.reversed();

	/** The query type. Needed to know if a query is batchable, for instance. */
	private final Type type;

	/** The SQL to execute */
	protected final String sql;

	/** SQL execution config */
	protected final Config config;

	/** The SQL with too long aliases replaced with generated UUIDs */
	private String safeAliasSQL;

	/** The generated IDs */
	final List<Comparable> generatedIds = new ArrayList<>();

	/** Aliases map : short alias → original alias */
	final Map<String, String> tooLongAliases = new HashMap<>();

	/** A reference to the root target Yopable that this query was generated for. Only required for generated keys. */
	protected Class target;

	/**
	 * The source elements of the query.
	 * If the query is {@link Type#INSERT}, and {@link #askGeneratedKeys} is set to true, their IDs will be set back.
	 */
	protected final List<Object> elements = new ArrayList<>();

	/**
	 * Default constructor : SQL query.
	 * <br>
	 * In the SQL query, aliases whose length is {@literal >} {@link Config#aliasMaxLength()} will be
	 * replaced with {@link ORMUtil#uniqueShortened(String, Config)}.
	 * @param sql    the SQL query to execute
	 * @param type   the query type
	 * @param config the SQL config (sql separator, use batch inserts...)
	 */
	public Query(String sql, Type type, Config config) {
		this.sql = sql;
		this.safeAliasSQL = sql;
		this.type = type;
		this.config = config;

		// Search table/column aliases that are too long for SQL : longest alias first !
		Set<String> tooLongAliases = new TreeSet<>(ALIAS_COMPARATOR);
		for (String word : StringUtils.split(sql, SQL_WORD_SPLIT_PATTERN)) {
			// if the word is not too long, that's OK
			// if the word contains a "." this is not an alias
			if(word.length() <= this.config.aliasMaxLength() || word.contains(Config.DOT)) {
				continue;
			}
			tooLongAliases.add(
				StringUtils.removeEnd(StringUtils.removeStart(word.trim(), "\""), "\"")
			);
		}

		for (String tooLongAlias : tooLongAliases) {
			String shortened = ORMUtil.uniqueShortened(tooLongAlias, config);
			this.tooLongAliases.put(tooLongAlias, shortened);
			this.safeAliasSQL = StringUtils.replace(this.safeAliasSQL, tooLongAlias, shortened);
		}
	}

	public Class getTarget() {
		return this.target;
	}

	/**
	 * @return the original SQL query
	 */
	public String getSql() {
		return this.sql;
	}

	/**
	 * @return the query with long aliases replaced with UUIDs.
	 */
	public String getSafeSql() {
		return this.safeAliasSQL;
	}

	/**
	 * Get the elements this query applies to.
	 * <br>
	 * This is useful when doing INSERT queries whose generated IDs must be set back to the Java objects.
	 * @return the {@link #elements} of this query
	 */
	public List<Object> getElements() {
		return this.elements;
	}

	/**
	 * Get the query type.
	 * @return {@link #type}
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * Get the SQL config for this query
	 * @return the SQL {@link #config}
	 */
	public Config getConfig() {
		return this.config;
	}

	/**
	 * Get the original alias for a shortened one. Return the given parameter if no entry.
	 * @param shortened the shortened value of the alias
	 * @return the original alias value, or the input value if nothing found.
	 */
	public String getAlias(String shortened) {
		return this.tooLongAliases.entrySet()
			.stream()
			.filter(e -> StringUtils.equals(shortened, e.getValue()))
			.map(Map.Entry::getKey)
			.findFirst()
			.orElse(shortened);
	}

	/**
	 * Get the shortened version of an alias. Return the given parameter if no entry.
	 * @param alias the (not shortened) alias
	 * @return the shortened value for this alias, or the input alias value if nothing found.
	 */
	public String getShortened(String alias) {
		return this.tooLongAliases.getOrDefault(alias, alias);
	}

	/**
	 * Ask for generated IDs on behalf of the target class.
	 * <br>
	 * This will be used to find the Id to associate to generated keys.
	 * <br>
	 * This is only applicable if {@link #type} is {@link Type#INSERT}.
	 * @param target the target class for which there will be generated keys. If null, generated keys will not be read.
	 * @return the current query, for chaining purposes
	 */
	public Query askGeneratedKeys(Class target) {
		this.target = target;
		return this;
	}

	/**
	 * Should generated keys be read and assigned to target {@link #elements} ?
	 * @return true if {@link #target} is not null and {@link #type} is {@link Type#INSERT}.
	 */
	public boolean askGeneratedKeys() {
		return this.target != null && this.type == Type.INSERT;
	}

	/**
	 * Get the auto-generated ID column of the {@link #target}.
	 * @return an array of string that contains the target column ID at index 0, or an empty array if target is null.
	 */
	public String[] getAutogenIdColumn() {
		return this.target == null || ! ORMUtil.isAutogenId(this.target)
			? new String[0]
			: new String[] {ORMUtil.getIdColumn(this.target)};
	}

	/**
	 * @return the Ids generated when executing this query
	 */
	public List<Comparable> getGeneratedIds() {
		return this.generatedIds;
	}

	/**
	 * Read the {@link #generatedIds} and affect them to the {@link #elements}.
	 * @throws YopRuntimeException if the size of the generated IDs is not the same as the source elements'.
	 */
	public void pushGeneratedIds() {
		if (! this.generatedIds.isEmpty() && !this.elements.isEmpty()) {
			if(this.generatedIds.size() != this.elements.size()) {
				throw new YopRuntimeException(
					"Generated IDs length [" + this.generatedIds.size() + "] "
					+ "is different from source elements length [" + this.elements.size() + "] ! "
					+ "Maybe your JDBC driver does not support batch inserts :-( "
					+ "Query was [" + this + "]"
				);
			}
			for (int i = 0; i < this.generatedIds.size(); i++) {
				Object element = this.elements.get(i);
				Class target = element.getClass();
				if (ORMUtil.isAutogenId(target)) {
					ORMUtil.setId(readGeneratedId(this.generatedIds.get(i), target), element);
				}
			}
		}
	}

	/**
	 * Read and transform the generated ID so it can be affected to the target type.
	 * @param input  the generated ID
	 * @param target the target object
	 * @return the ID that can be affected to the target element
	 */
	private static Comparable readGeneratedId(Comparable input, Class target) {
		return (Comparable) TransformUtil.transform(input, ORMUtil.getIdField(target).getType());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || this.getClass() != o.getClass()) return false;
		Query that = (Query) o;

		return Objects.equals(this.elements, that.elements)
			&& Objects.equals(this.sql, that.sql)
			&& Objects.equals(this.target, that.target)
			&& Objects.equals(this.parametersToString(), that.parametersToString());
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.elements, this.sql, this.target, this.parametersToString());
	}


	/**
	 * Move the parameters cursor to the next batch of parameters.
	 * @return true if there is a next batch available after moving the cursor
	 */
	public abstract boolean nextBatch();

	/**
	 * Get the current batch of parameters.
	 * @return the current batch of parameters.
	 * @throws ArrayIndexOutOfBoundsException if the batch cursor is not correctly set
	 */
	public abstract Parameters getParameters();

	/**
	 * Get a String representation of the parameters.
	 * <br>
	 * This method should never throw an {@link ArrayIndexOutOfBoundsException} !
	 * @return a String representation of the parameters, either batch or simple.
	 */
	public abstract String parametersToString();
}
