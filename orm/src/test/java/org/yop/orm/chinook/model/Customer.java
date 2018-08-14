package org.yop.orm.chinook.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.util.HashSet;
import java.util.Set;

@Table(name = "chinook_customer")
public class Customer extends Persistent {

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "last_name")
	private String lastName;

	@NaturalId
	@Column(name = "email")
	private String email;

	@Column(name = "phone")
	private String phone;

	@Column(name = "company")
	private String company;

	@Column(name = "address")
	private String address;

	@Column(name = "city")
	private String city;

	@Column(name = "postal_code")
	private String postalCode;

	@Column(name = "state")
	private String state;

	@Column(name = "country")
	private String country;

	@JoinTable(
		table = "rel_customer_invoice",
		sourceColumn = "id_customer",
		targetColumn = "id_invoice"
	)
	private Set<Invoice> invoices = new HashSet<>();

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public Set<Invoice> getInvoices() {
		return invoices;
	}
}
