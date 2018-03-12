package org.yop.orm.simple.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.model.Yopable;
import org.yop.orm.model.json.YopableJson;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class Other implements YopableJson {

	@Id(sequence = "seq_OTHER")
	@Column(name = "ID")
	private Long id;

	@NaturalId
	@Column(name = "NAME")
	private String name;

	@NaturalId
	@Column(name = "TIMESTAMP")
	private LocalDateTime timestamp;

	@JoinTable(table = "POJO_OTHER_relation", sourceColumn = "idOther", targetColumn = "idPojo")
	private transient Set<Pojo> pojos = new HashSet<>();

	public Set<Pojo> getPojos() {
		return pojos;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Yopable && this.equals((Yopable) o);
	}

	@Override
	public String toString() {
		return "Other{" +
			"id=" + id
			+ ", name='" + name + '\''
			+ ", timestamp=" + timestamp
			+ ", pojos=" + pojos
		+'}';
	}
}
