package org.yop.reflection.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Book.Entity
@SuppressWarnings("unused")
public class Book {

	private long id;

	@TechnicalID
	private String isbn;

	private String title;

	private String description;

	@ComposedOf
	private List<Sheet> sheets = new ArrayList<>();

	private Book() {}

	public Book(String isbn) {
		this.isbn = isbn;
	}

	public long getId() {
		return this.id;
	}

	public String getIsbn() {
		return this.isbn;
	}

	public String getTitle() {
		return this.title;
	}

	public String getDescription() {
		return this.description;
	}

	public List<Sheet> getSheets() {
		return this.sheets;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setSheets(List<Sheet> sheets) {
		this.sheets = sheets;
	}

	public static class Sheet {
		private String content;
		private int number;
		private Book book;

		public String getContent() {
			return this.content;
		}

		public int getNumber() {
			return this.number;
		}

		public Book getBook() {
			return this.book;
		}
	}

	public static class IsEmptyPredicate implements Predicate<Book> {
		@Override
		public boolean test(Book book) {
			return book != null && ! book.getSheets().isEmpty();
		}
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ComposedOf {}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TechnicalID {}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Entity {}
}
