import java.time.LocalDate;
import java.util.*;

import org.yop.orm.evaluation.*;
import org.yop.orm.model.Yopable;
import org.yop.orm.annotations.*;
import org.yop.orm.query.*;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.annotations.*;

/**
 * In this example, there are custom REST GET methods on {@link Book} that returns queries serialized to JSON.
 * <br>
 * Use the swagger UI to execute and get the JSON.
 * Then you can execute these queries with the swagger UI on the custom queries POST method.
 */
@Rest(
	path="book",
	summary = "Rest resource for books !",
	description = "A collection of sheets of paper bound together to hinge at one edge."
)
@Table(name="book")
public class Book implements Yopable {

	@Id
	@Column(name="id")
	private Long id;

	@Column(name="title")
	private String title;

	@NaturalId
	@Column(name="isbn13", not_null = true, length = 16)
	private String isbn13;

	@Column(name = "purchase_date")
	private LocalDate purchaseDate;

	@JoinTable(table = "rel_book_author", sourceColumn = "id_book", targetColumn = "id_author")
	private Collection<Author> authors = new ArrayList<>();

	@JoinColumn(remote = "id_book")
	private List<Chapter> chapters = new ArrayList<>();

	private Book(){}

	public Book(String title, LocalDate purchaseDate, String isbn13) {
		this.title = title;
		this.purchaseDate = purchaseDate;
		this.isbn13 = isbn13;
	}

	public Book(String title, LocalDate purchaseDate, String isbn13, Author... authors) {
		this.title = title;
		this.purchaseDate = purchaseDate;
		this.authors.addAll(Arrays.asList(authors));
		this.isbn13 = isbn13;
	}

	public Collection<Author> getAuthors() {
		return this.authors;
	}

	public List<Chapter> getChapters() {
		return this.chapters;
	}

	public String getTitle() {
		return this.title;
	}

	public String getIsbn13() {
		return this.isbn13;
	}

	public LocalDate getPurchaseDate() {
		return this.purchaseDate;
	}

	@Rest(path = "init", description = "insert some books/authors/chapters")
	public static Object init(IConnection connection) {
		// Delete everything !
		Delete.from(Book.class).joinAll().executeQueries(connection);

		Author ug    = new Author("Ug",    LocalDate.of(1984, 12, 8));
		Author roger = new Author("Roger", LocalDate.of(1977, 7, 7));
		Author mitch = new Author("Mitch", LocalDate.of(1900, 1, 1));
		Author bhl   = new Author("BHL",   LocalDate.of(1500, 1, 1));

		Book yopManual = new Book(
			"Yop unwritten manual",
			LocalDate.now(),
			"123-1-5678-000-1",
			ug, mitch
		);
		for (int i = 1; i <= 42; i++) {
			yopManual.chapters.add(new Chapter("Chapter #" + i, i));
		}

		Book yopForDummies = new Book(
			"Yop for dummies and/or my mom",
			LocalDate.of(1970, 1, 1),
			"123-1-5678-000-2",
			roger
		);
		for (int i = 1; i <= 42; i++) {
			yopForDummies.chapters.add(new Chapter("Chapter n°" + i, i));
		}

		Book dullBook = new Book(
			"Garter stitches through the ages",
			LocalDate.of(1234, 5, 6),
			"000-0-000-000-1",
			mitch,
			bhl,
			roger
		);
		for (int i = 1; i <= 175; i++) {
			dullBook.chapters.add(new Chapter("Dull chapter #" + i, i));
		}

		// Insert the books. Authors and chapters will be inserted thanks to 'joinAll'.
		List<Book> books = Arrays.asList(yopManual, yopForDummies, dullBook);
		Upsert.from(Book.class).joinAll().onto(books).execute(connection);
		return books;
	}

