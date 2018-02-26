package org.yop.orm.example;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.model.json.YopableJson;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

public class Other implements YopableJson {

	@Column(name = "ID")
	private Long id;

	@NaturalId
	@Column(name = "NAME")
	private String name;

	@NaturalId
	@Column(name = "TIMESTAMP")
	private Timestamp timestamp;

	@JoinTable(table = "POJO_OTHER_relation", sourceColumn = "idOther", targetColumn = "idPojo")
	private Set<Pojo> pojos = new HashSet<>();

	public Set<Pojo> getPojos() {
		return pojos;
	}
}
