package org.yop.orm.simple.model;

import org.yop.orm.annotations.*;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.json.annotations.YopJSONTransient;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@Table(name = "simple_pojo")
public class Pojo implements Yopable {

	public enum Type {FOO, BAR}

	@Id(sequence = "seq_POJO")
	@Column(name = "id")
	private Long id;

	@NaturalId
	@Column(name = "VERSION")
	private Integer version;

	@NaturalId
	@Column(name = "ACTIVE")
	private boolean active;

	@Column(name = "VERY_LONG_INT")
	private BigInteger aVeryLongInteger;

	@Column(name = "VERY_LONG_FLOAT")
	private BigDecimal aVeryLongFloat;

	@YopJSONTransient
	@Column(name = "PASSWORD")
	private String password;

	@JoinTable(table = "POJO_JOPO_relation", sourceColumn = "idPOJO", targetColumn = "idJOPO")
	private Set<Jopo> jopos = new HashSet<>();

	@JoinTable(table = "POJO_OTHER_relation", sourceColumn = "idPojo", targetColumn = "idOther")
	private List<Other> others = new ArrayList<>();

	@YopTransient
	@JoinProfile(profiles = "pojo_children_and_parent")
	@JoinTable(table = "POJO_POJO_relation", sourceColumn = "idPOJO_a", targetColumn = "idPOJO_b")
	private List<Pojo> children = new ArrayList<>();

	@JoinProfile(profiles = "pojo_children_and_parent")
	@JoinTable(table = "POJO_POJO_relation", sourceColumn = "idPOJO_b", targetColumn = "idPOJO_a")
	private transient Pojo parent;

	@Column(name = "TYPE", enum_strategy = Column.EnumStrategy.ORDINAL)
	private Type type;

	public Integer getVersion() {
		return this.version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public boolean isActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Type getType() {
		return this.type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public BigInteger getaVeryLongInteger() {
		return this.aVeryLongInteger;
	}

	public void setaVeryLongInteger(BigInteger aVeryLongInteger) {
		this.aVeryLongInteger = aVeryLongInteger;
	}

	public BigDecimal getaVeryLongFloat() {
		return this.aVeryLongFloat;
	}

	public void setaVeryLongFloat(BigDecimal aVeryLongFloat) {
		this.aVeryLongFloat = aVeryLongFloat;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Set<Jopo> getJopos() {
		return this.jopos;
	}

	public void setJopos(Set<Jopo> jopos) {
		this.jopos = jopos;
	}

	public List<Other> getOthers() {
		return this.others;
	}

	public Pojo getParent() {
		return this.parent;
	}

	public void setParent(Pojo parent) {
		this.parent = parent;
	}

	public List<Pojo> getChildren() {
		return this.children;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Pojo pojo = (Pojo) o;
		return Objects.equals(this.version, pojo.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.version);
	}

	@Override
	public String toString() {
		return "Pojo{"
			+ "id=" + this.id
			+ ", version=" + this.version
			+ ", jopos="   + this.jopos
			+ ", others="  + this.others
			+ ", parent="  + this.parent
		+ '}';
	}
}