	@Rest(path = "json_query_by_natural_id", description = "JSON query that finds books from ISBN13")
	public static String fromISBN13() {
		return Select
			.from(Book.class)
			.joinAll()
			.where(Where.naturalId(new Book(null, null, "000-0-000-000-1")))
			.toJSON()
			.toString();
	}

	@Rest(path = "json_query_by_author_natural_id", description = "JSON query that finds books from author natural key")
	public static String rogersBooks() {
		Author roger = new Author("Roger", LocalDate.of(1977, 7, 7));
		return Select
			.from(Book.class)
			.join(JoinSet.to(Book::getAuthors).where(Where.naturalId(roger)))
			.toJSON()
			.toString();
	}

	@Rest(path = "json_query_books_in", description = "JSON query that finds books whose title is in an enumeration")
	public static String booksIn() {
		Collection<String> titles = Arrays.asList(
			"Yop for dummies and/or my mom",
			"We don't have this one, I guess",
			"Garter stitches through the ages"
		);
		return Select
			.from(Book.class)
			.where(new In(Book::getTitle, titles))
			.toJSON()
			.toString();
	}

	@Rest(path = "json_query_books_or_clause", description = "JSON query with 'OR' clause")
	public static String booksOrClause() {
		Collection<String> titles = Arrays.asList(
			"Yop for dummies and/or my mom",
			"We still don't have this one, I guess"
		);
		return Select
			.from(Book.class)
			.where(new Or(
				new In(Book::getTitle, titles),
				Where.compare(Book::getIsbn13, Operator.LT, "000-0-000-000-2")
			))
			.toJSON()
			.toString();
	}

	@Rest(path = "json_query_books_path_ref", description = "JSON query with a path ref")
	public static String booksPathRef() {
		return Select
			.from(Book.class)
			.join(JoinSet.to(Book::getAuthors))
			.where(Where.compare(
				Book::getPurchaseDate,
				Operator.LT,
				Path.pathSet(Book::getAuthors).to(Author::getBirthDate)
			))
			.toJSON()
			.toString();
	}

	@Rest(path = "json_query_upsert_book", description = "JSON query UPSERT")
	public static String booksUpsert(IConnection connection) {
		Collection<Book> books = Select.from(Book.class).joinAll().where(Where.id(1L)).execute(connection);
		Book newBook = new Book("A new book", LocalDate.of(2031, 1, 1), "999-9-999-999-9");
		books.add(newBook);
		return Upsert.from(Book.class).joinAll().checkNaturalID().onto(books).toJSON().toString();
	}
}

@Rest(path="author")
@Table(name="author")
class Author implements Yopable {

	@Id
	@Column(name="id")
	private Long id;

	@NaturalId
	@Column(name="name")
	private String name;

	@NaturalId
	@Column(name="birth_date")
	private LocalDate birthDate;

	@YopTransient
	@JoinTable(table = "rel_book_author", sourceColumn = "id_author", targetColumn = "id_book")
	private List<Book> books = new ArrayList<>();

	private Author() {}

	public Author(String name, LocalDate birthDate) {
		this.name = name;
		this.birthDate = birthDate;
	}

	@Override
	public Long getId() {
		return this.id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public LocalDate getBirthDate() {
		return this.birthDate;
	}

	public List<Book> getBooks() {
		return this.books;
	}
}

@Rest(path="chapter")
@Table(name="chapter")
class Chapter implements Yopable {

	@Id
	@Column(name="id")
	private Long id;

	@Column(name="name")
	private String name;

	@Column(name = "chapterNumber")
	private Integer chapterNumber;

	@YopTransient
	@JoinColumn(local = "id_book")
	private Book book;

	private Chapter() {}

	public Chapter(String name, Integer chapterNumber) {
		this.name = name;
		this.chapterNumber = chapterNumber;
	}

	@Override
	public Long getId() {
		return this.id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public Integer getChapterNumber() {
		return this.chapterNumber;
	}

	public Book getBook() {
		return this.book;
	}
}