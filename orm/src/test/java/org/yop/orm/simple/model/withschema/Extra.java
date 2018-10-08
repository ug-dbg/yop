package org.yop.orm.simple.model.withschema;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinColumn;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Yopable;

import java.util.Objects;

@Table(name = "simple_extra", schema = "yop")
public class Extra implements Yopable {

	@Id(sequence = "seq_EXTRA")
	@Column(name = "id")
	private Long id;

	@Column(name = "user_name")
	private String userName;

	@Column(name = "style")
	private String style;

	@JoinColumn(local = "id_other")
	private transient Other other;

	@JoinColumn(local = "id_super_extra")
	private transient SuperExtra superExtra;

	public String getUserName() {
		return this.userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getStyle() {
		return this.style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public Other getOther() {
		return this.other;
	}

	public void setOther(Other other) {
		this.other = other;
	}

	public SuperExtra getSuperExtra() {
		return this.superExtra;
	}

	public void setSuperExtra(SuperExtra superExtra) {
		this.superExtra = superExtra;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Extra extra = (Extra) o;
		return Objects.equals(this.userName, extra.userName) && Objects.equals(this.style, extra.style);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.userName, this.style);
	}
}
