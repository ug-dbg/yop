package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.util.ArrayList;
import java.util.List;

@Table(name = "category")
public class Category extends Persistent {

	@NaturalId
	@Column(name = "code")
	private String code;

	@Column(name = "name")
	private String name;

	@Column(name = "description")
	private String description;

	@JoinTable(
		table = "category",
		sourceColumn = "id_category",
		targetColumn = "id_product"
	)
	private transient List<Product> products = new ArrayList<>();

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Product> getProducts() {
		return products;
	}
}
