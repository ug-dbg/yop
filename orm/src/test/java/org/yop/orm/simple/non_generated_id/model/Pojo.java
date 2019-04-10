package org.yop.orm.simple.non_generated_id.model;

import org.yop.orm.annotations.*;
import org.yop.orm.model.Yopable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

	@JoinTable(table = "RELATED_POJOS", sourceColumn = "FROM_ID", targetColumn = "TO_ID")
	private List<Pojo> related = new ArrayList<>();

	@JoinTable(table = "RELATED_POJOS", sourceColumn = "TO_ID", targetColumn = "FROM_ID")
	private List<Pojo> reverseRelated = new ArrayList<>();

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

	public List<Pojo> getRelated() {
		return this.related;
	}

	public List<Pojo> getReverseRelated() {
		return this.reverseRelated;
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
