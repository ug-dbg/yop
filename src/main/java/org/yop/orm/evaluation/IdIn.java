package org.yop.orm.evaluation;

import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.ORMUtil;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * ID restriction on several values. Can be easier to write than OR...
 */
public class IdIn  implements Evaluation {

	private static final String IN = " {0} IN ({1}) ";

	/** ID value restriction */
	private Collection<Long> values = new HashSet<>();

	/**
	 * Default constructor : gimme all the values you have !
	 * @param values the ID value restriction. Can be empty. Must not be null.
	 */
	public IdIn(Collection<Long> values) {
		this.values.addAll(values);
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * Simply build a SQL portion : '[Context][ID column] IN (?,?...)' and fill the parameters.
	 */
	@Override
	public <Y extends Yopable> String toSQL(Context<Y> context, Parameters parameters) {
		if(this.values.isEmpty()) {
			return "";
		}

		String idColumn = ORMUtil.getIdColumn(context);

		return MessageFormat.format(
			IN,
			idColumn,
			this.values
				.stream()
				.map(value -> {parameters.addParameter(idColumn + "=" + value, value); return "?";})
				.collect(Collectors.joining(","))
		);
	}
}
