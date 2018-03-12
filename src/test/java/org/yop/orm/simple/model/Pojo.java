package org.yop.orm.simple.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.model.json.YopableJson;

import java.util.*;

public class Pojo implements YopableJson {

	public enum Type {FOO, BAR}

	@Id(sequence = "seq_POJO")
	@Column(name = "ID")
	private Long id;

	@NaturalId
	@Column(name = "VERSION")
	private Integer version;

	@NaturalId
	@Column(name = "ACTIVE")
	private boolean active;

	@JoinTable(table = "POJO_JOPO_relation", sourceColumn = "idPOJO", targetColumn = "idJOPO")
	private Set<Jopo> jopos = new HashSet<>();

	@JoinTable(table = "POJO_OTHER_relation", sourceColumn = "idPojo", targetColumn = "idOther")
	private List<Other> others = new ArrayList<>();

	@JoinTable(table = "POJO_POJO_relation", sourceColumn = "idPOJO_a", targetColumn = "idPOJO_b")
	private transient List<Pojo> children = new ArrayList<>();

	@JoinTable(table = "POJO_POJO_relation", sourceColumn = "idPOJO_b", targetColumn = "idPOJO_a")
	private transient Pojo parent;

	@Column(name = "TYPE", enum_strategy = Column.EnumStrategy.ORDINAL)
	private Type type;

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Set<Jopo> getJopos() {
		return jopos;
	}

	public void setJopos(Set<Jopo> jopos) {
		this.jopos = jopos;
	}

	public List<Other> getOthers() {
		return others;
	}

	public Pojo getParent() {
		return parent;
	}

	public void setParent(Pojo parent) {
		this.parent = parent;
	}

	public List<Pojo> getChildren() {
		return children;
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
		return "Pojo{"
			+ "id=" + id
			+ ", version=" + this.version
			+ ", jopos="   + this.jopos
			+ ", others="  + this.others
			+ ", parent="  + this.parent
		+ '}';
	}
}