package org.yop.orm.gen;

/**
 * Foreign key model.
 * A Column can have/be a foreign key.
 * <br>
 * A FK constraint will be generated from this object.
 */
public class ForeignKey {
	private String name;
	private String referencedTable;
	private String referencedColumn;

	public ForeignKey(String name, String referencedTable, String referencedColumn) {
		this.name = name;
		this.referencedTable = referencedTable;
		this.referencedColumn = referencedColumn;
	}

	public String getName() {
		return name;
	}

	public String getReferencedTable() {
		return referencedTable;
	}

	public String getReferencedColumn() {
		return referencedColumn;
	}
}