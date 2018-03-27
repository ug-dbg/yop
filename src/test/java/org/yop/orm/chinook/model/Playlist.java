package org.yop.orm.chinook.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;
import org.yop.orm.model.json.YopableJson;

import java.util.HashSet;
import java.util.Set;

@Table(name = "playlist")
public class Playlist extends Persistent implements YopableJson {

	@Column(name = "name")
	private String name;

	@JoinTable(
		table = "rel_playlist_track",
		sourceColumn = "id_playlist",
		targetColumn = "id_track"
	)
	private Set<Track> tracks = new HashSet<>();

	public Set<Track> getTracks() {
		return this.tracks;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
