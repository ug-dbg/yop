package org.yop.orm.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.sql.Constants;

/**
 * Aggregate some common stuff for the model tests classes.
 */
public abstract class Persistent implements Yopable {

	@Id(sequence = Constants.DEFAULT_SEQ)
	@Column(name = "id")
	private Long id;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		return o != null && o instanceof Yopable && this.equals((Yopable) o);
	}
}
