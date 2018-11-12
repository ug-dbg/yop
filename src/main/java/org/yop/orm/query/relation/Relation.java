package org.yop.orm.query.relation;

import org.apache.commons.collections4.CollectionUtils;
import org.yop.orm.annotations.JoinColumn;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.IJoin;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.Query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A relation is a common interface to generate UPSERT/DELETE SQL for a {@link JoinColumn} or {@link JoinTable} field.
 * <br>
 * i.e. when we have to translate Java POJO objects into SQL UPSERT/DELETE queries.
 * <br><br>
 * The 'factory' method {@link #relation(Collection, IJoin)}
 * returns either a {@link JoinColumnRelation} or {@link JoinTableRelation}.
 * <br><br>
 * <b>
 *     Some of the methods of this interface are either suited for @JoinTable, @JoinColumn or both.
 *     <br>
 *     The implementation default of these method → empty list of queries.
 * </b>
 */
public interface Relation {

	/**
	 * Create a collection of queries to delete the relation between the source objects using the join directive
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return implementation default : an empty list
	 */
	default Collection<Query> toSQLDelete(Config config) {
		return new ArrayList<>(0);
	}

	/**
	 * Create a collection of queries to insert the relation between the source objects using the join directive
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return implementation default : an empty list
	 */
	default Collection<Query> toSQLInsert(Config config) {
		return new ArrayList<>(0);
	}

	/**
	 * Create a collection of queries to update the relation between the source objects using the join directive
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return implementation default : an empty list
	 */
	default Collection<Query> toSQLUpdate(Config config) {
		return new ArrayList<>(0);
	}

	/**
	 * Create a collection of queries to batch insert the relation between the source objects using the join directive.
	 * These queries should certainly use {@link org.yop.orm.sql.Parameters.DelayedValue}.
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return implementation default : an empty list
	 */
	default Collection<Query> toSQLBatchInsert(Config config) {
		return new ArrayList<>(0);
	}

	/**
	 * Create a collection of queries to batch update the relation between the source objects using the join directive.
	 * These queries should certainly use {@link org.yop.orm.sql.Parameters.DelayedValue}.
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return implementation default : an empty list
	 */
	default Collection<Query> toSQLBatchUpdate(Config config) {
		return new ArrayList<>(0);
	}

	/**
	 * Create a Relation object that can generate the DELETE/UPSERT code for the join directive on given objects.
	 * <ul>
	 *  <li>@JoinColumn → {@link JoinColumnRelation}</li>
	 *  <li>@JoinTable → {@link JoinTableRelation}</li>
	 *  <li>no annotation → an empty {@link Relation}, that does nothing :)</li>
	 *  </ul>
	 * @param sources the source objects, on which the join directive can be applied
	 * @param join    the join directive
	 * @param <From>  the relation source type
	 * @param <To>    the relation target type
	 * @return a Relation implementation
	 */
	@SuppressWarnings("unchecked")
	static <From extends Yopable, To extends Yopable> Relation relation(Collection<From> sources, IJoin<From, To> join) {
		if (CollectionUtils.isEmpty(sources)) {
			return new Relation() {};
		}

		From source = sources.iterator().next();
		Field field = join.getField((Class<From>) source.getClass());
		if (field.isAnnotationPresent(JoinTable.class)) {
			return new JoinTableRelation<>(sources, join);
		}
		if (field.isAnnotationPresent(JoinColumn.class)) {
			return new JoinColumnRelation<>(sources, join);
		}
		return new Relation() {};
	}
}
