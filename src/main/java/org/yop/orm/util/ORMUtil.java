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
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Constants;
import org.yop.orm.transform.ITransformer;

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

	/**
	 * Create the fully qualified ID column for a given context.
	 * @param context the target context
	 * @return the qualified ID column
	 */
	public static String getIdColumn(Context<? extends Yopable> context) {
		return context.getPath() + Constants.DOT + getIdColumn(context.getTarget());
	}

	/**
	 * Get the transformer for a given field.
	 * <ul>
	 *     <li>No transformer specified → {@link org.yop.orm.transform.VoidTransformer} singleton instance</li>
	 *     <li>Transformer specified → transformer singleton instance</li>
	 *     <li>Transformer specified but not instantiable → {@link org.yop.orm.transform.VoidTransformer}</li>
	 * </ul>
	 * @param field the field to read
	 * @return the transformer for the field. Never null.
	 */
	public static ITransformer getTransformerFor(Field field) {
		if(field.isAnnotationPresent(Column.class)) {
			Class<? extends ITransformer> transformer = field.getAnnotation(Column.class).transformer();
			try {
				return ITransformer.getTransformer(transformer);
			} catch (RuntimeException e) {
				logger.warn(
					"Could not instanciate transformer [{}] for [{}#{}]. Returning VoidTransformer.",
					transformer.getName(),
					field.getDeclaringClass(),
					field.getName()
				);
			}
		}
		return ITransformer.voidTransformer();
	}

	/**
	 * Read the value of a field.
	 * <br>
	 * If the field is a {@link Column} with a specified {@link ITransformer}, the field value is transformed,
	 * using {@link ITransformer#forSQL(Object, Column)}.
	 * @param field   the field to read
	 * @param element the element on which the field is to read
	 * @return the field value, that might have been transformed using the speficied {@link ITransformer}.
	 */
	@SuppressWarnings("unchecked")
	public static Object readField(Field field, Yopable element) {
		try {
			Object value = field.get(element);
			if(field.isAnnotationPresent(Column.class)) {
				Column column = field.getAnnotation(Column.class);
				return ITransformer.getTransformer(column.transformer()).forSQL(value, column);
			}
			return value;
		} catch (IllegalAccessException e) {
			throw new YopRuntimeException(
				"Could not read [" + field.getDeclaringClass() + "#" + field.getName()+ "] on [" + element + "] !"
			);
		}
	}

	/**
	 * See {@link Constants#USE_SEQUENCES}.
	 * @return true if the system variable 'yop.sql.sequences' is set.
	 */
	public static boolean useSequence() {
		return Constants.USE_SEQUENCES;
	}

	/**
	 * Read the sequence of an {@link Id} field.
	 * <br>
	 * What does it do ?
	 * <ul>
	 *   <li>not @Id or no sequence set → "" </li>
	 *   <li>Id field and sequence is not set to {@link Constants#DEFAULT_SEQ} → the sequence set on @Id </li>
	 *   <li>Id field and sequence is set to {@link Constants#DEFAULT_SEQ} → "seq_" + class simple name </li>
	 * </ul>
	 * @param field the field to read
	 * @return the sequence for this field, or an empty String
	 *
	 */
	public static String readSequence(Field field) {
		if(field.isAnnotationPresent(Id.class) && !StringUtils.isBlank(field.getAnnotation(Id.class).sequence())) {
			String seq = field.getAnnotation(Id.class).sequence();
			if (Constants.DEFAULT_SEQ.equals(seq)) {
				return "seq_" + field.getDeclaringClass().getSimpleName();
			}
			return seq;
		}
		return "";
	}
}
