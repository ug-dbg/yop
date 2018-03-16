package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "order_table")
public class Order extends Persistent {

	@Column(name = "timestamp", not_null = true)
	private LocalDateTime orderTimeStamp;

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

	@JoinTable(
		table = "rel_order_voucher",
		sourceColumn = "id_voucher",
		targetColumn = "id_order"
	)
	private List<Voucher> vouchers = new ArrayList<>();

	@JoinTable(
		table = "rel_order_return",
		sourceColumn = "id_order",
		targetColumn = "id_return"
	)
	private Return orderReturn;

	@JoinTable(
		table = "rel_order_payment",
		sourceColumn = "id_order",
		targetColumn = "id_payment"
	)
	private Payment payment;

	public LocalDateTime getOrderTimeStamp() {
		return orderTimeStamp;
	}

	public void setOrderTimeStamp(LocalDateTime orderTimeStamp) {
		this.orderTimeStamp = orderTimeStamp;
	}

	public List<Product> getProducts() {
		return products;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public List<Voucher> getVouchers() {
		return vouchers;
	}

	public Return getOrderReturn() {
		return orderReturn;
	}

	public void setOrderReturn(Return orderReturn) {
		this.orderReturn = orderReturn;
	}

	public Payment getPayment() {
		return payment;
	}

	public void setPayment(Payment payment) {
		this.payment = payment;
	}
}
