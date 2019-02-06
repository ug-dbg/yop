package org.yop.orm.simple.model;

import org.yop.orm.annotations.*;
import org.yop.orm.model.Yopable;

@Table(name = "cycle_pojo")
public class CyclePojo implements Yopable {

	@Id(sequence = "seq_Cycle_POJO")
	@Column(name = "id")
	private Long id;

	@JoinColumn(local = "parent_id")
	private CyclePojo parent;

	@JoinColumn(local = "jopo_id")
	private CycleJopo jopo;

	@Column(name = "string")
	private String string = "pojo";

	@Override
	public Long getId() {
		return this.id;
	}

	public CyclePojo getParent() {
		return this.parent;
	}

	public CycleJopo getJopo() {
		return this.jopo;
	}

	public void setParent(CyclePojo parent) {
		this.parent = parent;
	}

	public void setJopo(CycleJopo jopo) {
		this.jopo = jopo;
	}

	@Table(name = "cycle_jopo")
	public static class CycleJopo implements Yopable {
		@Id(sequence = "seq_Cycle_JOPO")
		@Column(name = "id")
		private Long id;

		@JoinColumn(local = "pojo_id")
		private CyclePojo pojo;

		@Column(name = "string")
		private String string = "jopo";

		@Override
		public Long getId() {
			return this.id;
		}

		public CyclePojo getPojo() {
			return this.pojo;
		}

		public void setPojo(CyclePojo pojo) {
			this.pojo = pojo;
		}

		public String getString() {
			return this.string;
		}

		public void setString(String string) {
			this.string = string;
		}
	}
}