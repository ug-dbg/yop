package org.yop.orm.evaluation;

import com.google.common.base.Joiner;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Or implements Evaluation {
	private List<Evaluation> evaluations = new ArrayList<>();

	public Or(Evaluation... evaluations) {
		this.evaluations.addAll(Arrays.asList(evaluations));
	}

	public Or(Collection<Evaluation> evaluations) {
		this.evaluations.addAll(evaluations);
	}

	public <T extends Yopable> String toSQL(Context<T> context) {
		return "("
			+ Joiner.on(" OR ").join(this.evaluations.stream().map(e -> e.toSQL(context)).collect(Collectors.toList()))
		+ ")";
	}
}
