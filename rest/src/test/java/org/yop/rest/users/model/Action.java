package org.yop.rest.users.model;

import org.yop.orm.annotations.*;
import org.yop.orm.model.Yopable;
import org.yop.rest.annotations.Rest;

import java.util.ArrayList;
import java.util.Collection;

@Rest(path = "action", description = "Actions REST resource ")
@Table(name = "action")
public class Action implements Yopable {

	@Id
	@Column(name = "id")
	private Long id;

	@NaturalId
	@Column(name = "name")
	private String name;

	@Column(name = "description", length = 255)
	private String description;

	@JoinTable(table = "rel_profile_action", sourceColumn = "action_id", targetColumn = "profile_id")
	private transient Collection<Profile> profilesForAction = new ArrayList<>();
}
