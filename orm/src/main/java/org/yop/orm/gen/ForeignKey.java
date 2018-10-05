package org.yop.orm.gen;

/**
 * Foreign key model.
 * A Column can have/be a foreign key.
 * <br>
 * A FK constraint will be generated from this object.
 */
public class ForeignKey {
	private final String name;
	private final String referencedTable;
	private final String referencedColumn;

	public ForeignKey(String name, String referencedTable, String referencedColumn) {
		this.name = name;
		this.referencedTable = referencedTable;
		this.referencedColumn = referencedColumn;
	}

	public String getName() {
		return this.name;
	}

	public String getReferencedTable() {
		return this.referencedTable;
	}

	public String getReferencedColumn() {
		return this.referencedColumn;
	}

	@Override
	public String toString() {
		return "ForeignKey{" +
			"name='" + this.name + '\'' +
			", referencedTable='" + this.referencedTable + '\'' +
			", referencedColumn='" + this.referencedColumn + '\'' +
		'}';
	}
}