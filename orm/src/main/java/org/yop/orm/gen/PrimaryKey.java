package org.yop.orm.gen;

/**
 * Primary key model.
 * A column can be a primary key, which can have an autoincrement.
 * Sequences should probably be stored here too.
 */
public class PrimaryKey {
	private final boolean autoincrement;

	public PrimaryKey(boolean autoincrement) {
		this.autoincrement = autoincrement;
	}

	public boolean isAutoincrement() {
		return this.autoincrement;
	}

	@Override
	public String toString() {
		return "PrimaryKey{" +
			"autoincrement=" + this.autoincrement +
		'}';
	}
}