package org.yop.orm.simple.non_generated_id.model;

import org.yop.orm.annotations.*;
import org.yop.orm.model.Yopable;

import java.time.LocalDate;
import java.util.Objects;

@Table(name = "non_generated_id_pojo")
public class Pojo implements Yopable {

	@Id(autoincrement = false)
	private String id;

	@Column(name = "VALUE")
	private String value;

	@Column(name = "POJO_DATE")
	private LocalDate date;

	@JoinColumn(local = "PARENT_ID", remote = "CHILD_ID")
	private Pojo parent;

	@JoinColumn(local = "CHILD_ID", remote = "PARENT_ID")
	private Pojo child;

	private Pojo() {}

	public Pojo(String id, String value, LocalDate date) {
		this.id = id;
		this.value = value;
		this.date = date;
	}

	public Pojo getParent() {
		return this.parent;
	}

	public void setParent(Pojo parent) {
		this.parent = parent;
	}

	public Pojo getChild() {
		return this.child;
	}

	public void setChild(Pojo child) {
		this.child = child;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || this.getClass() != o.getClass()) return false;
		Pojo pojo = (Pojo) o;
		return Objects.equals(this.id, pojo.id)
			&& Objects.equals(this.value, pojo.value)
			&& Objects.equals(this.date, pojo.date);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.value, this.date);
	}

	@Override
	public String toString() {
		return "Pojo{" +
			"id='" + this.id + '\'' +
			", value='" + this.value + '\'' +
			", date=" + this.date +
		'}';
	}
}
