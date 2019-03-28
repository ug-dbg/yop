package org.yop.orm.gen;

import org.apache.commons.lang.StringUtils;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.JoinColumn;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.sql.Config;
import org.yop.orm.sql.dialect.IDialect;
import org.yop.orm.util.ORMUtil;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A table column that can generate a column clause in an SQL CREATE query.
 * <br>
 * Columns are {@link Comparable} :
 * <ol>
 *     <li>{@link #isPK()}, reversed</li>
 *     <li>{@link #getName()}</li>
 *     <li>nulls last</li>
 * </ol>
 */
public class Column implements Comparable<Column> {
	private static final Comparator<Column> COMPARATOR =
		Comparator.nullsLast(
			Comparator.comparing(Column::isPK).reversed().thenComparing(Column::getName)
		);

	private final String name;
	private final String type;
	private final int length;
	private final List<String> sequences = new ArrayList<>();

	private final IDialect dialect;

	private PrimaryKey pk;
	private ForeignKey fk;

	private boolean naturalKey;
	private boolean notNull;

	Column(String name, Class type, int length, IDialect dialect) {
		this.name = name;
		this.type = dialect.getForType(type);
		this.length = length;
		this.dialect = dialect;
	}

	public void setFK(ForeignKey fk) {
		this.fk = fk;
	}

	public String getName() {
		return this.name;
	}

	public String getType() {
		return this.type;
	}

	public boolean isNaturalKey() {
		return this.naturalKey;
	}

	public int getLength() {
		return this.length;
	}

	public PrimaryKey getPk() {
		return this.pk;
	}

	public ForeignKey getFk() {
		return this.fk;
	}

	public boolean isNotNull() {
		return this.notNull;
	}

	public String toSQL() {
		return this.dialect.toSQL(this);
	}

	public boolean isPK() {
		return this.pk != null;
	}

	public List<String> getSequences() {
		return this.sequences;
	}

	@Override
	public String toString() {
		return "Column{" +
			"name='" + this.name + '\'' +
			", type='" + this.type + '\'' +
			", naturalKey=" + this.naturalKey +
			", notNull=" + this.notNull +
			", length=" + this.length +
			", sequences=" + this.sequences +
			", pk=" + this.pk +
			", fk=" + this.fk +
			", dialect=" + this.dialect +
		'}';
	}

	@Override
	public int compareTo(@Nonnull Column o) {
		return COMPARATOR.compare(this, o);
	}

	@SuppressWarnings("unchecked")
	static Column fromField(Field field, Config config) {
		Column column = new Column(
			ORMUtil.getColumnName(field),
			ORMUtil.getColumnType(field),
			ORMUtil.getColumnLength(field),
			config.getDialect()
		);
		column.notNull = ORMUtil.isColumnNotNullable(field);

		if(ORMUtil.isIdField(field)) {
			column.pk = new PrimaryKey(
				!field.isAnnotationPresent(Id.class) || field.getAnnotation(Id.class).autoincrement()
			);
			String seq = ORMUtil.readSequence(field, config);
			if(StringUtils.isNotBlank(seq)) {
				column.sequences.add(seq);
			}
		}
		column.naturalKey = field.isAnnotationPresent(NaturalId.class);
		return column;
	}

	static Column fromJoinColumnField(Field field, Config config) {
		JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
		Column column = new Column(
			joinColumn.local(),
			Long.class,
			50,
			config.getDialect()
		);
		column.notNull = false;
		column.naturalKey = field.isAnnotationPresent(NaturalId.class);
		return column;
	}
}