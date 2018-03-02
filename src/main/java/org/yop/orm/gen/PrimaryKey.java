package org.yop.orm.gen;

public class PrimaryKey {
	private boolean autoincrement;

	public PrimaryKey(boolean autoincrement) {
		this.autoincrement = autoincrement;
	}

	public boolean isAutoincrement() {
		return autoincrement;
	}
}