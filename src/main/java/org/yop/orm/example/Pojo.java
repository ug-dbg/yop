package org.yop.orm.example;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.model.json.YopableJson;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Pojo implements YopableJson {

	@Column(name = "ID")
	private Long id;

	@NaturalId
	@Column(name = "VERSION")
	private Integer version;

	@JoinTable(table = "POJO_JOPO_relation", sourceColumn = "idPOJO", targetColumn = "idJOPO")
	private Set<Jopo> jopos = new HashSet<>();

	@JoinTable(table = "POJO_POJO_relation", sourceColumn = "idPOJO_b", targetColumn = "idPOJO_a")
	private transient Pojo parent;

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public Set<Jopo> getJopos() {
		return jopos;
	}

	public void setJopos(Set<Jopo> jopos) {
		this.jopos = jopos;
	}

	public Pojo getParent() {
		return parent;
	}

	public void setParent(Pojo parent) {
		this.parent = parent;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Pojo pojo = (Pojo) o;
		return Objects.equals(version, pojo.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(version);
	}

	@Override
	public String toString() {
		return "Pojo{" + "id=" + id + ", version=" + version + ", jopos=" + jopos + ", parent=" + parent + '}';
	}
}