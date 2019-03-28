package org.yop.orm.composite.model;

import org.junit.Test;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Yopable;

import java.time.LocalDate;

@Table(name = "composite_book")
public class Book implements Yopable {

	@NaturalId
	@Id(autoincrement = false)
	@Column(name = "name", not_null = true)
	private String name;

	@NaturalId
	@Id(autoincrement = false)
	@Column(name = "author", not_null = true)
	private String author;

	@Column(name = "publish_date")
	private LocalDate publishDate;

	private Book() {}

	public Book(String name, String author) {
		this.name = name;
		this.author = author;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAuthor() {
		return this.author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public LocalDate getPublishDate() {
		return this.publishDate;
	}

	public void setPublishDate(LocalDate publishDate) {
		this.publishDate = publishDate;
	}

	@Override
	public String toString() {
		return "Book{" +
			"name='" + this.name + '\'' +
			", author='" + this.author + '\'' +
			", publishDate=" + this.publishDate +
		'}';
	}
}
