import java.util.*;

import org.yop.orm.evaluation.*;
import org.yop.orm.model.Yopable;
import org.yop.orm.annotations.*;
import org.yop.orm.query.*;
import org.yop.orm.sql.adapter.IConnection;
import org.yop.rest.annotations.*;

@Rest(
	path="book",
	summary = "Rest resource for books !",
	description = "is both a usually portable physical object "
		+ "and the body of immaterial representations "
		+ "or intellectual object whose material signs"
)
@Table(name="book")
public class Book implements Yopable {

	@Id
	@Column(name="id")
	private Long id;

	@Column(name="title")
	private String title;

	@JoinColumn(local = "id_book", remote = "id_author")
	private Author author;

	@JoinColumn(local = "id_book", remote = "id_chapter")
	private List<Chapter> chapters = new ArrayList<>();

	public Author getAuthor() {
		return this.author;
	}

	public List<Chapter> getChapters() {
		return this.chapters;
	}

	@Rest(path = "search", description = "search books with more than X chapters")
	public static Object search(IConnection connection, @RequestParam(name = "numberOfChapters") Integer min) {
		return Select
			.from(Book.class)
			.joinAll()
			.join(JoinSet.to(Book::getChapters).where(Where.compare(Chapter::getChapterNumber, Operator.GE, min)))
			.execute(connection);
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
	@JoinColumn(local = "id_author", remote = "id_book")
	private List<Book> books = new ArrayList<>();

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
class Chapter implements Yopable {

	@Id
	@Column(name="id")
	private Long id;

	@Column(name="name")
	private String name;

	@Column(name = "chapterNumber")
	private Integer chapterNumber;

	@YopTransient
	@JoinColumn(local = "id_chapter", remote = "id_book")
	private Book book;

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