package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.util.ArrayList;
import java.util.List;

@Table(name = "order_table")
public class Order extends Persistent {

	@JoinTable(
		table = "rel_order_products",
		sourceColumn = "id_order",
		targetColumn = "id_products"
	)
	private List<Product> products = new ArrayList<>();

	@JoinTable(
		table = "rel_customer_orders",
		sourceColumn = "id_order",
		targetColumn = "id_customer"
	)
	private transient Customer customer;

	public List<Product> getProducts() {
		return products;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
}
