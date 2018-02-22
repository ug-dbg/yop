package org.yop.orm.query;

import org.yop.orm.annotations.JoinTable;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.model.Yopable;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;

/**
 * Some common code to generate SQL from objects.
 * <br>
 * Every object SQL can be generated from should have a 'toSQL' method :
 * this class is for generating SQL from several ones.
 */
public class ToSQL {

	/**
	 * Generate 'join' clauses from a parent context, a target context and a field linking them.
	 * @param parent the parent context
	 * @param to     the target context
	 * @param field  the field from 'parent' to 'to' (should have a @JoinTable annotation)
	 * @param <From> the source type
	 * @param <To>   the target type
	 * @return the SQL join table clauses
	 */
	static <From extends Yopable, To extends Yopable> String toSQL(Context<From> parent, Context<To> to, Field field) {
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

		return toSQLJoinTable(
			joinTable, relationAlias, joinTableSourceColumn, parent.getPath() + Context.DOT + fromIdColumn
		)
		+
		toSQLTargetTable(
			to.getTableName(), targetTableAlias, toIdColumn, relationAlias + Context.DOT + joinTableTargetColumn
		);
	}

	private static String toSQLJoinTable(String table, String alias, String column, String referencedIDColumnAlias) {
		String sql = " left join " + table + " as " + alias;
		sql += " on " + referencedIDColumnAlias + " = " + alias + Context.DOT + column;
		return sql;
	}

	private static String toSQLTargetTable(String table, String alias, String column, String referencedJoinColumnAlias) {
		String sql = " left join " + table + " as " + alias;
		sql += " on " + referencedJoinColumnAlias + " = " + alias + Context.DOT + column;
		return sql;
	}

}
