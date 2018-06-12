package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "supplychain_delivery")
public class Delivery extends Persistent {

	@Column(name = "expected")
	private LocalDateTime expected;

	@JoinTable(
		table = "rel_customer_delivery",
		sourceColumn = "id_delivery",
		targetColumn = "id_order"
	)
	private Order order;

	@JoinTable(
		table = "rel_delivery_warehouse",
		sourceColumn = "id_delivery",
		targetColumn = "id_warehouse"
	)
	private Warehouse from;

	@JoinTable(
		table = "rel_delivery_deliveryservice",
		sourceColumn = "id_delivery",
		targetColumn = "id_deliveryservice"
	)
	private DeliveryService service;

	@JoinTable(
		table = "rel_delivery_products",
		sourceColumn = "id_delivery",
		targetColumn = "id_product"
	)
	private List<Product> products = new ArrayList<>();

	public LocalDateTime getExpected() {
		return expected;
	}

	public void setExpected(LocalDateTime expected) {
		this.expected = expected;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public Warehouse getFrom() {
		return from;
	}

	public void setFrom(Warehouse from) {
		this.from = from;
	}

	public DeliveryService getService() {
		return service;
	}

	public void setService(DeliveryService service) {
		this.service = service;
	}

	public List<Product> getProducts() {
		return products;
	}
}
