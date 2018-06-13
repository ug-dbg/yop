package org.yop.orm.sql;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IRequest;
import org.yop.orm.util.ORMUtil;

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
 *     <li>Identify aliases whose length is {@literal >} {@link Constants#SQL_ALIAS_MAX_LENGTH}</li>
 *     <li>Shorten these aliases using {@link ORMUtil#uniqueShortened(String)}</li>
 *     <li>Keep track of the alias → shorten alias conversions</li>
 *     <li>Keep track of the original query so it can be logged when required</li>
 * </ul>
 * This is highly questionable and query generation should use an SQL grammar tool.
 */
public abstract class Query {

	/** Query type enum, with an 'unknown' value */
	public enum Type {CREATE, DROP, SELECT, INSERT, UPDATE, DELETE, UNKNOWN}

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

	/** The SQL with too long aliases replaced with generated UUIDs */
	private String safeAliasSQL;

	/** True to ask the statement to return the generated Ids */
	boolean askGeneratedKeys = false;

	/** The generated IDs */
	final List<Long> generatedIds = new ArrayList<>();

	/** Aliases map : short alias → original alias */
	final Map<String, String> tooLongAliases = new HashMap<>();

	/** A reference to the root target Yopable that this query was generated for. Only required for generated keys. */
	protected Class<? extends Yopable> target;

	/**
	 * The source elements of the query.
	 * If the query is {@link Type#INSERT}, and {@link #askGeneratedKeys} is set to true, their IDs will be set back.
	 */
	protected final List<Yopable> elements = new ArrayList<>();

	/**
	 * Default constructor : SQL query.
	 * <br>
	 * In the SQL query, aliases whose length is {@literal >} {@link Constants#SQL_ALIAS_MAX_LENGTH} will be
	 * replaced with {@link ORMUtil#uniqueShortened(String)}.
	 * @param sql        the SQL query to execute
	 */
	public Query(String sql, Type type) {
		this.sql = sql;
		this.safeAliasSQL = sql;
		this.type = type;

		// Search table/column aliases that are too long for SQL : longest alias first !
		Set<String> tooLongAliases = new TreeSet<>(ALIAS_COMPARATOR);
		for (String word : StringUtils.split(sql, SQL_WORD_SPLIT_PATTERN)) {
			// if the word is not too long, that's OK
			// if the word contains a "." this is not an alias
			if(word.length() <= Constants.SQL_ALIAS_MAX_LENGTH || word.contains(Constants.DOT)) {
				continue;
			}
			tooLongAliases.add(
				StringUtils.removeEnd(StringUtils.removeStart(word.trim(), "\""), "\"")
			);
		}

		for (String tooLongAlias : tooLongAliases) {
			String shortened = ORMUtil.uniqueShortened(tooLongAlias);
			this.tooLongAliases.put(tooLongAlias, shortened);
			this.safeAliasSQL = StringUtils.replace(this.safeAliasSQL, tooLongAlias, shortened);
		}
	}

	public Class<? extends Yopable> getTarget() {
		return target;
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
	public List<Yopable> getElements() {
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
	 * Ask for generated IDs or not.
	 * @param value  true to ask the statement to return the generated IDs
	 * @param target the target class for which there will be generated keys
	 * @return the current query, for chaining purposes
	 */
	public Query askGeneratedKeys(boolean value, Class<? extends Yopable> target) {
		this.askGeneratedKeys = value;
		this.target = target;
		return this;
	}

	public boolean askGeneratedKeys() {
		return this.askGeneratedKeys;
	}

	/**
	 * Get the ID column of this query's {@link #target}.
	 * @return an array of string that contains the target column ID at index 0, or an empty array if target is null.
	 */
	public String[] getIdColumn() {
		return this.target == null ? new String[0] : new String[] {ORMUtil.getIdColumn(this.target)};
	}

	/**
	 * @return the Ids generated when executing this query
	 */
	public List<Long> getGeneratedIds() {
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
				this.elements.get(i).setId(this.generatedIds.get(i));
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
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
