package org.yop.orm.chinook.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

@Table(name = "chinook_invoice_line")
public class InvoiceLine extends Persistent {

	@Column(name = "unit_price")
	private float unitPrice;

	@Column(name = "quantity")
	private int quantity;

	@JoinTable(
		table = "rel_invoice_line_track",
		sourceColumn = "id_invoice_line",
		targetColumn = "id_track"
	)
	private Track track;

	@JoinTable(
		table = "rel_invoice_line",
		sourceColumn = "id_line",
		targetColumn = "id_invoice"
	)
	private transient Invoice invoice;

	public float getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(float unitPrice) {
		this.unitPrice = unitPrice;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public Track getTrack() {
		return track;
	}

	public void setTrack(Track track) {
		this.track = track;
	}

	public Invoice getInvoice() {
		return invoice;
	}

	public void setInvoice(Invoice invoice) {
		this.invoice = invoice;
	}
}
