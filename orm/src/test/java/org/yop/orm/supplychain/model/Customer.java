package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Table(name = "supplychain_customer")
public class Customer extends Persistent {

	@NaturalId
	@Column(name = "name")
	private String name;

	@NaturalId
	@Column(name = "date_of_birth")
	private LocalDate dateOfBirth;

	@NaturalId
	@Column(name = "nice")
	private boolean nice;

	@NaturalId
	@Column(name = "phone_number")
	private long phoneNumber;

	@NaturalId
	@Column(name = "sock_size")
	private short sockSize;

	@Column(name = "about")
	private String about;

	@JoinTable(
		table = "rel_customer_orders",
		sourceColumn = "id_customer",
		targetColumn = "id_order"
	)
	private List<Order> orders = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public boolean isNice() {
		return nice;
	}

	public void setNice(boolean nice) {
		this.nice = nice;
	}

	public long getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(long phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public short getSockSize() {
		return sockSize;
	}

	public void setSockSize(short sockSize) {
		this.sockSize = sockSize;
	}

	public String getAbout() {
		return about;
	}

	public void setAbout(String about) {
		this.about = about;
	}

	public List<Order> getOrders() {
		return orders;
	}
}
