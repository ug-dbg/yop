package org.yop.orm.chinook;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.Yop;
import org.yop.orm.annotations.LongTest;
import org.yop.orm.chinook.model.Artist;
import org.yop.orm.chinook.model.Employee;
import org.yop.orm.chinook.model.Genre;
import org.yop.orm.chinook.model.Track;
import org.yop.orm.evaluation.Operator;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.query.*;
import org.yop.orm.query.batch.BatchUpsert;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.sql.SQLException;
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

	private ChinookData source;

	@Override
	protected String getPackageNames() {
		return "org.yop.orm.chinook.model";
	}

	@Override
	public void setUp() throws SQLException, IOException, ClassNotFoundException {
		super.setUp();

		try {
			this.source = ChinookData.readData().cleanIds();

			// Batch insert the data !
			// This can be preeeeetty long with SQLite
			long start = System.currentTimeMillis();
			Yop.batchUpsert(Artist.class).onto(this.source.artists.values()).joinAll().execute(this.getConnection());
			logger.info("Batch upsert Music in [" + (System.currentTimeMillis() - start + "] ms"));

			// Insert all the employees data in the DB
			// We use a batch insert : the natural keys will be checked when merging insert queries into a batch query
			// and therefore no exception should occur.
			start = System.currentTimeMillis();
			BatchUpsert
				.from(Employee.class)
				.onto(this.source.employees.values())
				.join(Join.to(Employee::getReportsTo))
				.execute(this.getConnection());
			logger.info("Batch upsert Employees in [" + (System.currentTimeMillis() - start + "] ms"));
		} catch (JAXBException e) {
			logger.error("Could not load Chinook Data set !", e);
			throw new YopRuntimeException("Could not load Chinook Data set !", e);
		}
	}

	@Test
	public void test_music_ordered_select() throws SQLException, ClassNotFoundException {
		// Get all the data from the DB
		long start = System.currentTimeMillis();
		Set<Artist> artistsFromDB = Select.from(Artist.class).joinAll().execute(this.getConnection());
		logger.info(
			"Recovered [{}] artists and joined data in [{}] ms",
			artistsFromDB.size(),
			(System.currentTimeMillis() - start)
		);
		Assert.assertEquals(this.source.artists.size(), artistsFromDB.size());

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
	}

	@Test
	public void test_music_recurse_on_genre() throws SQLException, ClassNotFoundException {
		// Recurse on genre → tracks → genre ...
		long start = System.currentTimeMillis();
		Set<Genre> genres = Select
			.from(Genre.class)
			.orderBy(OrderBy.orderBy(Genre::getName, false))
			.joinAll()
			.execute(this.getConnection());
		logger.info("Select genres in [{}] ms", (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		Recurse
			.from(Genre.class)
			.onto(genres)
			.join(JoinSet.to(Genre::getTracksOfGenre).join(Join.to(Track::getGenre)))
			.execute(this.getConnection());
		logger.info("Recurse on genres in [{}] ms", (System.currentTimeMillis() - start));

		for (Genre genre : genres) {
			Assert.assertTrue(genre.getTracksOfGenre().size() > 0);
			for (Track track : genre.getTracksOfGenre()) {
				Assert.assertEquals(genre, track.getGenre());
			}
		}
	}

	@Test
	public void test_music_delete_genres() throws SQLException, ClassNotFoundException {
		// Let's delete all these genres with a "/" in their name !
		long slashed = this.source.tracks
			.values()
			.stream()
			.filter(track -> track.getGenre().getName().contains("/"))
			.count();

		long start = System.currentTimeMillis();
		Delete
			.from(Genre.class)
			.where(Where.compare(Genre::getName, Operator.LIKE, "%/%"))
			.join(JoinSet.to(Genre::getTracksOfGenre))
			.executeQueries(this.getConnection());
		logger.info("Deleted '/' genres in [{}] ms", (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		Set<Track> tracks = Select.from(Track.class).join(Join.to(Track::getGenre)).execute(this.getConnection());
		logger.info("Found left tracks for non '/' genres in [{}] ms", (System.currentTimeMillis() - start));

		Assert.assertTrue(tracks.size() == this.source.tracks.size() - slashed);
		for (Track track : tracks) {
			Assert.assertTrue(track.getGenre() != null);
			Assert.assertFalse(track.getGenre().getName().contains("/"));
		}
	}

	@Test
	public void test_employees_fetch() throws SQLException, ClassNotFoundException {
		// Employee data check : fetch 'reports to'
		Set<Employee> employeesFromDB = Select
			.from(Employee.class)
			.join(Join.to(Employee::getReportsTo))
			.execute(this.getConnection());
		Assert.assertEquals(this.source.employees.size(), employeesFromDB.size());

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
		Assert.assertEquals(this.source.employees.size(), employeesFromDB.size());

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
	}

	@Test
	public void test_employees_fetch_order() throws SQLException, ClassNotFoundException {
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

	@Test
	public void test_employees_recurse() throws SQLException, ClassNotFoundException {
		// Employee data check : does not fetch reporters/reports_to, but use Recurse
		Set<Employee> employeesFromDB = Select
			.from(Employee.class)
			.execute(this.getConnection());
		Assert.assertEquals(this.source.employees.size(), employeesFromDB.size());
		Recurse
			.from(Employee.class)
			.onto(employeesFromDB)
			.join(Join.to(Employee::getReportsTo))
			.execute(this.getConnection());

		Map<String, Employee> employeesByMail = employeesFromDB
			.stream()
			.collect(Collectors.toMap(Employee::getEmail, Function.identity()));

		Assert.assertEquals(
			"nancy@chinookcorp.com",
			employeesByMail.get("margaret@chinookcorp.com").getReportsTo().getEmail()
		);
	}

	@Test
	public void test_employees_recurse_with_self_reference() throws SQLException, ClassNotFoundException {
		Set<Employee> employeesFromDB = Select
			.from(Employee.class)
			.execute(this.getConnection());

		Map<String, Employee> employeesByMail = employeesFromDB
			.stream()
			.collect(Collectors.toMap(Employee::getEmail, Function.identity()));

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
	}

	@Test
	public void test_employees_recurse_with_weird_cycle() throws SQLException, ClassNotFoundException {
		Set<Employee> employeesFromDB = Select
			.from(Employee.class)
			.execute(this.getConnection());

		Map<String, Employee> employeesByMail = employeesFromDB
			.stream()
			.collect(Collectors.toMap(Employee::getEmail, Function.identity()));

		Employee jane = employeesByMail.get("jane@chinookcorp.com");
		jane.setReportsTo(jane);
		Upsert.from(Employee.class).onto(jane).join(Join.to(Employee::getReportsTo)).execute(this.getConnection());
		jane = Select.from(Employee.class).where(Where.naturalId(jane)).uniqueResult(this.getConnection());

		// A weird cycle : andrew (CEO, top guy) reports to jane
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
	}
}
