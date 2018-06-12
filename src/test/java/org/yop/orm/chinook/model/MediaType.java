package org.yop.orm.chinook.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;

import java.util.HashSet;
import java.util.Set;

@Table(name = "chinook_mediatype")
public class MediaType extends Persistent {

	@Column(name = "name")
	private String name;

	@JoinTable(
		table = "rel_track_media_type",
		sourceColumn = "id_media_type",
		targetColumn = "id_track"
	)
	private transient Set<Track> tracksOfType = new HashSet<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Track> getTracksOfType() {
		return tracksOfType;
	}
}
