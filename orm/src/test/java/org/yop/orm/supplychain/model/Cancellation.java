package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;

import java.time.LocalDateTime;

@Table(name = "supplychain_cancellation")
public class Cancellation {

	@Id
	private Long id;

	@Column(name = "reason")
	private String reason;

	@Column(name = "date")
	private LocalDateTime date;

	@JoinTable(
		table = "rel_order_cancellation",
		sourceColumn = "id_cancellation",
		targetColumn = "id_order"
	)
	private transient Order order;

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}
}
