package org.yop.orm.gen;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.ORMTypes;
import org.yop.orm.util.ORMUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A table column that can generate a column clause in an SQL CREATE query.
 */
public class Column implements Comparable<Column> {
	private static Comparator<Column> COMPARATOR =
		Comparator.nullsLast(
			Comparator.comparing(Column::isPK).reversed().thenComparing(Column::getName)
		);

	private String name;
	private String type;
	private boolean naturalKey;
	private boolean notNull;
	private int length;
	private List<String> sequences = new ArrayList<>();

	private PrimaryKey pk;
	private ForeignKey fk;

	private ORMTypes types;

	Column(String name, String type, int length, ORMTypes types) {
		this.name = name;
		this.type = type;
		this.length = length;
		this.types = types;
	}

	public void setFK(ForeignKey fk) {
		this.fk = fk;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public boolean isNaturalKey() {
		return naturalKey;
	}

	public int getLength() {
		return length;
	}

	public PrimaryKey getPk() {
		return pk;
	}

	public ForeignKey getFk() {
		return fk;
	}

	public boolean isNotNull() {
		return notNull;
	}

	public String toSQL() {
		return this.types.toSQL(this);
	}

	public boolean isPK() {
		return this.pk != null;
	}

	public List<String> getSequences() {
		return sequences;
	}

	@Override
	public String toString() {
		return "Column{" +
			"name='" + name + '\'' +
			", type='" + type + '\'' +
			", naturalKey=" + naturalKey +
			", notNull=" + notNull +
			", length=" + length +
			", sequences=" + sequences +
			", pk=" + pk +
			", fk=" + fk +
			", types=" + types +
		'}';
	}

	@Override
	public int compareTo(Column o) {
		return COMPARATOR.compare(this, o);
	}

	@SuppressWarnings("unchecked")
	static Column fromField(Field field, ORMTypes types) {
		Column column = new Column(
			ORMUtil.getColumnName(field),
			ORMUtil.getColumnType(field, types),
			ORMUtil.getColumnLength(field),
			types
		);
		column.notNull = ORMUtil.isColumnNotNullable(field);

		if(field.equals(ORMUtil.getIdField((Class<? extends Yopable>)field.getDeclaringClass()))) {
			column.pk = new PrimaryKey(
				!field.isAnnotationPresent(Id.class) || field.getAnnotation(Id.class).autoincrement()
			);
			String seq = ORMUtil.readSequence(field);
			if(StringUtils.isNotBlank(seq)) {
				column.sequences.add(seq);
			}
		}
		column.naturalKey = field.isAnnotationPresent(NaturalId.class);
		return column;
	}
}