package org.yop.orm.gen;

/**
 * Primary key model.
 * A column can be a primary key, which can have an autoincrement.
 * Sequences should probably be stored here too.
 */
public class PrimaryKey {
	private boolean autoincrement;

	public PrimaryKey(boolean autoincrement) {
		this.autoincrement = autoincrement;
	}

	public boolean isAutoincrement() {
		return autoincrement;
	}
}