package org.yop.orm.model;

import com.google.common.primitives.Primitives;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinTable;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface Yop (persistent) objects must implement. <br>
 * It relies on a long ID. <br>
 * A yopable must then provide an @Id annotated field. <br>
 *
 * Created by hugues on 23/01/15.
 */
public interface Yopable {

	Logger logger = LoggerFactory.getLogger(Yopable.class);

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
		return getIdField(this.getClass());
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

		return this.equals((Object)o);
	}

	static <T extends Yopable> Field getIdField(Class<T> clazz) {
		List<Field> idFields = Reflection.getFields(clazz, Id.class);
		if(idFields.size() == 0) {
			logger.trace("No @Id field on [{}]. Assuming 'id'", clazz.getName());
			Field field = Reflection.get(clazz, "id");
			if(field != null && Long.class.isAssignableFrom(Primitives.wrap(field.getType()))) {
				return field;
			}
			throw new YopRuntimeException("No Long ID field in [" + clazz.getName() + "] !");
		}
		if(idFields.size() > 1) {
			throw new YopRuntimeException("Several @Id fields ! Only one Field of Long type can be @Id !");
		}
		Field field = idFields.get(0);
		if(!Long.class.isAssignableFrom(field.getType())) {
			throw new YopRuntimeException("@Id field is not Long compatible !");
		}
		return field;
	}

	static <T extends Yopable> String getIdColumn(Class<T> clazz) {
		Field field = getIdField(clazz);
		return field.isAnnotationPresent(Column.class) ? field.getAnnotation(Column.class).name() : "ID";
	}
}
