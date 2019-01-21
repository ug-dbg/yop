package org.yop.orm.simple.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinColumn;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Yopable;

@Table(name = "bad_cycle_pojo")
public class BadCyclePojo implements Yopable {

	@Id(sequence = "seq_Bad_Cycle_POJO")
	@Column(name = "id")
	private Long id;

	@JoinColumn(local = "parent_id")
	private BadCyclePojo parent;
}
