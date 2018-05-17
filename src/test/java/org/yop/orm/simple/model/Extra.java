package org.yop.orm.simple.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinColumn;
import org.yop.orm.model.Yopable;

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
}
