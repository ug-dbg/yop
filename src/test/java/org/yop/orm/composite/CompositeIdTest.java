package org.yop.orm.composite;

import org.junit.Test;
import org.yop.orm.DBMSSwitch;
import org.yop.orm.composite.model.Book;
import org.yop.orm.model.ID;
import org.yop.orm.query.sql.Delete;
import org.yop.orm.query.sql.Select;
import org.yop.orm.query.sql.Upsert;
import org.yop.orm.sql.adapter.IConnection;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Set;

public class CompositeIdTest extends DBMSSwitch {
	@Override
	protected String getPackageNames() {
		return "org.yop.orm.composite.model";
	}

	@Test
	public void testCRUD() throws SQLException, ClassNotFoundException {
		try (IConnection connection = this.getConnection()){
			Book book = new Book("UG sa vie, son oeuvre.", "Ug");
			book.setPublishDate(LocalDate.parse("2017-11-13"));
			Upsert.from(Book.class).onto(book).execute(connection);

			book.setPublishDate(LocalDate.parse("1999-12-31"));
			Upsert.from(Book.class).onto(book).checkNaturalID().execute(connection);

			Set<Book> books = Select.from(Book.class).execute(connection);
			System.out.println(books);

			Delete.from(Book.class).whereId(books.stream().map(ID::id).toArray(Comparable[]::new)).executeQueries(connection);

			books = Select.from(Book.class).execute(connection);
			System.out.println(books.size());
		}
	}
}
