package org.yop.orm.simple.model.withschema;

import org.yop.orm.annotations.*;
import org.yop.orm.model.Yopable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Table(name = "simple_other", schema = "yop")
public class Other implements Yopable {

	@Id(sequence = "seq_OTHER")
	@Column(name = "id")
	private Long id;

	@NaturalId
	@Column(name = "NAME")
	private String name;

	@NaturalId
	@Column(name = "TIMESTAMP")
	private LocalDateTime timestamp;

	@JoinTable(table = "POJO_OTHER_relation", schema = "yop", sourceColumn = "idOther", targetColumn = "idPojo")
	private transient Set<Pojo> pojos = new HashSet<>();

	@JoinColumn(local = "id_extra", remote = "id_other")
	private Extra extra;

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

	public Extra getExtra() {
		return this.extra;
	}

	public void setExtra(Extra extra) {
		this.extra = extra;
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
		+'}';
	}
}
