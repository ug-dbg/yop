package org.yop.orm.query;

import org.yop.orm.evaluation.*;
import org.yop.orm.model.Yopable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

/**
 * An SQL request with a {@link Where} clause.
 * @param <Request> the request type (e.g. Select, Delete)
 * @param <T> the request target type
 */
abstract class WhereRequest<Request extends WhereRequest, T extends Yopable> extends SQLRequest<Request, T> {

	/** Where clauses */
	protected Where<T> where = new Where<>();

	/**
	 * Default constructor : final field {@link #context} must be initialized.
	 * @param context the context of the request
	 */
	WhereRequest(Context<T> context) {
		super(context);
	}

	/**
	 * The where clause of this request.
	 * @return the Where clause
	 */
	public Where<T> where() {
		return this.where;
	}

	/**
	 * Add an evaluation to the where clause (AND operator).
	 * @param evaluation the evaluation
	 * @return the current request, for chaining purposes
	 */
	@SuppressWarnings("unchecked")
	public Request where(Evaluation evaluation) {
		this.where.and(evaluation);
		return (Request) this;
	}

	/**
	 * Add a {@link Comparison} to the where clause (AND operator).
	 * @param getter the field getter
	 * @param op     the comparison operator
	 * @param ref    the comparison reference value.
	 * @return the current request, for chaining purposes
	 */
	@SuppressWarnings("unchecked")
	public Request where(Function<T, ?> getter, Operator op, Comparable ref) {
		this.where.and(new Comparison(getter, op, ref));
		return (Request) this;
	}

	/**
	 * Create an "ID IN" evaluation, against the given ID values.
	 * @param ids the expected IDs for the target type.
	 * @return the current request, for chaining purposes
	 */
	@SuppressWarnings("unchecked")
	public Request whereId(Long... ids) {
		this.where.and(new IdIn(Arrays.asList(ids)));
		return (Request) this;
	}

	/**
	 * Create an "natural key" evaluation ({@link NaturalKey}, against the given reference.
	 * @param reference the target object reference
	 * @return the current request, for chaining purposes
	 */
	@SuppressWarnings("unchecked")
	public Request whereNaturalId(T reference) {
		this.where.and(new NaturalKey<>(reference));
		return (Request) this;
	}

	/**
	 * Add an {@link In} to the where clause (AND operator).
	 * @param getter the field getter
	 * @param values the restriction values
	 * @return the current request, for chaining purposes
	 */
	@SuppressWarnings("unchecked")
	public <U> Request where(Function<T, U> getter, Collection<U> values) {
		this.where.and(new In(getter, values));
		return (Request) this;
	}

	/**
	 * Add several comparisons to the where clause, with an OR operator between them.
	 * @param compare the comparisons
	 * @return the current request, for chaining purposes
	 */
	@SuppressWarnings("unchecked")
	public Request or(Comparison... compare) {
		this.where.or(compare);
		return (Request) this;
	}
}
