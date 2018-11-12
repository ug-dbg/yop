package org.yop.orm.query;

import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Config;
import org.yop.orm.util.MessageUtil;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * ORDER BY clause.
 * <br>
 * For now, Yop only enables sorting on {@link Select} root context target class fields.
 * <b>It won't work using a relation's getter </b>.
 * <br><br>
 * It aims at writing order clauses like this :
 * {@code OrderBy.orderBy(Pojo::isActive, true).thenBy(Pojo::getType, false).thenBy(Pojo::getVersion, true)}
 * @param <T> the target class type
 */
public class OrderBy<T extends Yopable> {

	private static final String ORDER_BY = " ORDER BY ";
	private static final String ASC      = " ASC ";
	private static final String DESC     = " DESC ";

	/** Order fields. An order is just a getter and an ASC/DESC boolean */
	private List<Order<T, ?>> orders = new ArrayList<>();

	/**
	 * Restricted visibility constructor. Please use {@link #orderBy(Function, boolean)}.
	 */
	OrderBy() {}

	/**
	 * Initialize an Order by, for a given field.
	 * @param getter the field getter
	 * @param asc    true → ASC, false → DESC
	 * @param <T> the target type (holding the field)
	 * @return the Order by clause
	 */
	public static <T extends Yopable> OrderBy<T> orderBy(Function<T, ?> getter, boolean asc) {
		OrderBy<T> orderBy = new OrderBy<>();
		orderBy.orders.add(new Order<>(getter, asc));
		return orderBy;
	}

	/**
	 * Append a field ordering to this "Order by" clause.
	 * @param getter the field getter
	 * @param asc    true → ASC, false → DESC
	 * @return the current Order by clause, for chaining purposes
	 */
	public OrderBy<T> thenBy(Function<T, ?> getter, boolean asc) {
		this.orders.add(new Order<>(getter, asc));
		return this;
	}

	/**
	 * Generate the 'ORDER BY' SQL portion for the {@link #orders}
	 * @param target the target type (holding the fields whose getters are into {@link #orders}).
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the SQL portion that can be added to the {@link Select} query.
	 */
	public String toSQL(Class<T> target, Config config) {
		List<String> orderColumns = new ArrayList<>();
		for (Order<T, ?> order : this.orders) {
			Field field = Reflection.findField(target, order.getter);

			String orderColumn =
				ORMUtil.getTargetName(target)
				+ config.dot()
				+ ORMUtil.getColumnName(field);

			orderColumn += order.asc ? ASC : DESC;
			orderColumns.add(orderColumn);
		}
		return this.orders.isEmpty() ? "" : ORDER_BY + MessageUtil.join(", ", orderColumns);
	}

	/**
	 * An order clause : A field getter (From → To) and a boolean (true → ASC, false → DESC)
	 * @param <From> the root type
	 * @param <To>   the target type
	 */
	private static class Order<From extends Yopable, To> {
		/** The field getter */
		private Function<From, To> getter;

		/** true → ASC, false → DESC */
		private boolean asc;

		/**
		 * Order clause constructor. Give me all I need !
		 * @param getter the field getter
		 * @param asc    true → ASC, false → DESC
		 */
		private Order(Function<From, To> getter, boolean asc) {
			this.getter = getter;
			this.asc = asc;
		}
	}
}
