package org.yop.orm.chinook.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;
import org.yop.orm.transform.AbbreviateTransformer;

import java.util.HashSet;
import java.util.Set;

@Table(name = "Track")
public class Track extends Persistent {

	@Column(name = "name", length = 200)
	private String name;

	@Column(
		name = "composer", length = 100,
		length_strategy = Column.LengthStrategy.CUT,
		transformer = AbbreviateTransformer.class
	)
	private String composer;

	@Column(name = "length_milliseconds")
	private long milliseconds;

	@Column(name = "bytes")
	private long bytes;

	@Column(name = "unit_price")
	private float unitPrice;

	@JoinTable(
		table = "rel_track_media_type",
		sourceColumn = "id_track",
		targetColumn = "id_media_type"
	)
	private MediaType mediaType;

	@JoinTable(
		table = "rel_track_genre",
		sourceColumn = "id_track",
		targetColumn = "id_genre"
	)
	private Genre genre;

	@JoinTable(
		table = "rel_track_album",
		sourceColumn = "id_track",
		targetColumn = "id_album"
	)
	private transient Album album;

	@JoinTable(
		table = "rel_playlist_track",
		sourceColumn = "id_track",
		targetColumn = "id_playlist"
	)
	private transient Set<Playlist> inPlaylists = new HashSet<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getComposer() {
		return composer;
	}

	public void setComposer(String composer) {
		this.composer = composer;
	}

	public long getMilliseconds() {
		return milliseconds;
	}

	public void setMilliseconds(long milliseconds) {
		this.milliseconds = milliseconds;
	}

	public long getBytes() {
		return bytes;
	}

	public void setBytes(long bytes) {
		this.bytes = bytes;
	}

	public float getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(float unitPrice) {
		this.unitPrice = unitPrice;
	}

	public MediaType getMediaType() {
		return mediaType;
	}

	public void setMediaType(MediaType mediaType) {
		this.mediaType = mediaType;
	}

	public Genre getGenre() {
		return genre;
	}

	public void setGenre(Genre genre) {
		this.genre = genre;
	}

	public Album getAlbum() {
		return album;
	}

	public void setAlbum(Album album) {
		this.album = album;
	}

	public Set<Playlist> getInPlaylists() {
		return inPlaylists;
	}
}
