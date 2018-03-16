package org.yop.orm.supplychain.model;

import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.annotations.Table;
import org.yop.orm.model.Persistent;
import org.yop.orm.transform.AbbreviateTransformer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "product")
public class Product  extends Persistent {

	@NaturalId
	@Column(name = "name")
	private String name;

	@Column(name = "price", not_null = true)
	private float price;

	@Column(name = "description")
	private String description;

	@Column(
		name = "comments",
		length = 49,
		length_strategy = Column.LengthStrategy.CUT,
		transformer = AbbreviateTransformer.class
	)
	private String comment;

	@Column(name = "wheight")
	private float weight;

	@Column(name = "height")
	private int height;

	@Column(name = "length")
	private Integer length;

	@Column(name = "width")
	private int width;

	@Column(name = "creation_date")
	private LocalDateTime creationDate;

	@Column(name = "available_from")
	private LocalDateTime availableFrom;

	@Column(name = "available_until")
	private LocalDateTime availableUntil;

	@JoinTable(
		table = "rel_product_reference",
		sourceColumn = "id_product",
		targetColumn = "id_reference"
	)
	private Reference reference;

	@JoinTable(
		table = "rel_product_category",
		sourceColumn = "id_product",
		targetColumn = "id_category"
	)
	private List<Category> categories = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getPrice() {
		return price;
	}

	public void setPrice(float price) {
		this.price = price;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public LocalDateTime getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(LocalDateTime creationDate) {
		this.creationDate = creationDate;
	}

	public LocalDateTime getAvailableFrom() {
		return availableFrom;
	}

	public void setAvailableFrom(LocalDateTime availableFrom) {
		this.availableFrom = availableFrom;
	}

	public LocalDateTime getAvailableUntil() {
		return availableUntil;
	}

	public void setAvailableUntil(LocalDateTime availableUntil) {
		this.availableUntil = availableUntil;
	}

	public Reference getReference() {
		return reference;
	}

	public void setReference(Reference reference) {
		this.reference = reference;
	}
}
