package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.util.ArrayList;
import java.util.List;

@Table(name = "deliveryservice")
public class DeliveryService extends Persistent {

	@Column(name = "name")
	private String name;

	@NaturalId
	@Column(name = "siret")
	private String siret;

	@JoinTable(
		table = "rel_delivery_deliveryservice",
		sourceColumn = "id_deliveryservice",
		targetColumn = "id_delivery"
	)
	private transient List<Delivery> deliveries = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSiret() {
		return siret;
	}

	public void setSiret(String siret) {
		this.siret = siret;
	}

	public List<Delivery> getDeliveries() {
		return deliveries;
	}
}
