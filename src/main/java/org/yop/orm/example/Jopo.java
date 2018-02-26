package org.yop.orm.example;

import org.yop.orm.model.Yopable;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;

import java.util.Objects;

public class Jopo implements Yopable {

	@Column(name = "ID")
	private Long id;

	@Column(name = "NAME")
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
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Jopo jopo = (Jopo) o;
		return Objects.equals(name, jopo.name);
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