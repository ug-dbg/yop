package org.yop.orm.model;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Interface Yop (persistent) objects must implement. <br>
 * It relies on a long ID. <br>
 * A yopable must provide an ID field that is a Long/long. It can either be :
 * <ul>
 *     <li>named 'id'</li>
 *     <li>annotated with {@link org.yop.orm.annotations.Id}</li>
 *     <li>both :)</li>
 * </ul>
 * Yop uses the default getter/setter methods that can find the ID field a lot.
 * <b>
 * Overriding {@link #getId()} and {@link #setId(Long)} in each Yopable should result in huge performance gain.
 * </b>
 * Created by hugues on 23/01/15.
 */
public interface Yopable {

	/**
	 * Get the current object ID.
	 * <br>
	 * A yopable must have a 'Long' id field. It can be named 'id' or not.
	 * If not, it must be {@link org.yop.orm.annotations.Id} annotated.
	 * <br>
	 * Overriding this method to return explicitly the ID value can result in huge performance gain.
	 * @return the id field value
	 */
	default Long getId() {
		Field idField = this.getIdField();
		try {
			return (Long) idField.get(this);
		} catch (IllegalAccessException e) {
			throw new YopRuntimeException(
				"Unable to access Id field [" + Reflection.fieldToString(idField) + "]"
			);
		}
	}

	/**
	 * Set the current object ID.
	 * <br>
	 * A yopable must have a 'Long' id field. It can be named 'id' or not.
	 * If not, it must be {@link org.yop.orm.annotations.Id} annotated.
	 * <br>
	 * Overriding this method to set explicitly the ID value can result in huge performance gain.
	 * @param id the id field value
	 */
	default void setId(Long id) {
		Field idField = this.getIdField();
		try {
			idField.set(this, id);
		} catch (IllegalAccessException e) {
			throw new YopRuntimeException(
				"Unable to access Id field [" + Reflection.fieldToString(idField) + "]"
			);
		}
	}

	/**
	 * Get the ID field name.
	 * @return the ID field name
	 */
	default String getIdFieldName() {
		return this.getIdField().getName();
	}

	/**
	 * Get the ID field.
	 * @return the ID field
	 */
	default Field getIdField() {
		return ORMUtil.getIdField(this.getClass());
	}

	/**
	 * Get the ID column name : {@link Column#name()} or, if no @Column annotation, 'ID'.
	 * @return the ID column name
	 */
	default String getIdColumn() {
		Field field = this.getIdField();
		return field.isAnnotationPresent(Column.class) ? field.getAnnotation(Column.class).name() : "ID";
	}

	/**
	 * Get all the fields that are {@link NaturalId} annotated.
	 * @return the natural ID fields
	 */
	default Collection<Field> getNaturalId() {
		return Reflection.getFields(this.getClass(), NaturalId.class);
	}

	/**
	 * Equals method on another Yopable object.
	 * Check if :
	 * <ul>
	 *     <li>o is null</li>
	 *     <li>o's class is not the same as this</li>
	 *     <li>this ID and o's ID are different</li>
	 *     <li>the natural ID is implemented and this != o for at least one field</li>
	 *     <li>Object.equals</li>
	 * </ul>
	 * @param o the other Yopable object to check
	 * @return true if this equals o
	 */
	default boolean equals(Yopable o) {
		if(o == null) {
			return false;
		}

		if(o == this) {
			return true;
		}

		if(! this.getClass().equals(o.getClass())) {
			return false;
		}

		if(this.getId() != null && o.getId() != null) {
			return this.getId().equals(o.getId());
		}

		Collection<Field> naturalId = this.getNaturalId();
		if(! naturalId.isEmpty()) {
			for (Field field : naturalId) {
				try {
					Object thisField = field.get(this);
					Object thatField = field.get(o);
					if ((thisField == null && thatField != null) || (thisField != null && thatField == null)
					||  (thisField != null && !thisField.equals(thatField))) {
						return false;
					}
				} catch (IllegalAccessException e) {
					throw new YopRuntimeException(
						"Unable to read @NaturalId field [" + Reflection.fieldToString(field) + "]",
						e
					);
				}
			}
			return true;
		}

		return false;
	}

	/**
	 * Hashcode method for any Yopable object.
	 * <ul>
	 *     <li>o is null → NullPointerException</li>
	 *     <li>the natural ID is implemented → hashcode for all natural fields</li>
	 *     <li>o's ID not null → ID hashcode</li>
	 *     <li>{@link System#identityHashCode(Object)}</li>
	 * </ul>
	 * @param o the other Yopable object to check
	 * @return true if this equals o
	 */
	static int hashCode(Yopable o) {
		if(o == null) {
			throw new NullPointerException();
		}

		Collection<Field> naturalId = o.getNaturalId();
		if(! naturalId.isEmpty()) {
			HashCodeBuilder builder = new HashCodeBuilder();
			for (Field field : naturalId) {
				try {
					builder.append(field.get(o));
				} catch (IllegalAccessException e) {
					throw new YopRuntimeException(
						"Unable to read @NaturalId field [" + Reflection.fieldToString(field) + "]",
						e
					);
				}
			}
			return builder.toHashCode();
		}

		if(o.getId() != null) {
			return o.getId().hashCode();
		}

		return System.identityHashCode(o);
	}
}
