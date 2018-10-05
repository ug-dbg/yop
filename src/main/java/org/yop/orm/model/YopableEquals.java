package org.yop.orm.model;

/**
 * The Yopable interface have nice {@link Yopable#equals(Yopable)}/{@link Yopable#hashCode(Yopable)} default methods
 * that you might want to use without overriding {@link Object#hashCode()} and {@link Object#equals(Object)}.
 * <br><br>
 * This class is kind of a wrapper on a Yopable instance.
 * It explicitly uses {@link Yopable#equals(Yopable)}/{@link Yopable#hashCode(Yopable)} on the wrapped object.
 */
public class YopableEquals {

	/** the wrapped element */
	private final Yopable yopable;

	/**
	 * Default constructor : give me the Yopable object to wrap.
	 * @param yopable the element to wrap
	 */
	public YopableEquals(Yopable yopable) {
		this.yopable = yopable;
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o != null
			&& this.getClass() == o.getClass()
			&& this.yopable.equals(((YopableEquals) o).yopable);
	}

	@Override
	public int hashCode() {
		return Yopable.hashCode(this.yopable);
	}
}
