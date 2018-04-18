package org.yop.orm.model;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Interface Yop (persistent) objects must implement. <br>
 * It relies on a long ID. <br>
 * A yopable must then provide an @Id annotated field. <br>
 *
 * Created by hugues on 23/01/15.
 */
public interface Yopable {

	default Long getId() {
		Field idField = this.getIdField();
		idField.setAccessible(true);

		try {
			return (Long) idField.get(this);
		} catch (IllegalAccessException e) {
			throw new YopRuntimeException(
				"Unable to access Id field [" + idField.getName() + "] on [" + this.getClass().getName() + "]"
			);
		}
	}

	default void setId(Long id) {
		Field idField = this.getIdField();
		idField.setAccessible(true);

		try {
			idField.set(this, id);
		} catch (IllegalAccessException e) {
			throw new YopRuntimeException(
				"Unable to access Id field [" + idField.getName() + "] on [" + this.getClass().getName() + "]"
			);
		}
	}

	default String getIdFieldName() {
		return this.getIdField().getName();
	}

	default Field getIdField() {
		return ORMUtil.getIdField(this.getClass());
	}

	default String getIdColumn() {
		Field field = this.getIdField();
		return field.isAnnotationPresent(Column.class) ? field.getAnnotation(Column.class).name() : "ID";
	}

	default Collection<Field> getNaturalId() {
		return Reflection.getFields(this.getClass(), NaturalId.class);
	}

	/**
	 * Find all the @JoinTable fields of the current instance that are transient.
	 * @return the transient @JoinTable fields
	 */
	default Collection<Field> getTransientRelations() {
		return  Reflection
			.getFields(this.getClass(), JoinTable.class, false)
			.stream()
			.filter(f -> !Reflection.isNotTransient(f))
			.collect(Collectors.toList());
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
						"Unable to read @NaturalId field [" + field.getName() + "] from [" + this.getClass() + "]",
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
						"Unable to read @NaturalId field [" + field.getName() + "] from [" + o.getClass() + "]",
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
