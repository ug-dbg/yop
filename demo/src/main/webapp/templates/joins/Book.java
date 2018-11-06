import java.time.LocalDate;
import java.util.*;

import org.yop.orm.evaluation.*;
import org.yop.orm.model.Yopable;
import org.yop.orm.annotations.*;
import org.yop.orm.query.*;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.annotations.*;

/**
 * In this example, a custom method {@link Author#getAuthorsByName(IConnection, String, boolean)} is configured
 * to retrieve authors by name and fetch their books, on demand.
 * <br>
 * This could also be achieved using a JSON query on 'Author' REST endpoint, with 'POST' method,
 * that sets a custom a fetch graph (as well as custom evaluations).
 * <br>
 * For instance :
 * <pre>
 * {@code
 * {
 *  "where": {
 *    "evaluations": [
 *      {
 *        "type": "Comparison",
 *        "field": "name",
 *        "op": "LIKE",
 *        "ref": "%oge%"
 *      }
 *    ]
 *  },
 *  "joins": [
 *    {"field": "books", "joins": [{"field": "chapters"}]}
 *  ],
 *  "target": "Author"
 * }
 * }
 * </pre>
 *
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
class Author implements Yopable {

	@Id
	@Column(name="id")
	private Long id;

	@Column(name="name")
	private String name;

	@YopTransient
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

	/**
	 * Select authors, given their name, with the possibility to retrieve their books as well.
	 * <br>
	 * We use a custom {@link Select} query with a {@link Comparison} in the {@link Where} clause.
	 * <br>
	 * We add an explicit join for [Author→Book], on demand.
	 * <br>
	 * Since a parameter controls the fetch graph, we need to to the serialization in the very method.
	 * → We use {@link Select#toJSONQuery()} to use the same joins in the serialization.
	 * @param connection the underlying connection, will be provided by the servlet
	 * @param name       the name of the author(s) to find, configured here as a path parameter. Will be used as %name%.
	 * @param fetchBooks true to fetch the books from the author(s)
	 * @return the results of the Select query, serialized to JSON array, as string
	 */
	@Rest(path = "{name}", description = "fetch authors by name")
	public static String getAuthorsByName(
		IConnection connection,
		@PathParam(name = "name") String name,
		@RequestParam(name = "fetchBooks", required = true) boolean fetchBooks) {

		String nameCriteria = "%" + name + "%";
		Select<Author> select = Select
			.from(Author.class)
			.where(Where.compare(Author::getName, Operator.LIKE, nameCriteria));
		if (fetchBooks) {
			select.join(JoinSet.to(Author::getBooks));
		}
		Collection<Author> authors = select.execute(connection);

		// We must do the serialization here : Yop REST does not know the explicit joins to apply.
		// Select.toJSONQuery creates a JSON query with joins from the Select query.
		return select.toJSONQuery().onto(authors).toJSON();
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