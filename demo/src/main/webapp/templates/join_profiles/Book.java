import java.time.LocalDate;
import java.util.*;

import org.yop.orm.evaluation.*;
import org.yop.orm.model.Yopable;
import org.yop.orm.annotations.*;
import org.yop.orm.query.*;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.annotations.*;

/**
 * This is a not-very-inventive first example for YOP.
 * <br>
 * A book has authors  (N-N) relationship → JoinTable. <br>
 * A book has chapters (1-N) relationship → JoinColumn. <br>
 * Cycles are cut using @YopTransient → these relations must be joined explicitely. <br>
 * <br>
 * An init method inserts some data so you can play with the very nice Swagger UI and the default YOP rest methods.
 */
@Rest(
	path="book",
	summary = "Rest resource for books !",
	description = "A collection of sheets of paper bound together to hinge at one edge."
)
@JoinProfiles(names = {"book_to_author"})
@Table(name="book")
public class Book implements Yopable {

	@Id
	@Column(name="id")
	private Long id;

	@Column(name="title")
	private String title;

	@Column(name = "purchase_date")
	private LocalDate purchaseDate;

	@JoinProfile(profiles = {"chapter_to_author", "book_to_author"})
	@JoinTable(table = "rel_book_author", sourceColumn = "id_book", targetColumn = "id_author")
	private Collection<Author> authors = new ArrayList<>();

	@JoinProfile(profiles = {"author_to_chapters"})
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

	@Rest(path = "init", description = "insert some books/authors/chapters")
	public static Object init(IConnection connection) {
		// Delete everything !
		Delete.from(Book.class).joinAll().executeQueries(connection);

		Author ug    = new Author("Ug");
		Author roger = new Author("Roger");
		Author mitch = new Author("Mitch");
		Author bhl   = new Author("BHL");

		Book yopManual = new Book("Yop unwritten manual", LocalDate.now(), ug, mitch);
		for (int i = 1; i <= 42; i++) {
			yopManual.chapters.add(new Chapter("Chapter #" + i, i));
		}

		Book yopForDummies = new Book("Yop for dummies and/or my mom", LocalDate.of(1970, 1, 1), roger);
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
@JoinProfiles(names = {"author_to_books", "author_to_chapters"})
class Author implements Yopable {

	@Id
	@Column(name="id")
	private Long id;

	@Column(name="name")
	private String name;

	@YopTransient
	@JoinProfile(profiles = {"author_to_books", "author_to_chapters"})
	@JoinTable(table = "rel_book_author", sourceColumn = "id_author", targetColumn = "id_book")
	private List<Book> books = new ArrayList<>();

	private Author() {}

	public Author(String name) {
		this.name = name;
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

	public List<Book> getBooks() {
		return this.books;
	}
}

@Rest(path="chapter")
@Table(name="chapter")
@JoinProfiles(names = {"chapter_to_author", "chapter_to_book"})
class Chapter implements Yopable {

	@Id
	@Column(name="id")
	private Long id;

	@Column(name="name")
	private String name;

	@Column(name = "chapterNumber")
	private Integer chapterNumber;

	@YopTransient
	@JoinProfile(profiles = {"chapter_to_author", "chapter_to_book"})
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