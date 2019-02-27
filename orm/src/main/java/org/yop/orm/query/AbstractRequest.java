package org.yop.orm.query;

import org.yop.orm.exception.YopInvalidJoinException;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.JoinUtil;

import java.util.Collection;
import java.util.function.Function;

/**
 * A request that have join clauses relative to a context (e.g. SQL query or Serialize query).
 * <br>
 * This class should contain all the <i>join</i> methods implementations.
 * @param <Request> the request type (e.g. Select, Upsert, Delete, JSON, XML...)
 * @param <T> the request target type
 */
public abstract class AbstractRequest<Request extends AbstractRequest, T extends Yopable> {

	/** Root context : target class and SQL path **/
	protected final Context<T> context;

	/** Join clauses */
	protected IJoin.Joins<T> joins = new IJoin.Joins<>();

	/**
	 * Default constructor : final field {@link #context} must be initialized.
	 * @param context the context of the request
	 */
	public AbstractRequest(Context<T> context) {
		this.context = context;
	}

	/**
	 * Get the root target class.
	 * @return {@link Context#getTarget()} from {@link #context}
	 */
	public Class<T> getTarget() {
		return this.context.getTarget();
	}

	/**
	 * Get the joins for this request.
	 * <br>
	 * N.B. This can be an implementation for org.yop.orm.query.serialize.Serialize#getJoins()
	 * @return {@link #joins}
	 */
	public IJoin.Joins<T> getJoins() {
		return this.joins;
	}

	/**
	 * Join to a new type.
	 * @param join the join clause
	 * @param <R> the target join type
	 * @return the current request, for chaining purpose
	 */
	@SuppressWarnings("unchecked")
	public <R extends Yopable> Request join(IJoin<T, R> join) {
		this.joins.add(join);
		return (Request) this;
	}

	/**
	 * Add relations - to others Yopable types.
	 * @param joins the join clauses
	 * @return the current directive, for chaining purpose
	 */
	@SuppressWarnings("unchecked")
	public Request join(Collection<IJoin<T, ?>> joins) {
		this.joins.addAll(joins);
		return (Request) this;
	}

	/**
	 * Explicit join using a getter.
	 * @param getter the getter method reference to the target join class
	 * @param <A> the target type for the join (i.e. the return type of the getter)
	 * @return the current request, for chaining purpose
	 * @throws YopInvalidJoinException if the path is invalid
	 */
	@SuppressWarnings("unchecked")
	public <A> Request join(Function<T, A> getter) {
		this.joins.join(this.context, getter);
		return (Request) this;
	}

	/**
	 * Explicit join using 2 consecutive getters.
	 * @param first  the getter method reference to the first target join class
	 * @param second the getter method reference from the first target join class to a second one.
	 * @param <A> the target type for the first join (i.e. the return type of the first getter)
	 * @param <B> the source type for the second join (i.e. the return type of the first getter, or a collection of it)
	 * @param <C> the target type for the second join (i.e. the return type of the second getter)
	 * @return the current request, for chaining purpose
	 * @throws YopInvalidJoinException if the path is invalid
	 */
	@SuppressWarnings("unchecked")
	public <A, B, C> Request join(Function<T, A> first, Function<B, C> second) {
		this.joins.join(this.context, first, second);
		return (Request) this;
	}

	/**
	 * Explicit join using 3 consecutive getters.
	 * @param first  the getter method reference to the first target join class
	 * @param second the getter method reference from the first target join class to a second one.
	 * @param third  the getter method reference from the second target join class to a third one.
	 * @param <A> the target type for the first join (i.e. the return type of the first getter)
	 * @param <B> the source type for the second join (i.e. the return type of the first getter, or a collection of it)
	 * @param <C> the target type for the second join (i.e. the return type of the second getter)
	 * @param <D> the source type for the third join (i.e. the return type of the second getter, or a collection of it)
	 * @param <E> the target type for the third join (i.e. the return type of the third getter)
	 * @return the current request, for chaining purpose
	 * @throws YopInvalidJoinException if the path is invalid
	 */
	@SuppressWarnings("unchecked")
	public <A, B, C, D, E> Request join(
		Function<T, A> first,
		Function<B, C> second,
		Function<D, E> third) {
		this.joins.join(this.context, first, second, third);
		return (Request) this;
	}

	/**
	 * Explicit join using 4 consecutive getters.
	 * @param first  the getter method reference to the first target join class
	 * @param second the getter method reference from the first target join class to a second one.
	 * @param third  the getter method reference from the second target join class to a third one.
	 * @param fourth the getter method reference from the third target join class to a fourth one.
	 * @param <A> the target type for the first join (i.e. the return type of the first getter)
	 * @param <B> the source type for the second join (i.e. the return type of the first getter, or a collection of it)
	 * @param <C> the target type for the second join (i.e. the return type of the second getter)
	 * @param <D> the source type for the third join (i.e. the return type of the second getter, or a collection of it)
	 * @param <E> the target type for the third join (i.e. the return type of the third getter)
	 * @param <F> the source type for the fourth join (i.e. the return type of the third getter, or a collection of it)
	 * @param <G> the target type for the fourth join (i.e. the return type of the fourth getter)
	 * @return the current request, for chaining purpose
	 * @throws YopInvalidJoinException if the path is invalid
	 */
	@SuppressWarnings("unchecked")
	public <A, B, C, D, E, F, G> Request join(
		Function<T, A> first,
		Function<B, C> second,
		Function<D, E> third,
		Function<F, G> fourth) {
		this.joins.join(this.context, first, second, third, fourth);
		return (Request) this;
	}

	/**
	 * Fetch the whole data graph. Stop on transient fields.
	 * <br>
	 * <b>⚠⚠⚠ There must be no cycle in the data graph model ! ⚠⚠⚠</b>
	 * <br><br>
	 * <b>⚠⚠⚠ Any join previously set is cleared ! Please add transient fetch clause after this ! ⚠⚠⚠</b>
	 * @return the current request, for chaining purpose
	 */
	@SuppressWarnings("unchecked")
	public Request joinAll() {
		this.joins.clear();
		JoinUtil.joinAll(this.context.getTarget(), this.joins);
		return (Request) this;
	}

	/**
	 * Add the joins which are targeted by profiles, using {@link org.yop.orm.annotations.JoinProfile} on fields.
	 * @return the current request, for chaining purpose
	 */
	@SuppressWarnings("unchecked")
	public Request joinProfiles(String... profiles) {
		if (profiles.length > 0) {
			JoinUtil.joinProfiles(this.context.getTarget(), this.joins, profiles);
		}

		return (Request) this;
	}

	/**
	 * Turn this query into another {@link AbstractRequest} query, with the same {@link #joins}.
	 * <br>
	 * <b>The joins clauses are not duplicated when creating the JSON query !</b>
	 * @param request the target request
	 * @return the target request query with the current request joins parameters
	 */
	public <OtherRequest extends AbstractRequest<OtherRequest, T>> OtherRequest to(OtherRequest request) {
		this.joins.forEach(request::join);
		return request;
	}
}
