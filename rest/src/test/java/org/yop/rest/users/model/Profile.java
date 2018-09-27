package org.yop.rest.users.model;

import org.yop.orm.annotations.*;
import org.yop.orm.model.Yopable;
import org.yop.rest.annotations.Rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Rest(path = "profile", description = "Profiles REST resource ")
@Table(name = "profile")
public class Profile implements Yopable {

	@Id
	@Column(name = "id")
	private Long id;

	@NaturalId
	@Column(name = "name")
	private String name;

	@JoinTable(table = "rel_user_profile", sourceColumn = "profile_id", targetColumn = "user_id")
	private transient List<User> usersWithProfile = new ArrayList<>();

	@JoinTable(table = "rel_profile_action", sourceColumn = "profile_id", targetColumn = "action_id")
	private Collection<Action> actionsForProfile = new ArrayList<>();

	@Override
	public boolean equals(Object o) {
		return o instanceof Yopable && Yopable.super.equals((Yopable) o);
	}

	@Override
	public int hashCode() {
		return Yopable.hashCode(this);
	}
}
