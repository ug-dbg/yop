package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

@Table(name = "manufacturer")
public class Manufacturer extends Persistent {

	@Column(name = "name")
	private String name;

	@Column(name = "address")
	private String address;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
}
