package org.yop.orm.simple.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinColumn;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Yopable;

import java.util.Objects;

@Table(name = "simple_extra")
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
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public Other getOther() {
		return other;
	}

	public void setOther(Other other) {
		this.other = other;
	}

	public SuperExtra getSuperExtra() {
		return superExtra;
	}

	public void setSuperExtra(SuperExtra superExtra) {
		this.superExtra = superExtra;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Extra extra = (Extra) o;
		return Objects.equals(userName, extra.userName) && Objects.equals(style, extra.style);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userName, style);
	}
}
