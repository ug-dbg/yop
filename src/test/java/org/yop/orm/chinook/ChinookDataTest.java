package org.yop.orm.chinook;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.annotations.LongTest;
import org.yop.orm.chinook.model.*;
import org.yop.orm.chinook.model.xml.ChinookDataSet;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.*;
import org.yop.orm.query.batch.BatchUpsert;
import org.yop.orm.util.Reflection;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Some tests using the <a href="https://archive.codeplex.com/?p=chinookdatabase">chinook data sample</a>.
 * I am so not good at testing I try using the data from somebody else :-D
 */
@LongTest
public class ChinookDataTest extends DBMSSwitch {

	private static final Logger logger = LoggerFactory.getLogger(ChinookDataTest.class);
	private static final String CHINOOK_RESOURCE = "/chinook/data_sample/ChinookData.xml";

	@Override
	protected String getPackagePrefix() {
		return "org.yop.orm.chinook.model";
	}

	@Test
	public void test() throws JAXBException, SQLException, ClassNotFoundException {
		if (!this.check()) {
			logger.warn("The test class [{}] won't be run.", this.getClass().getName());
			return;
		}

		ChinookData source = this.readData();

		// Batch insert the data !
		// This can be preeeeetty long with SQLite
		long start = System.currentTimeMillis();
		BatchUpsert.from(Artist.class).onto(source.artists.values()).joinAll().execute(this.getConnection());
		logger.info("Batch upsert in [" + (System.currentTimeMillis() - start + "] ms"));

		// Get all the data from the DB
		start = System.currentTimeMillis();
		Set<Artist> artistsFromDB = Select.from(Artist.class).joinAll().execute(this.getConnection());
		logger.info(
			"Recovered [{}] artists and joined data in [{}] ms",
			artistsFromDB.size(),
			(System.currentTimeMillis() - start)
		);
		Assert.assertEquals(source.artists.size(), artistsFromDB.size());

		// Order by test !
		Set<Track> orderedTracks = Select
			.from(Track.class)
			.joinAll()
			.orderBy(OrderBy.orderBy(Track::getName, true).thenBy(Track::getBytes, false))
			.execute(this.getConnection());

		// Order checking on strings is quite difficult : special characters, spaces, case, numerics...
		// A Collator might not exactly match the data order from the DB...
		// Just check on ascii names with no spaces :-D
		Track last = null;
		for (Track next : orderedTracks) {
			if (last != null
			&& StringUtils.isAlpha(last.getName())
			&& StringUtils.isAlpha(next.getName())
			&& StringUtils.isAsciiPrintable(last.getName())
			&& StringUtils.isAsciiPrintable(next.getName())) {
				Assert.assertTrue(
					"[" + next.getName() + "] is not >= to [" + last.getName() + "]",
					StringUtils.compareIgnoreCase(next.getName(), last.getName()) >= 0
				);
				if (StringUtils.equals(next.getName(), last.getName())) {
					Assert.assertTrue(
						"[" + next.getBytes() + "] is not <= to [" + last.getBytes() + "]",
						next.getBytes() <= last.getBytes()
					);
				}
			}
			last = next;
		}

		// Insert all the employees data in the DB
		// We use a batch insert : the natural keys will be checked and therefore no exception should occur.
		start = System.currentTimeMillis();
		BatchUpsert
			.from(Employee.class)
			.onto(source.employees.values())
			.join(Join.to(Employee::getReportsTo))
			.execute(this.getConnection());
		logger.info("Upsert in [" + (System.currentTimeMillis() - start + "] ms"));

		// Employee data check : fetch 'reports to'
		Set<Employee> employeesFromDB = Select
			.from(Employee.class)
			.join(Join.to(Employee::getReportsTo))
			.execute(this.getConnection());
		Assert.assertEquals(source.employees.size(), employeesFromDB.size());

		Map<String, Employee> employeesByMail = employeesFromDB
			.stream()
			.collect(Collectors.toMap(Employee::getEmail, Function.identity()));

		Assert.assertEquals(
			employeesByMail.get("andrew@chinookcorp.com"),
			employeesByMail.get("nancy@chinookcorp.com").getReportsTo()
		);

		// Employee data check : fetch 'reporters'
		employeesFromDB = Select
			.from(Employee.class)
			.join(JoinSet.to(Employee::getReporters))
			.execute(this.getConnection());
		Assert.assertEquals(source.employees.size(), employeesFromDB.size());

		employeesByMail = employeesFromDB
			.stream()
			.collect(Collectors.toMap(Employee::getEmail, Function.identity()));

		Assert.assertEquals(
			new HashSet<>(Arrays.asList("margaret@chinookcorp.com", "jane@chinookcorp.com", "steve@chinookcorp.com")),
			employeesByMail
				.get("nancy@chinookcorp.com")
				.getReporters()
				.stream()
				.map(Employee::getEmail)
				.collect(Collectors.toSet())
		);

		// Employee data check : does not fetch reporters/reports_to, but use Recurse
		employeesFromDB = Select
			.from(Employee.class)
			.execute(this.getConnection());
		Assert.assertEquals(source.employees.size(), employeesFromDB.size());
		Recurse
			.from(Employee.class)
			.onto(employeesFromDB)
			.join(Join.to(Employee::getReportsTo))
			.execute(this.getConnection());

		employeesByMail = employeesFromDB
			.stream()
			.collect(Collectors.toMap(Employee::getEmail, Function.identity()));

		Assert.assertEquals(
			"nancy@chinookcorp.com",
			employeesByMail.get("margaret@chinookcorp.com").getReportsTo().getEmail()
		);

		// Jane now reports to herself (what a promotion)
		// Update, fetch, recurse and check
		Employee jane = employeesByMail.get("jane@chinookcorp.com");
		jane.setReportsTo(jane);
		Upsert.from(Employee.class).onto(jane).join(Join.to(Employee::getReportsTo)).execute(this.getConnection());
		jane = Select.from(Employee.class).where(Where.naturalId(jane)).uniqueResult(this.getConnection());
		Recurse
			.from(Employee.class)
			.onto(jane)
			.join(Join.to(Employee::getReportsTo))
			.execute(this.getConnection());
		Recurse
			.from(Employee.class)
			.onto(employeesFromDB)
			.join(JoinSet.to(Employee::getReporters))
			.execute(this.getConnection());

		employeesByMail = employeesFromDB
			.stream()
			.collect(Collectors.toMap(Employee::getEmail, Function.identity()));
		Assert.assertEquals(
			"jane@chinookcorp.com",
			employeesByMail.get("jane@chinookcorp.com").getReportsTo().getEmail()
		);
		Assert.assertEquals("jane@chinookcorp.com", jane.getReportsTo().getEmail());

		// Jane should not be in Nancy's reporters list anymore
		Assert.assertEquals(
			new HashSet<>(Arrays.asList("margaret@chinookcorp.com", "steve@chinookcorp.com")),
			employeesByMail
				.get("nancy@chinookcorp.com")
				.getReporters()
				.stream()
				.map(Employee::getEmail)
				.collect(Collectors.toSet())
		);

		// A nasty cycle !
		Employee andrew = employeesByMail.get("andrew@chinookcorp.com");
		andrew.setReportsTo(jane);
		Upsert.from(Employee.class).onto(andrew).join(Join.to(Employee::getReportsTo)).execute(this.getConnection());

		jane = Select.from(Employee.class).where(Where.naturalId(jane)).uniqueResult(this.getConnection());

		Recurse
			.from(Employee.class)
			.onto(jane)
			.join(JoinSet.to(Employee::getReporters))
			.join(Join.to(Employee::getReportsTo))
			.execute(this.getConnection());
		Assert.assertTrue(jane.getReporters().contains(andrew));

		// "Order by" test, on 2 different fields !
		// I know, this is pretty insane. Don't let all that craziness go to your head.
		Set<Employee> employees = Select
			.from(Employee.class)
			.joinAll()
			.orderBy(OrderBy.orderBy(Employee::getTitle, true).thenBy(Employee::getBirthDate, true))
			.execute(this.getConnection());
		List<String> orderedMails = employees.stream().map(Employee::getEmail).collect(Collectors.toList());

		Assert.assertEquals(
			Arrays.asList(
				"andrew@chinookcorp.com",
				"michael@chinookcorp.com",
				"laura@chinookcorp.com",
				"robert@chinookcorp.com",
				"nancy@chinookcorp.com",
				"margaret@chinookcorp.com",
				"steve@chinookcorp.com",
				"jane@chinookcorp.com"
			),
			orderedMails
		);

		employees = Select
			.from(Employee.class)
			.joinAll()
			.orderBy(OrderBy.orderBy(Employee::getTitle, true).thenBy(Employee::getBirthDate, false))
			.execute(this.getConnection());
		orderedMails = employees.stream().map(Employee::getEmail).collect(Collectors.toList());

		Assert.assertEquals(
			Arrays.asList(
				"andrew@chinookcorp.com",
				"michael@chinookcorp.com",
				"robert@chinookcorp.com",
				"laura@chinookcorp.com",
				"nancy@chinookcorp.com",
				"jane@chinookcorp.com",
				"steve@chinookcorp.com",
				"margaret@chinookcorp.com"
			),
			orderedMails
		);
	}

