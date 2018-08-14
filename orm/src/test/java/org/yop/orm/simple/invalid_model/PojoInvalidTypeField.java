package org.yop.orm.simple.invalid_model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Yopable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

@Table(name = "simple_pojo")
public class PojoInvalidTypeField implements Yopable {

	public enum Type {FOO, BAR}

	@Id(sequence = "seq_POJO")
	@Column(name = "id")
	private Long id;

	@NaturalId
	@Column(name = "VERSION")
	private Integer[] version;

	@NaturalId
	@Column(name = "ACTIVE")
	private boolean active;

	@Column(name = "VERY_LONG_INT")
	private BigInteger aVeryLongInteger;

	@Column(name = "VERY_LONG_FLOAT")
	private BigDecimal aVeryLongFloat;

	@Column(name = "TYPE", enum_strategy = Column.EnumStrategy.ORDINAL)
	private PojoInvalidTable.Type type;

	public Integer[] getVersion() {
		return version;
	}

	public void setVersion(Integer[] version) {
		this.version = version;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public PojoInvalidTable.Type getType() {
		return type;
	}

	public void setType(PojoInvalidTable.Type type) {
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

	@Override
	public int hashCode() {
		return Objects.hash(version);
	}

	@Override
	public String toString() {
		return "Pojo{"
			+ "id=" + id
			+ ", version=" + this.version
		+ '}';
	}

}
