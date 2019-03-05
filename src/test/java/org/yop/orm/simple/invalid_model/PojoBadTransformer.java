package org.yop.orm.simple.invalid_model;

import org.yop.orm.annotations.*;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.simple.model.Jopo;
import org.yop.orm.simple.model.Other;
import org.yop.orm.transform.ITransformer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@Table(name = "simple_pojo")
public class PojoBadTransformer implements Yopable {

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

	@Column(name = "VERY_LONG_INT", transformer = BadTransformer.class)
	private BigInteger aVeryLongInteger;

	@Column(name = "VERY_LONG_FLOAT")
	private BigDecimal aVeryLongFloat;

	@JoinTable(table = "POJO_JOPO_relation", sourceColumn = "idPOJO", targetColumn = "idJOPO")
	private Set<Jopo> jopos = new HashSet<>();

	@JoinTable(table = "POJO_OTHER_relation", sourceColumn = "idPojo", targetColumn = "idOther")
	private List<Other> others = new ArrayList<>();

	@JoinTable(table = "POJO_POJO_relation", sourceColumn = "idPOJO_a", targetColumn = "idPOJO_b")
	private transient List<PojoBadTransformer> children = new ArrayList<>();

	@JoinTable(table = "POJO_POJO_relation", sourceColumn = "idPOJO_b", targetColumn = "idPOJO_a")
	private transient PojoBadTransformer parent;

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

	public BigInteger getaVeryLongInteger() {
		return aVeryLongInteger;
	}

	public void setaVeryLongInteger(BigInteger aVeryLongInteger) {
		this.aVeryLongInteger = aVeryLongInteger;
	}

	public BigDecimal getaVeryLongFloat() {
		return aVeryLongFloat;
	}

	public void setaVeryLongFloat(BigDecimal aVeryLongFloat) {
		this.aVeryLongFloat = aVeryLongFloat;
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

	public PojoBadTransformer getParent() {
		return parent;
	}

	public void setParent(PojoBadTransformer parent) {
		this.parent = parent;
	}

	public List<PojoBadTransformer> getChildren() {
		return children;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PojoBadTransformer pojo = (PojoBadTransformer) o;
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

	private static class BadTransformer implements ITransformer<BigInteger> {

		public BadTransformer() {
			throw new YopRuntimeException("This transformer is bad !");
		}

		@Override
		public Object forSQL(BigInteger bigDecimal, Column column) {
			return null;
		}

		@Override
		public BigInteger fromSQL(Object fromJDBC, Class into) {
			return null;
		}
	}
}