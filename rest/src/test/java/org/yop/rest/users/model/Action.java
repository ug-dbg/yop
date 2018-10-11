package org.yop.rest.users.model;

import org.yop.orm.annotations.*;
import org.yop.orm.model.Yopable;
import org.yop.rest.annotations.Rest;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Any specific action in an application.
 * <br>
 * An action can be in one or several {@link Profile}.
 */
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

	@Override
	public Long getId() {
		return this.id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Collection<Profile> getProfilesForAction() {
		return this.profilesForAction;
	}

	public void setProfilesForAction(Collection<Profile> profilesForAction) {
		this.profilesForAction = profilesForAction;
	}
}
