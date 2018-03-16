package org.yop.orm.simple.model;

import org.yop.orm.annotations.Id;
import org.yop.orm.model.Yopable;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;

import java.util.Objects;

public class Jopo implements Yopable {

	@Id(sequence = "seq_JOPO")
	@Column(name = "id")
	private Long id;

	@Column(name = "NAME", not_null = true)
	private String name;

	@JoinTable(table = "POJO_JOPO_relation", sourceColumn = "idJOPO", targetColumn = "idPOJO")
	private transient Pojo pojo;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Pojo getPojo() {
		return pojo;
	}

	public void setPojo(Pojo pojo) {
		this.pojo = pojo;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Yopable && this.equals((Yopable) o);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return "Jopo{" + "id=" + id + ", name='" + name + '\'' + ", pojo.id=" + (pojo != null ? pojo.getId() : "null") + '}';
	}
}