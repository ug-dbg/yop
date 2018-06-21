package org.yop.orm.simple.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinColumn;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Yopable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

@Table(name = "simple_superextra")
public class SuperExtra implements Yopable {

	@Id(sequence = "seq_SUPER_EXTRA")
	@Column(name = "id")
	private Long id;

	@Column(name = "extra_size")
	private Long size;

	@JoinColumn(remote = "id_super_extra")
	private Collection<Extra> extras = new ArrayList<>();

	public Collection<Extra> getExtras() {
		return extras;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SuperExtra that = (SuperExtra) o;
		return Objects.equals(size, that.size);
	}

	@Override
	public int hashCode() {
		return Objects.hash(size);
	}
}
