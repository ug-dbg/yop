package org.yop.orm.chinook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.chinook.model.*;
import org.yop.orm.chinook.model.xml.ChinookDataSet;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.Reflection;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

class ChinookData {

	private static final Logger logger = LoggerFactory.getLogger(ChinookData.class);

	private static final String CHINOOK_RESOURCE = "/chinook/data_sample/ChinookData.xml";

	Map<Integer, Album>       albums = new HashMap<>();
	Map<Integer, Artist>      artists = new HashMap<>();
	Map<Integer, Customer>    customers = new HashMap<>();
	Map<Integer, Employee>    employees = new HashMap<>();
	Map<Integer, Genre>       genres = new HashMap<>();
	Map<Integer, Invoice>     invoices = new HashMap<>();
	Map<Integer, InvoiceLine> invoiceLines = new HashMap<>();
	Map<Integer, MediaType>   mediaTypes = new HashMap<>();
	Map<Integer, Playlist>    playlists = new HashMap<>();
	Map<Integer, Track>       tracks = new HashMap<>();

	/**
	 * Clean all the ids of the data maps and return the current object
	 * @return the current object after all the IDs are cleaned
	 */
	ChinookData cleanIds() {
		cleanIds(this.albums.values());
		cleanIds(this.artists.values());
		cleanIds(this.tracks.values());
		cleanIds(this.playlists.values());
		cleanIds(this.customers.values());
		cleanIds(this.invoiceLines.values());
		cleanIds(this.invoices.values());
		cleanIds(this.employees.values());
		cleanIds(this.mediaTypes.values());
		cleanIds(this.genres.values());
		return this;
	}

	/**
	 * Clean all the IDs of a given Yopable list (set them to null)
	 * @param yopables the list whose IDs are to be cleaned
	 */
	private static void cleanIds(Collection<? extends Yopable> yopables) {
		yopables.forEach(y -> y.setId(null));
	}

