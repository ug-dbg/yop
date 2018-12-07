package org.yop.orm.evaluation;

import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.SQLPart;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Or evaluation simply joins other evaluations with an " OR " keyword.
 */
public class Or implements Evaluation {
	/** The evaluations to join */
	private final Evaluations evaluations = new Evaluations();

	private Or() {}

	/**
	 * Default constructor with explicit vararg evaluations feeding
	 * @param evaluations the evaluations to join
	 */
	public Or(Evaluation... evaluations) {
		this();
		this.evaluations.addAll(Arrays.asList(evaluations));
	}

	/**
	 * Default constructor with evaluations to join as a collection
	 * @param evaluations the evaluations to join
	 */
	public Or(Collection<Evaluation> evaluations) {
		this();
		this.evaluations.addAll(evaluations);
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * Simply joins the {@link #evaluations} {@link Evaluation#toSQL(Context, Config)} with an " OR " keyword.
	 * <br>
	 * If {@link #evaluations} is not empty, the output SQL is wrapped into parentheses.
	 */
	@Override
	public <T extends Yopable> CharSequence toSQL(Context<T> context, Config config) {
		if(this.evaluations.isEmpty()) {
			return "";
		}
		List<CharSequence> evaluations = this.evaluations
			.stream()
			.map(e -> e.toSQL(context, config))
			.collect(Collectors.toList());
		return new SQLPart("(").append(SQLPart.join(" OR ", evaluations)).append(")");
	}
}
