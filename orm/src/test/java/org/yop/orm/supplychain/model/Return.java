package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.time.LocalDateTime;

@Table(name = "supplychain_return")
public class Return extends Persistent {

	@Column(name = "return_date")
	private LocalDateTime when;

	@Column(name = "reason")
	private String reason;

	@JoinTable(
		table = "rel_order_return",
		sourceColumn = "id_return",
		targetColumn = "id_order"
	)
	private transient Order order;

	public LocalDateTime getWhen() {
		return when;
	}

	public void setWhen(LocalDateTime when) {
		this.when = when;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}
}
