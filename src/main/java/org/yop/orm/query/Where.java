package org.yop.orm.query;

import com.google.common.base.Joiner;
import org.yop.orm.evaluation.Comparaison;
import org.yop.orm.evaluation.Evaluation;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.evaluation.Or;
import org.yop.orm.model.Yopable;

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
	 * Add comparisons to the where clause using an AND operator and an OR operator between them.
	 * @param in the comparisons to add
	 * @return the current SELECT, for chaining purposes
	 */
	public final Where<T> or(Comparaison... in) {
		List<Evaluation> comparaisons = Arrays.asList(in);
		return this.matches(new Or(comparaisons));
	}

	/**
	 * Add comparisons to the where clause using an AND operator and an AND operator between them.
	 * @param in the comparisons to add
	 * @return the current SELECT, for chaining purposes
	 */
	public final Where<T> and(Comparaison... in) {
		Arrays.asList(in).forEach(this::matches);
		return this;
	}

	/**
	 * Add evaluations to the where clause using an AND operator and an OR operator between them.
	 * @param evaluations the evaluations to add
	 * @return the current SELECT, for chaining purposes
	 */
	public Where<T> or(Evaluation... evaluations) {
		return this.matches(new Or(evaluations));
	}

	/**
	 * Create the current WHERE clause SQL.
	 * @return the SQL WHERE clause
	 */
	public String toSQL(Context<T> context) {
		String sql = "";
		sql += Joiner.on(" AND ").join(evaluations.stream().map(e -> e.toSQL(context)).collect(Collectors.toList()));
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
	public static <T extends Yopable> Comparaison compare(Function<T, ?> getter, Operator op, Comparable ref) {
		return new Comparaison(getter, op, ref);
	}

	/**
	 * Create an 'IS NULL' clause for a given getter
	 * @param getter the getter
	 * @param <T> the current type, holding the getter
	 * @return a Compare object that can be added to the where clause
	 */
	public static <T extends Yopable> Comparaison isNull(Function<T, Comparable<?>> getter) {
		return new Comparaison(getter, Operator.IS_NULL, null);
	}

	/**
	 * Create an 'IS NOT NULL' clause for a given getter
	 * @param getter the getter
	 * @param <T> the current type, holding the getter
	 * @return a Compare object that can be added to the where clause
	 */
	public static <T extends Yopable> Comparaison isNotNull(Function<T, Comparable<?>> getter) {
		return new Comparaison(getter, Operator.IS_NOT_NULL, null);
	}
}
