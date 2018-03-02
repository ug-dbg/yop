package org.yop.orm.gen;

import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.ORMTypes;
import org.yop.orm.util.ORMUtil;

import java.lang.reflect.Field;

/**
 * A table column that can generate a column clause in an SQL CREATE query.
 */
public class Column {
	private String name;
	private String type;
	private boolean naturalKey;
	private boolean notNull;
	private int length;

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

	private String toSQL() {
		return this.types.toSQL(this);
	}

	@Override
	public String toString() {
		return this.toSQL();
	}

	@SuppressWarnings("unchecked")
	static Column fromField(Field field, ORMTypes types) {
		Column column = new Column(
			ORMUtil.getColumnName(field),
			ORMUtil.getColumnType(field, types),
			ORMUtil.getColumnLength(field),
			types
		);

		if(field.equals(ORMUtil.getIdField((Class<? extends Yopable>)field.getDeclaringClass()))) {
			column.pk = new PrimaryKey(
				!field.isAnnotationPresent(Id.class) || field.getAnnotation(Id.class).autoincrement()
			);
		}
		column.naturalKey = field.isAnnotationPresent(NaturalId.class);
		return column;
	}
}