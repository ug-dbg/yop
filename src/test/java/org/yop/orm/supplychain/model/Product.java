package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.util.ArrayList;
import java.util.List;

@Table(name = "product")
public class Product  extends Persistent {

	@Column(name = "name")
	private String name;

	@JoinTable(
		table = "rel_product_reference",
		sourceColumn = "id_product",
		targetColumn = "id_reference"
	)
	private Reference reference;

	@JoinTable(
		table = "rel_product_category",
		sourceColumn = "id_product",
		targetColumn = "id_category"
	)
	private List<Category> categories = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Reference getReference() {
		return reference;
	}

	public void setReference(Reference reference) {
		this.reference = reference;
	}
}
