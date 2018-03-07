package org.yop.orm.query;

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.yop.orm.evaluation.*;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.MessageUtil;
import org.yop.orm.util.ORMUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SQL WHERE clause for the given target type.
 * @param <T> the target type
 */
public class Where<T extends Yopable>  {

	/** The where clause evaluations. Joined with AND. Use {@link Or} to create an OR evaluation. */
	private Collection<Evaluation> evaluations = new ArrayList<>();

	/**
	 * Default constructor.
	 * Should be instantiated from the {@link org.yop.orm.query} package only !
	 * <br>
	 * Use the {@link Select#where()} or {@link Join#where()} to get the {@link Where} instance you need !
	 */
	Where() {}

	/**
	 * Add an evaluation to the where clause, using the AND operator
	 * @param evaluation the evaluation to add
	 * @return the current WHERE clause, for chaining purposes
	 */
	public Where<T> matches(Evaluation evaluation) {
		this.evaluations.add(evaluation);
		return this;
	}

	/**
	 * Add a natural key evaluation to the where clause, using the AND operator
	 * @param reference a target class instance, whose natural ID will be used as reference
	 * @return the current WHERE clause, for chaining purposes
	 */
	public Where<T> naturalID(T reference) {
		this.evaluations.add(new NaturalKey<>(reference));
		return this;
	}

	/**
	 * Add comparisons to the where clause using an AND operator and an OR operator between them.
	 * @param in the comparisons to add
	 * @return the current WHERE clause, for chaining purposes
	 */
	public final Where<T> or(Comparison... in) {
		List<Evaluation> comparaisons = Arrays.asList(in);
		return this.matches(new Or(comparaisons));
	}

	/**
	 * Add comparisons to the where clause using an AND operator and an AND operator between them.
	 * @param in the comparisons to add
	 * @return the current WHERE, for chaining purposes
	 */
	public final Where<T> and(Evaluation... in) {
		Arrays.asList(in).forEach(this::matches);
		return this;
	}

	/**
	 * Add evaluations to the where clause using an AND operator and an OR operator between them.
	 * @param evaluations the evaluations to add
	 * @return the current WHERE, for chaining purposes
	 */
	public Where<T> or(Evaluation... evaluations) {
		return this.matches(new Or(evaluations));
	}

	/**
	 * Create the current WHERE clause SQL.
	 * @param context    the context from which the query clause must be built
	 * @param parameters the query parameters that will be updated with this where clause paramters
	 * @return the SQL WHERE clause
	 */
	public String toSQL(Context<T> context, Parameters parameters) {
		String sql = "";
		sql += Joiner.on(" AND ").join(
			evaluations.stream().map(e -> e.toSQL(context, parameters)).collect(Collectors.toList())
		);
		return sql;
	}

	/**
	 * Create a comparison for a given getter
	 * @param getter the getter
	 * @param op     the operator to use
	 * @param ref    the reference
	 * @param <T> the current type, holding the getter
	 * @return a Compare object that can be added to the where clause
	 */
	public static <T extends Yopable> Comparison compare(Function<T, ?> getter, Operator op, Comparable ref) {
		return new Comparison(getter, op, ref);
	}

	/**
	 * Create an 'IS NULL' clause for a given getter
	 * @param getter the getter
	 * @param <T> the current type, holding the getter
	 * @return a Compare object that can be added to the where clause
	 */
	public static <T extends Yopable> Comparison isNull(Function<T, Comparable<?>> getter) {
		return new Comparison(getter, Operator.IS_NULL, null);
	}

	/**
	 * Create an 'IS NOT NULL' clause for a given getter
	 * @param getter the getter
	 * @param <T> the current type, holding the getter
	 * @return a Compare object that can be added to the where clause
	 */
	public static <T extends Yopable> Comparison isNotNull(Function<T, Comparable<?>> getter) {
		return new Comparison(getter, Operator.IS_NOT_NULL, null);
	}

	/**
	 * Create a "NaturalId" evaluation, against a target type instance as reference.
	 * @param reference a target class instance, whose natural ID will be used as reference
	 * @param <T> the current type
	 * @return a NaturalId Evaluation object that can be added to the where clause
	 */
	public static <T extends Yopable> Evaluation naturalId(T reference) {
		return new NaturalKey<>(reference);
	}

	/**
	 * See : {@link #id(Collection)}.
	 */
	public static Evaluation id(Long... ids) {
		return id(Arrays.asList(ids));
	}

	/**
	 * Create an "ID" evaluation, against a given ID value.
	 * @param ids the expected IDs for the target type.
	 * @return an 'ID IN (?)' Evaluation object that can be added to the where clause
	 */
	public static Evaluation id(Collection<Long> ids) {
		return new Evaluation() {
			@Override
			public <Y extends Yopable> String toSQL(Context<Y> context, Parameters parameters) {
				String idColumn = context.getPath() + "." + ORMUtil.getIdColumn(context.getTarget());
				String inClause = idColumn + " IN (";
				for (Long id : ids) {
					parameters.addParameter(idColumn + "=" + id, id);
					inClause += "?,";
				}
				inClause = StringUtils.removeEnd(inClause, ",");
				inClause += ")";
				return inClause;
			}
		};
	}

	/**
	 * Simply join some where clauses using " AND ". Clauses can be null or empty.
	 * @param whereClauses the where clauses to join
	 * @return the new where clause
	 */
	public static String toSQL(String... whereClauses) {
		return toSQL(Arrays.asList(whereClauses));
	}


	/**
	 * Simply join some where clauses using " AND ". Clauses can be null or empty.
	 * @param whereClauses the where clauses to join
	 * @return the new where clause
	 */
	public static String toSQL(Collection<String> whereClauses) {
		return MessageUtil.join(" AND ", whereClauses);
	}
}
