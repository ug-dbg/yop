package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

@Table(name = "reference")
public class Reference extends Persistent {

	@NaturalId
	@Column(name = "manufacturer_ref")
	private String manufacturerReference;

	@JoinTable(
		table = "rel_reference_manufacturer",
		sourceColumn = "id_reference",
		targetColumn = "id_manufacturer"
	)
	private Manufacturer manufacturer;

	public String getManufacturerReference() {
		return manufacturerReference;
	}

	public void setManufacturerReference(String manufacturerReference) {
		this.manufacturerReference = manufacturerReference;
	}

	public Manufacturer getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(Manufacturer manufacturer) {
		this.manufacturer = manufacturer;
	}
}
