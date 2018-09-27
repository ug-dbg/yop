package org.yop.rest.users.model;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.annotations.ContentParam;
import org.yop.rest.annotations.PathParam;
import org.yop.rest.annotations.Rest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Rest(path = "user", description = "Users REST resource ")
@Table(name = "user")
public class User implements Yopable {

	@Id
	@Column(name = "id")
	private Long id;

	@Column(name = "name")
	private String name;

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
		@ContentParam String content,
		@PathParam String path,
		NameValuePair[] parameters) {

		throw new UnsupportedOperationException("Not implemented yet !");
	}

	@Override
	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
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

	public List<Profile> getProfiles() {
		return this.profiles;
	}
}
