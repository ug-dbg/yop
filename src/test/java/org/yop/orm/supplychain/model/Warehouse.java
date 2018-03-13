package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.util.ArrayList;
import java.util.List;

@Table(name = "warehouse")
public class Warehouse extends Persistent {

	@Column(name = "address")
	private String address;

	@Column(name = "active")
	private boolean active;

	@Column(name = "capacity")
	private long capacity;

	@JoinTable(
		table = "rel_warehouse_organisation",
		sourceColumn = "id_warehouse",
		targetColumn = "id_organisation"
	)
	private transient Organisation owner;

	@JoinTable(
		table = "rel_warehouse_product",
		sourceColumn = "id_organisation",
		targetColumn = "id_product"
	)
	private List<Product> products = new ArrayList<>();

	@JoinTable(
		table = "rel_delivery_warehouse",
		sourceColumn = "id_warehouse",
		targetColumn = "id_delivery"
	)
	private transient List<Delivery> deliveries = new ArrayList<>();

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public long getCapacity() {
		return capacity;
	}

	public void setCapacity(long capacity) {
		this.capacity = capacity;
	}

	public Organisation getOwner() {
		return owner;
	}

	public void setOwner(Organisation owner) {
		this.owner = owner;
	}

	public List<Product> getProducts() {
		return products;
	}

	public List<Delivery> getDeliveries() {
		return deliveries;
	}
}
