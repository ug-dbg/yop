package org.yop.orm.model;

import org.yop.orm.exception.YopMappingException;
import org.yop.orm.util.ORMUtil;
import org.yop.reflection.Reflection;

import java.lang.reflect.Field;

/**
 * Interface Yop (persistent) objects can implement for convenience. <br>
 * It relies on an ID. <br>
 * A yopable must provide an ID field that is a {@link Comparable}. It can either be :
 * <ul>
 *     <li>named 'id'</li>
 *     <li>annotated with {@link org.yop.orm.annotations.Id}</li>
 *     <li>both :)</li>
 * </ul>
 * Created by hugues on 23/01/15.
 */
public interface Yopable {

	/**
	 * Get the current object ID.
	 * <br>
	 * A yopable must have a {@link Comparable} id field. It can be named 'id' or not.
	 * If not, it must be {@link org.yop.orm.annotations.Id} annotated.
	 * @return the id field value
	 */
	default Comparable getId() {
		return (Comparable) Reflection.readField(ORMUtil.getIdField(this.getClass()), this);
	}

	/**
	 * Mostly, Yopable objects should have a Long ID.
	 * <br>
	 * You can override this method instead of {@link #setId(Comparable)} so you don't have to cast the id parameter.
	 * @param id the Long id value for the current instance.
	 */
	default void setId(Long id) {
		Field idField = ORMUtil.getIdField(this.getClass());
		if (!Long.class.isAssignableFrom(idField.getType())) {
			throw new YopMappingException(
				"Using [" + id + "] to set a non Long ID field [" + Reflection.fieldToString(idField) + "]"
			);
		}
		this.setId((Comparable) id);
	}

	/**
	 * Set the current object ID.
	 * <br>
	 * A yopable must have a {@link Comparable} id field. It can be named 'id' or not.
	 * If not, it must be {@link org.yop.orm.annotations.Id} annotated.
	 * @param id the id field value
	 */
	default void setId(Comparable id) {
		Reflection.set(ORMUtil.getIdField(this.getClass()), this, id);
	}

	/**
	 * Equals method on another Yopable object.
	 * <br>
	 * See {@link ORMUtil#equals(Object, Object)}.
	 * @return true if this equals o
	 */
	default boolean equals(Yopable o) {
		return ORMUtil.equals(this, o);
	}

	/**
	 * Hashcode method for any Yopable object.
	 * <br>
	 * See {@link ORMUtil#hashCode(Object)}.
	 */
	static int hashCode(Object o) {
		return ORMUtil.hashCode(o);
	}
}
