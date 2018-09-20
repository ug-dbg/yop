package org.yop.orm.query;

import com.google.common.base.Joiner;
import org.yop.orm.evaluation.*;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.MessageUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SQL WHERE clause for the given target type.
 * <br><br>
 * A where clause is a collection of {@link Evaluation} that are joined with the 'AND' SQL keyword.
 * <br>
 * An {@link Or} evaluation - whose own evaluations will be joined with the 'OR' SQL keyword -
 * can be added into {@link #evaluations}.
 * <br><br>
 * You can do standard comparisons (see {@link Comparison}) between a field (using a method reference)
 * and a reference value.
 * <br>
 * You can do an Evaluation given a natural ID : {@link #naturalID(Yopable)}.
 * <br>
 * You can also do standard comparisons between a field and an other target field anywhere in the data graph :
 * see {@link Path}. Example :
 * <pre>
 * {@code
 * Path<Pojo, String> jopoName = Path.pathSet(Pojo::getJopos).to(Jopo::getName);
 * Set<Pojo> matches = Select
 *  .from(Pojo.class)
 *  .join(JoinSet.to(Pojo::getJopos))
 *  .join(JoinSet.to(Pojo::getOthers).where(Where.compare(Other::getName, Operator.EQ, jopoName)))
 *  .execute(connection);
 * }
 * </pre>
 *
 * @param <T> the target type
 */
public class Where<T extends Yopable>  {

	/** The where clause evaluations. Joined with AND. Use {@link Or} to create an OR evaluation. */
	private final Collection<Evaluation> evaluations = new ArrayList<>();

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
		List<Evaluation> comparisons = Arrays.asList(in);
		return this.matches(new Or(comparisons));
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
	 * @param parameters the query parameters that will be updated with this where clause parameters
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
	 * Create an "ID" evaluation, against the given ID values.
	 * @param ids the expected IDs for the target type.
	 * @return an 'ID IN (?)' Evaluation object that can be added to the where clause
	 */
	public static Evaluation id(Long... ids) {
		return id(Arrays.asList(ids));
	}

	/**
	 * Create an "ID" evaluation, against ths given ID values.
	 * @param ids the expected IDs for the target type.
	 * @return an 'ID IN (?)' Evaluation object that can be added to the where clause
	 */
	public static Evaluation id(Collection<Long> ids) {
		return new IdIn(ids);
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