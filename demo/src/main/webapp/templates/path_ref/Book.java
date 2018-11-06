import java.time.LocalDate;
import java.util.*;

import org.yop.orm.evaluation.*;
import org.yop.orm.model.Yopable;
import org.yop.orm.annotations.*;
import org.yop.orm.query.*;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.annotations.*;

/**
 * In this example, authors have a birth date and books a purchase date.
 * <br>
 * The init method inserts some books, authors and chapters.
 * <br><br>
 * A subtle {@link #timeParadox(IConnection, Boolean)} method
 * tries to find books which were purchased before the birth of any of their authors ;-)
 * <br>
 * This method uses a {@link Path} in the {@link Comparison}
 * to build a path from {@link Book} to {@link Author#getBirthDate()}.
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

	@Column(name = "purchase_date")
	private LocalDate purchaseDate;

	@JoinTable(table = "rel_book_author", sourceColumn = "id_book", targetColumn = "id_author")
	private Collection<Author> authors = new ArrayList<>();

	@JoinColumn(remote = "id_book")
	private List<Chapter> chapters = new ArrayList<>();

	private Book(){}

	public Book(String title, LocalDate purchaseDate) {
		this.title = title;
		this.purchaseDate = purchaseDate;
	}

	public Book(String title, LocalDate purchaseDate, Author... authors) {
		this.title = title;
		this.purchaseDate = purchaseDate;
		this.authors.addAll(Arrays.asList(authors));
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

	public LocalDate getPurchaseDate() {
		return this.purchaseDate;
	}

	@Rest(path = "time_paradox", description = "Find books purchased before the author is actually born !")
	public static Set<Book> timeParadox(
		IConnection connection,
		@RequestParam(name = "joinAll") Boolean joinAll) {
		Select<Book> select = Select
			.from(Book.class)
			.join(JoinSet.to(Book::getAuthors))
			.where(Where.compare(
				Book::getPurchaseDate,
				Operator.LT,
				Path.pathSet(Book::getAuthors).to(Author::getBirthDate)
		));
		if (joinAll != null && joinAll) {
			select.joinAll();
		}
		return select.execute(connection);
	}

	@Rest(path = "init", description = "insert some books/authors/chapters")
	public static Object init(IConnection connection) {
		// Delete everything !
		Delete.from(Book.class).joinAll().executeQueries(connection);

		Author ug    = new Author("Ug", LocalDate.of(1984, 12, 8));
		Author roger = new Author("Roger", LocalDate.of(1977, 7, 7));
		Author mitch = new Author("Mitch", LocalDate.of(1900, 1, 1));
		Author bhl   = new Author("BHL", LocalDate.of(1500, 1, 1));

		Book yopManual = new Book("Yop unwritten manual", LocalDate.now(), ug, mitch);
		for (int i = 1; i <= 42; i++) {
			yopManual.chapters.add(new Chapter("Chapter #" + i, i));
		}

		Book yopForDummies = new Book("Yop for dummies and/or my mom and/or Roger", LocalDate.of(1970, 1, 1), roger);
		for (int i = 1; i <= 42; i++) {
			yopForDummies.chapters.add(new Chapter("Chapter n°" + i, i));
		}

		Book dullBook = new Book("Garter stitches through the ages", LocalDate.of(1234, 5, 6), mitch, bhl, roger);
		for (int i = 1; i <= 175; i++) {
			dullBook.chapters.add(new Chapter("Dull chapter #" + i, i));
		}

		// Insert the books. Authors and chapters will be inserted thanks to 'joinAll'.
		List<Book> books = Arrays.asList(yopManual, yopForDummies, dullBook);
		Upsert.from(Book.class).joinAll().onto(books).execute(connection);
		return books;
	}
}

@Rest(path="author")
@Table(name="author")
class Author implements Yopable {

	@Id
	@Column(name="id")
	private Long id;

	@Column(name="name")
	private String name;

	@Column(name="birth_date")
	private LocalDate birthDate;

	@YopTransient
	@JoinTable(table = "rel_book_author", sourceColumn = "id_author", targetColumn = "id_book")
	private List<Book> books = new ArrayList<>();

	private Author() {}

	public Author(String name) {
		this.name = name;
	}

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