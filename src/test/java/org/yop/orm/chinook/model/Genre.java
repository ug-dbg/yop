package org.yop.orm.chinook.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.util.HashSet;
import java.util.Set;

@Table(name = "genre")
public class Genre extends Persistent {

	@NaturalId
	@Column(name = "name")
	private String name;

	@JoinTable(
		table = "rel_track_genre",
		sourceColumn = "id_genre",
		targetColumn = "id_track"
	)
	private transient Set<Track> tracksOfGenre = new HashSet<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Track> getTracksOfGenre() {
		return tracksOfGenre;
	}
}
