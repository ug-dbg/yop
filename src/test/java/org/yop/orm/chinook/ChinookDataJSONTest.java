package org.yop.orm.chinook;

import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.chinook.model.*;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.query.Join;
import org.yop.orm.query.JoinSet;
import org.yop.orm.query.serialize.json.JSON;

import javax.xml.bind.JAXBException;
import java.io.IOException;

/**
 * Some tests using the <a href="https://archive.codeplex.com/?p=chinookdatabase">chinook data sample</a>.
 * <br>
 * These tests simply rely on the {@link JSON} API and require no underlying database.
 */
public class ChinookDataJSONTest {

	private static final Logger logger = LoggerFactory.getLogger(ChinookDataTest.class);

	private static ChinookData source;

	@BeforeClass
	public static void setUp() {
		try {
			source = ChinookData.readData();
		} catch (JAXBException e) {
			logger.error("Could not load Chinook Data set !", e);
			throw new YopRuntimeException("Could not load Chinook Data set !", e);
		}
	}

	@Test
	public void test_single_artist_to_json() throws IOException, JSONException {
		String json = JSON
			.from(Artist.class)
			.joinAll()
			.joinIDsAll()
			.toJSON(source.artists.get(1));
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/chinook/json/test_single_artist_to_json_expected.json")
		);
		JSONAssert.assertEquals(expected, json, true);
	}

	@Test
	public void test_single_artist_to_json_pretty_print() throws IOException {
		String json = JSON
			.from(Artist.class)
			.withBuilder(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping())
			.joinAll()
			.joinIDsAll()
			.toJSON(source.artists.get(1));
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/chinook/json/test_single_artist_to_json_expected.json")
		);
		Assert.assertEquals(expected, json);
	}

	@Test
	public void test_artists_to_json() throws IOException, JSONException {
		String json = JSON
			.from(Artist.class)
			.joinAll()
			.joinIDsAll()
			.onto(source.artists.values())
			.toJSON();
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/chinook/json/test_artists_to_json_expected.json")
		);
		JSONAssert.assertEquals(expected, json, true);
	}

	@Test
	public void test_albums_to_json() throws IOException, JSONException {
		String json = JSON
			.from(Album.class)
			.join((JoinSet.to(Album::getTracks)))
			.joinIDs(JoinSet.to(Album::getTracks).join(Join.to(Track::getGenre)))
			.onto(source.albums.values())
			.toJSON();
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/chinook/json/test_albums_to_json_expected.json")
		);
		JSONAssert.assertEquals(expected, json, true);
	}

	@Test
	public void test_genres_to_json() throws IOException, JSONException {
		String json = JSON
			.from(Genre.class)
			.join((JoinSet.to(Genre::getTracksOfGenre)))
			.joinIDs((JoinSet.to(Genre::getTracksOfGenre).join(Join.to(Track::getAlbum))))
			.onto(source.genres.values())
			.toJSON();
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/chinook/json/test_genres_to_json_expected.json")
		);
		JSONAssert.assertEquals(expected, json, true);
	}

	@Test
	public void test_invoices_to_json() throws IOException, JSONException {
		String json = JSON
			.from(Invoice.class)
			.join(JoinSet.to(Invoice::getLines))
			.onto(source.invoices.values())
			.toJSON();
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/chinook/json/test_invoices_to_json_expected.json")
		);
		JSONAssert.assertEquals(expected, json, true);
	}

	@Test
	public void test_invoice_lines_to_json() throws IOException, JSONException {
		String json = JSON
			.from(InvoiceLine.class)
			.joinIDs(Join.to(InvoiceLine::getInvoice))
			.joinIDs(Join.to(InvoiceLine::getTrack))
			.onto(source.invoiceLines.values())
			.toJSON();
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/chinook/json/test_invoice_lines_to_json_expected.json")
		);
		JSONAssert.assertEquals(expected, json, true);
	}

	@Test
	public void test_media_types_to_json() throws IOException, JSONException {
		String json = JSON
			.from(MediaType.class)
			.joinIDs(JoinSet.to(MediaType::getTracksOfType))
			.onto(source.mediaTypes.values())
			.toJSON();
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/chinook/json/test_media_types_to_json_expected.json")
		);
		JSONAssert.assertEquals(expected, json, true);
	}

	@Test
	public void test_playlists_to_json() throws IOException, JSONException {
		String json = JSON
			.from(Playlist.class)
			.joinAll()
			.joinIDs(JoinSet.to(Playlist::getTracks).join(Join.to(Track::getAlbum)))
			.onto(source.playlists.values())
			.toJSON();
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/chinook/json/test_playlists_to_json_expected.json")
		);
		JSONAssert.assertEquals(expected, json, true);
	}

	@Test
	public void test_employees_to_json() throws IOException, JSONException {
		String json = JSON
			.from(Employee.class)
			.joinAll()
			.joinIDs(Join.to(Employee::getReportsTo))
			.joinIDs(JoinSet.to(Employee::getReporters))
			.onto(source.employees.values())
			.toJSON();
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/chinook/json/test_employees_to_json_expected.json")
		);
		JSONAssert.assertEquals(expected, json, true);
	}

	@Test
	public void test_customers_to_json() throws IOException, JSONException {
		String json = JSON
			.from(Customer.class)
			.join(JoinSet.to(Customer::getInvoices).join(JoinSet.to(Invoice::getLines)))
			.joinIDs(JoinSet.to(Customer::getInvoices).join(JoinSet.to(Invoice::getLines).join(Join.to(InvoiceLine::getTrack))))
			.onto(source.customers.values())
			.toJSON();
		String expected = IOUtils.toString(
			this.getClass().getResourceAsStream("/chinook/json/test_customers_to_json_expected.json")
		);
		JSONAssert.assertEquals(expected, json, true);
	}
}
