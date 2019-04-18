package org.yop.rest.users.model;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.yop.orm.annotations.*;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.serialize.json.annotations.YopJSONTransient;
import org.yop.orm.query.sql.Select;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.annotations.Content;
import org.yop.rest.annotations.RequestPath;
import org.yop.rest.annotations.Rest;
import org.yop.rest.servlet.RestResponse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * A user whose natural key is the email.
 * <br>
 * An user can have several {@link Profile}, which contains the {@link Action} it is allowed to do.
 */
@Rest(path = "user", description = "Users REST resource ")
@Table(name = "yop_user")
public class User implements Yopable {

	@Id(sequence = "seq_user")
	@Column(name = "id")
	private Long id;

	@Column(name = "name")
	private String name;

	@YopJSONTransient
	@Column(name = "password_hash")
	private String passwordHash;

	@NaturalId
	@Column(name = "email")
	private String email;

	@Column(name = "birth_date")
	private LocalDate birthDate;

	@Column(name = "another_date")
	private Date anotherDate;

	@JoinTable(table = "rel_user_profile", sourceColumn = "user_id", targetColumn = "profile_id")
	private List<Profile> profiles = new ArrayList<>();

	@Rest(
		path = "",
		methods = "DELETE",
		description = "Delete users and returned deleted entries. Not implemented yet !",
		summary = "Delete and return users."
	)
	public static Collection<User> delete(
		IConnection connection,
		Header[] headers,
		@Content String content,
		@RequestPath String path,
		NameValuePair[] parameters) {

		throw new UnsupportedOperationException("Not implemented yet !");
	}

	@Rest(path = "all", description = "Get all users with profiles and actions !", summary = "Get all users.")
	public static RestResponse<User> getAll(IConnection connection) {
		Select<User> select = Select.from(User.class).join(User::getProfiles, Profile::getActionsForProfile);
		RestResponse<User> output = RestResponse.build(User.class, select.execute(connection));
		output.getJoins().addAll(select.getJoins());
		return output;
	}

	@Override
	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public void setAnotherDate(Date anotherDate) {
		this.anotherDate = anotherDate;
	}

	public String getEmail() {
		return this.email;
	}

	public LocalDate getBirthDate() {
		return this.birthDate;
	}

	public Date getAnotherDate() {
		return this.anotherDate;
	}

	public String getPasswordHash() {
		return this.passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public List<Profile> getProfiles() {
		return this.profiles;
	}
}
