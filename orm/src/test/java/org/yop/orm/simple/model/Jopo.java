package org.yop.orm.simple.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.ORMUtil;

import java.util.Objects;

@Table(name = "simple_jopo")
public class Jopo {

	@Id(sequence = "seq_JOPO")
	@Column(name = "id")
	private Long id;

	@Column(name = "NAME", not_null = true)
	private String name;

	@JoinTable(table = "POJO_JOPO_relation", sourceColumn = "idJOPO", targetColumn = "idPOJO")
	private transient Pojo pojo;

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Pojo getPojo() {
		return this.pojo;
	}

	public void setPojo(Pojo pojo) {
		this.pojo = pojo;
	}

	@Override
	public boolean equals(Object o) {
		return ORMUtil.equals(this, o);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name);
	}

	@Override
	public String toString() {
		return "Jopo{"
			+ "id=" + this.id
			+ ", name='" + this.name + '\''
			+ ", pojo.id=" + (this.pojo != null ? this.pojo.getId() : "null")
		+ '}';
	}
}