	/**
	 * Read the 'chinook' data source (XML file) containing albums, tracks, customers, employees...
	 * @return the data from {@link #CHINOOK_RESOURCE}
	 * @throws JAXBException could not unmarshall the chinook data
	 */
	static ChinookData readData() throws JAXBException {
		InputStream dataStream = ChinookData.class.getResourceAsStream(CHINOOK_RESOURCE);

		JAXBContext jaxbContext = JAXBContext.newInstance(ChinookDataSet.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		ChinookDataSet model = (ChinookDataSet) jaxbUnmarshaller.unmarshal(dataStream);
		ChinookData data = new ChinookData();

		for (Object o : model.getGenreOrMediaTypeOrArtist()) {
			if(o instanceof ChinookDataSet.Album) {
				ChinookDataSet.Album casted = (ChinookDataSet.Album) o;
				Album album = getOrPut(data.albums, casted.getAlbumId(), Album.class);
				naiveMap(o, album);
				album.setArtist(getOrPut(data.artists, casted.getArtistId(), Artist.class));
				album.getArtist().getAlbums().add(album);
			}

			if(o instanceof ChinookDataSet.Artist) {
				int artistId = ((ChinookDataSet.Artist) o).getArtistId();
				Artist artist;
				if(data.artists.containsKey(artistId)) {
					artist = data.artists.get(artistId);
				} else {
					artist = new Artist();
					artist.setId((long) artistId);
					data.artists.put(artistId, artist);
				}
				naiveMap(o, artist);
			}

			if(o instanceof ChinookDataSet.Customer) {
				int customerId = ((ChinookDataSet.Customer) o).getCustomerId();
				Customer customer;
				if(data.customers.containsKey(customerId)) {
					customer = data.customers.get(customerId);
				} else {
					customer = new Customer();
					customer.setId((long) customerId);
					data.customers.put(customerId, customer);
				}
				naiveMap(o, customer);
			}

			if(o instanceof ChinookDataSet.Employee) {
				ChinookDataSet.Employee casted = (ChinookDataSet.Employee) o;
				Employee employee = getOrPut(data.employees, (casted).getEmployeeId(), Employee.class);
				naiveMap(o, employee);
				employee.setBirthDate(toDate((casted).getBirthDate().toGregorianCalendar()));
				employee.setHireDate(toDate((casted).getHireDate().toGregorianCalendar()));
				employee.setReportsTo(getOrPut(data.employees, (casted).getReportsTo(), Employee.class));
				if(employee.getReportsTo() != null) {
					employee.getReportsTo().getReporters().add(employee);
				}
			}

			if(o instanceof ChinookDataSet.Genre) {
				Genre genre = getOrPut(data.genres, ((ChinookDataSet.Genre) o).getGenreId(), Genre.class);
				naiveMap(o, genre);
			}

			if(o instanceof ChinookDataSet.Invoice) {
				ChinookDataSet.Invoice casted = (ChinookDataSet.Invoice) o;
				Invoice invoice = getOrPut(data.invoices, (casted).getInvoiceId(), Invoice.class);
				naiveMap(o, invoice);
				invoice.setInvoiceDate(toDateTime((casted).getInvoiceDate().toGregorianCalendar()));
				invoice.setCustomer(getOrPut(data.customers, (casted).getCustomerId(), Customer.class));
				invoice.getCustomer().getInvoices().add(invoice);
			}

			if(o instanceof ChinookDataSet.InvoiceLine) {
				ChinookDataSet.InvoiceLine casted = (ChinookDataSet.InvoiceLine) o;
				InvoiceLine line = getOrPut(data.invoiceLines, (casted).getInvoiceLineId(), InvoiceLine.class);
				naiveMap(o, line);
				line.setTrack(getOrPut(data.tracks, (casted).getTrackId(), Track.class));
				line.setUnitPrice((casted).getUnitPrice().floatValue());
				line.setInvoice(getOrPut(data.invoices, (casted).getInvoiceId(), Invoice.class));
				line.getInvoice().getLines().add(line);
			}

			if(o instanceof ChinookDataSet.MediaType) {
				ChinookDataSet.MediaType casted = (ChinookDataSet.MediaType) o;
				MediaType type = getOrPut(data.mediaTypes, (casted).getMediaTypeId(), MediaType.class);
				naiveMap(o, type);
			}

			if(o instanceof ChinookDataSet.Playlist) {
				ChinookDataSet.Playlist casted = (ChinookDataSet.Playlist) o;
				Playlist playlist = getOrPut(data.playlists, (casted).getPlaylistId(), Playlist.class);
				naiveMap(o, playlist);
			}

			if(o instanceof ChinookDataSet.PlaylistTrack) {
				ChinookDataSet.PlaylistTrack casted = (ChinookDataSet.PlaylistTrack) o;
				Playlist playlist = getOrPut(data.playlists, (casted).getPlaylistId(), Playlist.class);
				playlist.getTracks().add(getOrPut(data.tracks, (casted).getTrackId(), Track.class));
				for (Track track : playlist.getTracks()) {
					track.getInPlaylists().add(playlist);
				}
			}

			if(o instanceof ChinookDataSet.Track) {
				ChinookDataSet.Track casted = (ChinookDataSet.Track) o;
				Track track = getOrPut(data.tracks, (casted).getTrackId(), Track.class);
				naiveMap(o, track);
				track.setAlbum(getOrPut(data.albums, (casted).getAlbumId(), Album.class));
				track.getAlbum().getTracks().add(track);
				track.setGenre(getOrPut(data.genres, (casted).getGenreId(), Genre.class));
				track.getGenre().getTracksOfGenre().add(track);
				track.setMediaType(getOrPut(data.mediaTypes, (casted).getMediaTypeId(), MediaType.class));
				track.getMediaType().getTracksOfType().add(track);
			}
		}

		return data;
	}

	/**
	 * Try to map every field from 'from' to 'to' with the same field name.
	 * @param from the data source
	 * @param to   the target Yopable object
	 */
	private static void naiveMap(Object from, Yopable to) {
		List<Field> fields = Reflection.getFields(from.getClass(), true);
		for (Field field : fields) {
			try {
				String name = field.getName();
				Field targetField = to.getClass().getDeclaredField(name);

				field.setAccessible(true);
				targetField.setAccessible(true);
				Reflection.set(targetField, to, Reflection.readField(field, from));
			} catch (NoSuchFieldException | IllegalArgumentException | YopRuntimeException e) {
				logger.trace(
					"Could not map field [{}#{}] onto [{}]",
					field.getDeclaringClass(),
					field.getName(),
					to.getClass().getName()
				);
			}
		}
	}

	/**
	 * Get some data from the data map using the given ID.
	 * If no data, create a new one with the ID, add it to the map and return.
	 * @param data  the data map
	 * @param id    the target data ID
	 * @param clazz the target data class
	 * @param <T> the target data type
	 * @return the data from the data map, or a new instance with the given ID, after being added to the map.
	 */
	private static <T extends Yopable> T getOrPut(Map<Integer, T> data, Integer id, Class<T> clazz) {
		if(id == null) {
			return null;
		}
		if(data.containsKey(id)) {
			return data.get(id);
		}
		T newTarget = Reflection.newInstanceNoArgs(clazz);
		newTarget.setId(Long.valueOf(id));
		data.put(id, newTarget);
		return newTarget;
	}

	/**
	 * Convert a calendar to a local date
	 * @param calendar the calendar to convert
	 * @return the local date for the input calendar
	 */
	private static LocalDate toDate(Calendar calendar) {
		return calendar.toInstant().atZone(calendar.getTimeZone().toZoneId()).toLocalDate();
	}

	/**
	 * Convert a calendar to a local date/time
	 * @param calendar the calendar to convert
	 * @return the local date/time for the input calendar
	 */
	private static LocalDateTime toDateTime(Calendar calendar) {
		return calendar.toInstant().atZone(calendar.getTimeZone().toZoneId()).toLocalDateTime();
	}
}
