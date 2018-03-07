package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

@Table(name = "voucher")
public class Voucher extends Persistent {

	@Column(name = "label")
	private String label;

	@Column(name = "value")
	private float value;

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
	}
}
