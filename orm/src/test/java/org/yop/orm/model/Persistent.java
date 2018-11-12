package org.yop.orm.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.sql.Config;

/**
 * Aggregate some common stuff for the model tests classes.
 */
public abstract class Persistent implements Yopable {

	@Id(sequence = Config.SQL_DEFAULT_SEQ_DEFAULT)
	@Column(name = "id")
	private Long id;

	@Override
	public Long getId() {
		return this.id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		return o != null && o instanceof Yopable && this.equals((Yopable) o);
	}

	@Override
	public int hashCode() {
		return Yopable.hashCode(this);
	}
}
