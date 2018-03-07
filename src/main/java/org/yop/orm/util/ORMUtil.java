package org.yop.orm.util;

import com.google.common.primitives.Primitives;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.Id;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.annotations.Table;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.model.Yopable;
import org.yop.orm.sql.Constants;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.yop.orm.util.Reflection.isNotTransient;

/**
 * Some utility methods to read ORM annotations, find ID field, column type...
 */
public class ORMUtil {

	private static final Logger logger = LoggerFactory.getLogger(ORMUtil.class);

	/**
	 * Get the table name for the given yopable target
	 * (read {@link Table} annotation or return class name to upper case).
	 * @param target the target Yopable implementation
	 * @return the table name for the current context
	 */
	public static String getTableName(Class<? extends Yopable> target) {
		if(target.isAnnotationPresent(Table.class)) {
			return target.getAnnotation(Table.class).name();
		}
		return target.getSimpleName().toUpperCase();
	}

	/**
	 * Get the schema name for the given yopable target
	 * (read {@link Table} annotation or return empty string).
	 * @param target the target Yopable implementation
	 * @return the table name for the current context
	 */
	public static String getSchemaName(Class<? extends Yopable> target) {
		if(target.isAnnotationPresent(Table.class)) {
			return target.getAnnotation(Table.class).schema();
		}
		return "";
	}

	/**
	 * Get the schema name for the given yopable target
	 * (read {@link Table} annotation or return empty string).
	 * @param target the target Yopable implementation
	 * @return the table name for the current context
	 */
	public static String getQualifiedTableName(Class<? extends Yopable> target) {
		String schemaName = getSchemaName(target);
		String tableName = getTableName(target);
		return StringUtils.isBlank(schemaName) ? tableName : schemaName + Constants.DOT + tableName;
	}

	/**
	 * Get the ID field for a Yopable class.
	 * <br>
	 * For now, Yop only supports one single techical Long ID field, that might have (or not) an @Id annotation.
	 * @param clazz the Yopable class
	 * @param <T> the yopable type
	 * @return the ID field
	 * @throws YopMappingException no Yop compatible ID field found or several ones.
	 */
	public static <T extends Yopable> Field getIdField(Class<T> clazz) {
		List<Field> idFields = Reflection.getFields(clazz, Id.class);
		if(idFields.size() == 0) {
			logger.trace("No @Id field on [{}]. Assuming 'id'", clazz.getName());
			Field field = Reflection.get(clazz, "id");
			if(field != null && Long.class.isAssignableFrom(Primitives.wrap(field.getType()))) {
				return field;
			}
			throw new YopMappingException("No Long ID field in [" + clazz.getName() + "] !");
		}
		if(idFields.size() > 1) {
			throw new YopMappingException("Several @Id fields ! Only one Field of Long type can be @Id !");
		}
		Field field = idFields.get(0);
		if(!Long.class.isAssignableFrom(field.getType())) {
			throw new YopMappingException("@Id field is not Long compatible !");
		}
		return field;
	}

	/**
	 * Get the ID column name for a Yopable
	 * @param clazz the yopable class
	 * @param <T> the yopable type
	 * @return the ID column name or "ID" if the id field has no @Column annotation
	 */
	public static <T extends Yopable> String getIdColumn(Class<T> clazz) {
		Field field = getIdField(clazz);
		return field.isAnnotationPresent(Column.class) ? field.getAnnotation(Column.class).name() : "ID";
	}

	/**
	 * Read the column name for a given field, using @Column annotation
	 * @param columnField the field to read
	 * @return {@link Column#name()} or {@link Class#getName()} in upper case if no specified name on @Column.
	 */
	public static String getColumnName(Field columnField) {
		Column annotation = columnField.getAnnotation(Column.class);
		return StringUtils.isEmpty(annotation.name()) ? columnField.getName().toUpperCase() : annotation.name();
	}

	/**
	 * Read the column length for a given field, using @Column annotation
	 * @param columnField the field to read
	 * @return {@link Column#length()} (which has a default value)
	 */
	public static Integer getColumnLength(Field columnField) {
		Column annotation = columnField.getAnnotation(Column.class);
		return annotation.length();
	}

	/**
	 * Get the column data type for a given field.
	 * @param field the column field
	 * @param types the specific DBMS types
	 * @return simple link to {@link ORMTypes#getForType(Class)}
	 */
	public static String getColumnType(Field field, ORMTypes types) {
		return types.getForType(field.getType());
	}

	/**
	 * Get all the non transient, non synthetic fields of a class or its superclass that have the @NaturalId annotation.
	 * @param clazz the class
	 * @return the natural key fields
	 */
	public static List<Field> getNaturalKeyFields(Class clazz){
		List<Field> fields = new ArrayList<>();
		for(Field field : Reflection.getFields(clazz, true)){
			if(field.isAnnotationPresent(NaturalId.class) && isNotTransient(field)){
				fields.add(field);
				field.setAccessible(true);
			}
		}
		return fields;
	}

	/**
	 * Generate an unique shortened alias for the given one
	 * @param alias the alias that is too long
	 * @return an unique alphabetic alias
	 */
	public static String uniqueShortened(String alias) {
		return RandomStringUtils.randomAlphabetic(Math.min(Constants.SQL_ALIAS_MAX_LENGTH, 10));
	}
}
