package org.yop.orm.chinook.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;
import org.yop.orm.model.json.YopableJson;
import org.yop.orm.transform.AbbreviateTransformer;

import java.util.HashSet;
import java.util.Set;

@Table(name = "album")
public class Album extends Persistent implements YopableJson {

	@Column(
		name = "title",
		length_strategy = Column.LengthStrategy.CUT,
		transformer = AbbreviateTransformer.class
	)
	private String title;

	@JoinTable(
		table = "rel_album_artist",
		sourceColumn = "id_album",
		targetColumn = "id_artist"
	)
	private transient Artist artist;

	@JoinTable(
		table = "rel_track_album",
		sourceColumn = "id_album",
		targetColumn = "id_track"
	)
	private Set<Track> tracks = new HashSet<>();

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Artist getArtist() {
		return artist;
	}

	public void setArtist(Artist artist) {
		this.artist = artist;
	}

	public Set<Track> getTracks() {
		return tracks;
	}
}
