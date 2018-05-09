package org.yop.orm.chinook.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Table(name = "invoice")
public class Invoice extends Persistent {

	@Column(name = "invoice_date")
	private LocalDateTime invoiceDate;

	@Column(name = "billingAddress")
	private String billingAddress;

	@Column(name = "billingCity")
	private String billingCity;

	@Column(name = "billing_postal_code")
	private String billingPostalCode;

	@Column(name = "billing_state")
	private String billingState;

	@Column(name = "billing_country")
	private String billingCountry;

	@Column(name = "total")
	private double total;

	@JoinTable(
		table = "rel_customer_invoice",
		sourceColumn = "id_invoice",
		targetColumn = "id_customer"
	)
	private transient Customer customer;

	@JoinTable(
		table = "rel_invoice_line",
		sourceColumn = "id_invoice",
		targetColumn = "id_line"
	)
	private Set<InvoiceLine> lines = new HashSet<>();

	public LocalDateTime getInvoiceDate() {
		return invoiceDate;
	}

	public void setInvoiceDate(LocalDateTime invoiceDate) {
		this.invoiceDate = invoiceDate;
	}

	public String getBillingAddress() {
		return billingAddress;
	}

	public void setBillingAddress(String billingAddress) {
		this.billingAddress = billingAddress;
	}

	public String getBillingCity() {
		return billingCity;
	}

	public void setBillingCity(String billingCity) {
		this.billingCity = billingCity;
	}

	public String getBillingPostalCode() {
		return billingPostalCode;
	}

	public void setBillingPostalCode(String billingPostalCode) {
		this.billingPostalCode = billingPostalCode;
	}

	public String getBillingState() {
		return billingState;
	}

	public void setBillingState(String billingState) {
		this.billingState = billingState;
	}

	public String getBillingCountry() {
		return billingCountry;
	}

	public void setBillingCountry(String billingCountry) {
		this.billingCountry = billingCountry;
	}

	public double getTotal() {
		return total;
	}

	public void setTotal(double total) {
		this.total = total;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Set<InvoiceLine> getLines() {
		return lines;
	}
}
