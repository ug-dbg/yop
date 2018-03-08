package org.yop.orm.evaluation;

import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Constants;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * IN clause evaluation.
 * <br>
 * Something like : '[Context][field] IN (?,?...)'
 * <br>
 * This does not work for @JoinTable fields. Sorry about that.
 */
public class In implements Evaluation {

	private static final String IN = " {0} IN ({1}) ";

	/** The field getter on which a restriction is set */
	private Function<?, ?> getter;

	/** The restriction values */
	private Collection<Object> values = new HashSet<>();

	/**
	 * Default constructor : I need the field getter and the restriction values.
	 * @param getter the field getter
	 * @param values the restriction values
	 * @param <From> the source type (holding the field)
	 * @param <To>   the target type (field type)
	 */
	public <From extends Yopable, To> In(Function<From, To> getter, Collection<To> values) {
		this.getter = getter;
		this.values.addAll(values);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y extends Yopable> String toSQL(Context<Y> context, Parameters parameters) {
		if(this.values.isEmpty()) {
			return "";
		}

		String column =
			context.getPath()
			+ Constants.DOT
			+ ORMUtil.getColumnName(Reflection.findField(context.getTarget(), (Function<Y, ?>)this.getter));

		return MessageFormat.format(
			IN,
			column,
			this.values
				.stream()
				.map(value -> {parameters.addParameter(column + "=" + value, value); return "?";})
				.collect(Collectors.joining(","))
		);
	}

}
