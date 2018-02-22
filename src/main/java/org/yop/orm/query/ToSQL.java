package org.yop.orm.query;

import org.yop.orm.annotations.JoinTable;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.text.MessageFormat;

/**
 * Some common code to generate SQL from objects.
 * <br>
 * Every object SQL can be generated from should have a 'toSQL' method :
 * this class is for generating SQL from several ones.
 */
public class ToSQL {

	public enum JoinType {
		JOIN(" join "), INNER_JOIN(" inner join "), LEFT_JOIN(" left join ");

		JoinType(String sql) {
			this.sql = sql;
		}

		private String sql;
	}

	private static final String JOIN = " {0} as {1} on {2} = {3} ";

	/**
	 * Generate 'join' clauses from a parent context, a target context and a field linking them.
	 * @param parent the parent context
	 * @param to     the target context
	 * @param field  the field from 'parent' to 'to' (should have a @JoinTable annotation)
	 * @param <From> the source type
	 * @param <To>   the target type
	 * @return the SQL join table clauses
	 */
	static <From extends Yopable, To extends Yopable> String toSQLJoin(
		JoinType type,
		Context<From> parent,
		Context<To> to,
		Field field) {

		Class<From> from = parent.getTarget();
		JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);
		if(joinTableAnnotation == null) {
			throw new YopMappingException(
				"Field [" + from.getName() + "#" + field.getName() + "] has no JoinTable annotation !"
			);
		}

		String joinTable = joinTableAnnotation.table();
		String joinTableSourceColumn = joinTableAnnotation.sourceColumn();
		String joinTableTargetColumn = joinTableAnnotation.targetColumn();
		String relationAlias = parent.getPath() + Context.SQL_SEPARATOR + field.getName();
		String fromIdColumn = Reflection.newInstanceNoArgs(parent.getTarget()).getIdColumn();

		String targetTableAlias = to.getPath();
		String toIdColumn = Reflection.newInstanceNoArgs(to.getTarget()).getIdColumn();

		return
			toSQLJoin(type, joinTable, relationAlias, joinTableSourceColumn, parent.getPath() + Context.DOT + fromIdColumn)
			+
			toSQLJoin(type, to.getTableName(), targetTableAlias, toIdColumn, relationAlias + Context.DOT + joinTableTargetColumn);
	}

	private static String toSQLJoin(JoinType type, String table, String alias, String column, String referencedAlias) {
		return type.sql + MessageFormat.format(JOIN, table, alias, referencedAlias, alias + Context.DOT + column);
	}
}