	/**
	 * Read the 'chinook' data source (XML file) containing albums, tracks, customers, employees...
	 * @return the data from {@link #CHINOOK_RESOURCE}
	 * @throws JAXBException could not unmarshall the chinook data
	 */
	private ChinookData readData() throws JAXBException {
		InputStream dataStream = this.getClass().getResourceAsStream(CHINOOK_RESOURCE);

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

		return data.cleanIds();
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
				targetField.set(to, field.get(from));
			} catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
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

	/**
	 * The data from the Chinook data set as several maps, one for each type.
	 * Key of the data maps is the ID.
	 */
	private static class ChinookData {
		private Map<Integer, Album> albums = new HashMap<>();
		private Map<Integer, Artist> artists = new HashMap<>();
		private Map<Integer, Customer> customers = new HashMap<>();
		private Map<Integer, Employee> employees = new HashMap<>();
		private Map<Integer, Genre> genres = new HashMap<>();
		private Map<Integer, Invoice> invoices = new HashMap<>();
		private Map<Integer, InvoiceLine> invoiceLines = new HashMap<>();
		private Map<Integer, MediaType> mediaTypes = new HashMap<>();
		private Map<Integer, Playlist> playlists = new HashMap<>();
		private Map<Integer, Track> tracks = new HashMap<>();

		/**
		 * Clean all the ids of the data maps and return the current object
		 * @return the current object after all the IDs are cleaned
		 */
		private ChinookData cleanIds() {
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
	}
}
