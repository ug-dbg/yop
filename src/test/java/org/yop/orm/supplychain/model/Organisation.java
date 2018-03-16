package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.util.ArrayList;
import java.util.List;

@Table(name="organisation")
public class Organisation extends Persistent {

	@NaturalId
	@Column(name = "name")
	private String name;

	@NaturalId
	@Column(name = "someDummyFloat")
	private float someDummyFloat;

	@JoinTable(
		table = "rel_warehouse_organisation",
		sourceColumn = "id_organisation",
		targetColumn = "id_warehouse"
	)
	private List<Warehouse> warehouses = new ArrayList<>();

	@JoinTable(
		table = "rel_employee_organisation",
		sourceColumn = "id_organisation",
		targetColumn = "id_employee"
	)
	private List<Employee> employees = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getSomeDummyFloat() {
		return someDummyFloat;
	}

	public void setSomeDummyFloat(float someDummyFloat) {
		this.someDummyFloat = someDummyFloat;
	}

	public List<Warehouse> getWarehouses() {
		return warehouses;
	}

	public List<Employee> getEmployees() {
		return employees;
	}
}
