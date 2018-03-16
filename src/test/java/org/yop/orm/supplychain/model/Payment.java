package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.time.LocalDateTime;

@Table(name = "payment")
public class Payment extends Persistent {

	public enum Method {CREDIT_CARD, CASH, CHEQUE, MONOPOLY_MONEY}

	@Column(name = "payment_date")
	private LocalDateTime when;

	@Column(name = "method")
	private Method method;

	@JoinTable(
		table = "rel_order_payment",
		sourceColumn = "id_payment",
		targetColumn = "id_order"
	)
	private transient Order order;

	public LocalDateTime getWhen() {
		return when;
	}

	public void setWhen(LocalDateTime when) {
		this.when = when;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}
}
