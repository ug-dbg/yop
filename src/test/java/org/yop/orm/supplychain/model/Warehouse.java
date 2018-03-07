package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

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
}
