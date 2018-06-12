package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

@Table(name="supplychain_price")
public class Price extends Persistent {

	@Column(name = "value")
	private float value;

	@JoinTable(table = "rel_price_voucher", sourceColumn = "idPrice", targetColumn = "idVoucher")
	private Voucher voucher;

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
	}

	public Voucher getVoucher() {
		return voucher;
	}

	public void setVoucher(Voucher voucher) {
		this.voucher = voucher;
	}
}
