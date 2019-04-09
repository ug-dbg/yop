package org.yop.orm.query.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.evaluation.Explicit;
import org.yop.orm.model.JsonAble;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.SQLExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * A paging directive.
 * <br>
 * Paging is very elusive in SQL.
 * <ul>
 *     <li>SQLite and MySQL uses the OFFSET/LIMIT syntax</li>
 *     <li>some MySQL versions have OFFSET/LIMIT limitation in sub-queries</li>
 *     <li>MSSQL and Oracle have their own complicated syntax</li>
 *     <li>SQL:2008 introduced a standard syntax that might be available or not given the DBMS and its driver.</li>
 *     <li>It is always possible to page using 2 queries.</li>
 * </ul>
 */
public class Paging implements JsonAble {

	private static final Logger logger = LoggerFactory.getLogger(Paging.class);

	/**
	 * Paging method enum.
	 */
	public enum Method {
		/**
		 * SQLite and MySQL can use this syntax. Be careful, MySQL can have limitations with limits in sub-queries !
		 */
		LIMIT(" OFFSET ? ", " LIMIT ? "),

		/**
		 * The standard SQL:2008 paging method.
		 * Postgres and SQL Server seem to work quite well with this syntax.
		 */
		SQL_2008(" OFFSET ? ROWS ", " FETCH NEXT ? ROWS ONLY "),

		/**
		 * This is the Paging fallback.
		 * <br>
		 * Use 2 queries to achieve paging :
		 * <ul>
		 *     <li>Select the ids that match</li>
		 *     <li>Filter these IDs</li>
		 *     <li>Select the data for the filtered IDs</li>
		 * </ul>
		 */
		TWO_QUERIES("", "");

		private String offsetFormat;
		private String limitFormat;

		Method(String offsetFormat, String limitFormat) {
			this.offsetFormat = offsetFormat;
			this.limitFormat = limitFormat;
		}

		public String limit() {
			return this.limitFormat;
		}

		public String offset() {
			return this.offsetFormat;
		}

		/**
		 * Get the paging method for the given name. Do not throw any exception.
		 * @param name the paging method name
		 * @return the Paging method with the given name. Returns {@link Method#TWO_QUERIES} if no match.
		 */
		public static Method byName(String name) {
			try {
				return Method.valueOf(name);
			} catch (RuntimeException e) {
				logger.warn("No paging method named [{}]. Returning [{}]", name, TWO_QUERIES.name());
				return TWO_QUERIES;
			}
		}
	}

	/** From which offset the paging should start. If null → start from first offset. */
	private Long offset;

	/** The number of rows to filter. If null → no limit. */
	private Long limit;

	/** Default constructor. Required for JSON deserialization. */
	private Paging() {}

	/**
	 * Default constructor. Provide with paging offset and limit.
	 * @param offset the paging offset, see {@link #offset}.
	 * @param limit  the paging limit, see {@link #limit}.
	 */
	Paging(Long offset, Long limit) {
		this();
		this.offset = offset;
		this.limit = limit;
	}

	/**
	 * Is this paging object actually paging ?
	 * @return true if either {@link #limit} or {@link #offset} is not null.
	 */
	boolean isPaging() {
		return this.offset != null || this.limit != null;
	}

	/**
	 * Filter a list of ids. This is what is used when the paging method is set to {@link Method#TWO_QUERIES}.
	 * @param ids the ids to filter
	 * @return a new List of ids, a sublist of the ids parameters for the current paging offset and limit.
	 */
	List<Comparable> pageIds(List<Comparable> ids) {
		int from = this.offset == null ? 0 : this.offset.intValue();
		int to = this.limit == null ? (ids.size()) : Math.min(ids.size(), from + this.limit.intValue());
		return from > to ? new ArrayList<>(0) : ids.subList(from, to);
	}

	/**
	 * Paging might require a default order (SQL does not ensure a stable unique order) when fetching ids).
	 * <br>
	 * This returns an 'order by id' for the target class <b>only if</b> {@link #isPaging()} is true.
	 * @param context the current context
	 * @param config  the SQL configuration to use
	 * @param <T> the context target type
	 * @return an ASC {@link OrderBy#orderById(boolean)} or an empty String
	 */
	public <T extends Yopable> String toSQLOrderBy(Context<T> context, Config config) {
		return this.isPaging() ? OrderBy.<T>orderById(true).toSQL(context.getTarget(), config) : "";
	}

	/**
	 * Create the SQL portion for the current paging directive.
	 * <br>
	 * This reads the config {@link Config#getPagingMethod()}.
	 * @param context    the query context
	 * @param config     the connection configuration
	 * @return a SQL limit restriction.
	 *         An empty string if {@link Config#getPagingMethod()} is {@link Method#TWO_QUERIES}.
	 */
	public CharSequence toSQL(Context<?> context, Config config) {
		if (! this.isPaging()) {
			return "";
		}

		if (config.getPagingMethod() == Method.LIMIT) {
			return SQLExpression.join(" ", this.toSQLLimit(context, config), this.toSQLOffset(context,config));
		}
		if (config.getPagingMethod() == Method.SQL_2008) {
			return SQLExpression.join(" ", this.toSQLOffset(context, config), this.toSQLLimit(context, config));
		}
		if (config.getPagingMethod() == Method.TWO_QUERIES) {
			logger.info("Paging method uses 2 queries. Paging will be done by filtering fetched ids.");
		}
		return "";
	}

	private CharSequence toSQLLimit(Context<?> context, Config config) {
		return this.limit == null ? "" : new Explicit(config.getPagingMethod().limit())
			.setParameter("limit",  this.limit)
			.toSQL(context, config);
	}

	private CharSequence toSQLOffset(Context<?> context, Config config) {
		return this.offset == null ? "" : new Explicit(config.getPagingMethod().offset())
			.setParameter("offset",  this.offset)
			.toSQL(context, config);
	}
}
