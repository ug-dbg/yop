package org.yop.orm.chinook.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.util.HashSet;
import java.util.Set;

@Table(name = "artist")
public class Artist extends Persistent {

	@Column(name = "name", length = 200)
	private String name;

	@JoinTable(
		table = "rel_album_artist",
		sourceColumn = "id_artist",
		targetColumn = "id_album"
	)
	private Set<Album> albums = new HashSet<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Album> getAlbums() {
		return this.albums;
	}
}
