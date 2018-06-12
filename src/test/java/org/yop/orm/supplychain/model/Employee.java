package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

@Table(name = "supplychain_employee")
public class Employee extends Persistent {

	public enum Role {CEO, CTO, GURU, NO_IDEA}

	@Column(name = "name")
	private String name;

	@Column(name = "role", enum_strategy = Column.EnumStrategy.NAME)
	private Role role;

	@Column(name = "active")
	private boolean active;

	@JoinTable(
		table = "rel_employee_organisation",
		sourceColumn = "id_employee",
		targetColumn = "id_organisation"
	)
	private transient Organisation organisation;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Organisation getOrganisation() {
		return organisation;
	}

	public void setOrganisation(Organisation organisation) {
		this.organisation = organisation;
	}
}
