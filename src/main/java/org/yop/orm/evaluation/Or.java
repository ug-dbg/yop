package org.yop.orm.evaluation;

import com.google.common.base.Joiner;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Or evaluation simply joins other evaluations with an " OR " keyword.
 */
public class Or implements Evaluation {
	/** The evaluations to join */
	private List<Evaluation> evaluations = new ArrayList<>();

	/**
	 * Default constructor with explicit vararg evaluations feeding
	 * @param evaluations the evaluations to join
	 */
	public Or(Evaluation... evaluations) {
		this.evaluations.addAll(Arrays.asList(evaluations));
	}

	/**
	 * Default constructor with evaluations to join as a collection
	 * @param evaluations the evaluations to join
	 */
	public Or(Collection<Evaluation> evaluations) {
		this.evaluations.addAll(evaluations);
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * Simply joins the {@link #evaluations} {@link Evaluation#toSQL(Context, Parameters)} with an " OR " keyword.
	 * <br>
	 * If {@link #evaluations} is not empty, the output SQL is wrapped into parentheses.
	 */
	@Override
	public <T extends Yopable> String toSQL(Context<T> context, Parameters parameters) {
		if(this.evaluations.isEmpty()) {
			return "";
		}
		return "("
			+ Joiner.on(" OR ").join(
				this.evaluations.stream().map(e -> e.toSQL(context, parameters)).collect(Collectors.toList())
			)
		+ ")";
	}
}
