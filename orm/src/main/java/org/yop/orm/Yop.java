package org.yop.orm;

import org.yop.orm.model.Yopable;
import org.yop.orm.query.batch.BatchUpsert;
import org.yop.orm.query.join.IJoin;
import org.yop.orm.query.serialize.json.JSON;
import org.yop.orm.query.sql.*;

import java.util.Collection;
import java.util.function.Function;

/**
 * This is an entry point to YOP's main API.
 * <br>
 * You will find here some static methods to the the CRUD query builders :
 * <ul>
 *     <li>{@link Select}</li>
 *     <li>{@link Upsert} and {@link BatchUpsert}</li>
 *     <li>{@link Delete}</li>
 *     <li>{@link Hydrate}</li>
 *     <li>{@link JSON}</li>
 * </ul>
 * You will also find static methods to YOP join directives ({@link IJoin} implementations):
 * <ul>
 *     <li>to a single {@link Yopable} → {@link SQLJoin} : {@link #to(Function)}</li>
 *     <li>to a collection of {@link Yopable} → {@link SQLJoin} : {@link #toN(Function)}</li>
 * </ul>
 * Consider this class an API <i>vade mecum</i>.
 */
public class Yop {

	/** Private constructor. This is an utility class. */
	private Yop() {}

	/**
	 * Shortcut to {@link Select#from(Class)}.
	 * @param what the target {@link Yopable} annotated class
	 * @param <What> the target type
	 * @return a {@link Select} directive.
	 */
	public static <What extends Yopable> Select<What> select(Class<What> what) {
		return Select.from(what);
	}

	/**
	 * Shortcut to {@link Upsert#from(Class)}.
	 * <br>
	 * Yop offers the same API for INSERT and UPDATE queries :
	 * <ul>
	 *     <li>the Yopable object has a non null ID field → update</li>
	 *     <li>the Yopable object has a null ID field → insert</li>
	 * </ul>
	 * See the {@link Upsert#checkNaturalID()} if you don't have the objects IDs but natural keys.
	 * @param what the target {@link Yopable} annotated class
	 * @param <What> the target type
	 * @return a {@link Upsert} directive.
	 */
	public static <What extends Yopable> Upsert<What> upsert(Class<What> what) {
		return Upsert.from(what);
	}

	/**
	 * Shortcut to {@link BatchUpsert#from(Class)}.
	 * <br>
	 * The API is similar to {@link #upsert(Class)}.
	 * @param what the target {@link Yopable} annotated class
	 * @param <What> the target type
	 * @return a {@link Upsert} (actually a {@link BatchUpsert} directive.
	 */
	public static <What extends Yopable> Upsert<What> batchUpsert(Class<What> what) {
		return BatchUpsert.from(what);
	}

	/**
	 * Shortcut to {@link Delete}.
	 * @param what the target {@link Yopable} annotated class
	 * @param <What> the target type
	 * @return a {@link Delete} directive.
	 */
	public static <What extends Yopable> Delete<What> delete(Class<What> what) {
		return Delete.from(what);
	}

	/**
	 * Shortcut to {@link Hydrate}.
	 * @param what the target {@link Yopable} annotated class
	 * @param <What> the target type
	 * @return a {@link Hydrate} directive.
	 */
	public static <What extends Yopable> Hydrate<What> hydrate(Class<What> what) {
		return Hydrate.from(what);
	}

	/**
	 * Shortcut to {@link JSON}.
	 * @param what the target {@link Yopable} annotated class
	 * @param <What> the target type
	 * @return a {@link JSON} directive.
	 */
	public static <What extends Yopable> JSON<What> json(Class<What> what) {
		return JSON.from(what);
	}

	/**
	 * Shortcut to {@link SQLJoin}.
	 * <br>
	 * This is the {@link IJoin} implementation for 1 → 1 or N → 1 relationships,
	 * i.e. when the target class is a {@link Yopable} and not a {@link Collection} of {@link Yopable}.
	 * @param getter the relation field getter
	 * @param <From> the relation source type
	 * @param <To>   the relation target type
	 * @return an {@link SQLJoin} relation for the given getter
	 */
	public static <From extends Yopable, To extends Yopable> SQLJoin<From, To> to(Function<From, To> getter) {
		return SQLJoin.to(getter);
	}

	/**
	 * Shortcut to {@link SQLJoin}.
	 * <br>
	 * This is the {@link IJoin} implementation for 1 → N or N → N relationships,
	 * i.e. when the target class is a {@link Collection} of {@link Yopable} and not a single {@link Yopable}.
	 * @param getter the relation field getter
	 * @param <From> the relation source type
	 * @param <To>   the relation target type
	 * @return an {@link SQLJoin} relation for the given getter
	 */
	public static <From extends Yopable, To extends Yopable> SQLJoin<From, To> toN(Function<From, Collection<To>> getter) {
		return SQLJoin.toN(getter);
	}
}
