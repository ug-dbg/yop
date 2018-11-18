package org.yop.orm.util;

import com.google.common.primitives.Primitives;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.*;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;
import org.yop.orm.transform.ITransformer;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

import static org.yop.orm.util.Reflection.isNotTransient;

/**
 * Some utility methods to read ORM annotations, find ID field, column type...
 */
public class ORMUtil {

	private static final Logger logger = LoggerFactory.getLogger(ORMUtil.class);

	/** [Yopable class → target name] cache map : {@link Class#getSimpleName()} can be a bit slow */
	private static final Map<Class, String> TARGET_NAMES = new HashMap<>();

	/** [Field → parametrized type] cache map : {@link Field#getGenericType()} can be a bit slow */
	private static final Map<Field, Class> FIELD_PARAMETRIZED_TYPES = new HashMap<>();

	/**
	 * It is useful to know if a {@link Yopable} field is a Collection or a Yopable. Or neither.
	 */
	enum FieldType {
		COLLECTION, YOPABLE, OTHER;

		public static FieldType fromField(Field field) {
			if (Collection.class.isAssignableFrom(field.getType())) {
				return COLLECTION;
			}
			if (Yopable.class.isAssignableFrom(field.getType())) {
				return YOPABLE;
			}
			return OTHER;
		}
	}

	/**
	 * Get the table name for the given yopable target
	 * (read {@link Table} annotation or return class name to upper case).
	 * @param target the target Yopable implementation
	 * @return the table name for the current context
	 */
	public static String getTableName(Class<? extends Yopable> target) {
		Table table = Reflection.getAnnotation(target, Table.class);
		if(table != null) {
			return table.name();
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
		Table table = Reflection.getAnnotation(target, Table.class);
		if(table != null) {
			return table.schema();
		}
		return "";
	}

	/**
	 * Get the qualified name for the given yopable target
	 * (read {@link Table} annotation or return empty string).
	 * @param target the target Yopable implementation
	 * @return the table name for the current context
	 */
	public static String getTableQualifiedName(Class<? extends Yopable> target) {
		Table table = Reflection.getAnnotation(target, Table.class);
		if(table != null) {
			return MessageUtil.join(".", table.schema(), table.name());
		}
		return "";
	}

	/**
	 * Get the qualified name for the given join table annotation (table name prefixed with schema name if applicable)
	 * @param joinTable the join table annotation
	 * @return the fully qualified table name for the given annotation
	 */
	public static String getJoinTableQualifiedName(JoinTable joinTable) {
		return MessageUtil.join(".", joinTable.schema(), joinTable.table());
	}

	/**
	 * Return a name for a given target class that will be used in a query to create a context.
	 * <br>
	 * This is different from {@link #getTableName(Class)} which determine the Table associated to a class.
	 * <br><br>
	 * For now this is simply {@link Class#getSimpleName()}.
	 * <br>
	 * Restriction is you cannot use 2 classes with the same name in a request.
	 * I don't feel it is totally absurd.
	 * <br><br>
	 * This method first checks the cache map {@link #TARGET_NAMES}.
	 * It adds the computed value and adds it to the cache map if it was not yet known.
	 * @param target the target class
	 * @param <T> the target class type ({@link Yopable})
	 * @return the target class context name
	 */
	public static <T extends Yopable> String getTargetName(Class<T> target) {
		if (!TARGET_NAMES.containsKey(target)) {
			TARGET_NAMES.put(target, target.getSimpleName());
		}
		return TARGET_NAMES.get(target);
	}

	/**
	 * Get the schema name for the given yopable target
	 * (read {@link Table} annotation or return empty string).
	 * @param target the target Yopable implementation
	 * @param config the SQL config (sql separator, use batch inserts...)
	 * @return the table name for the current context
	 */
	public static String getQualifiedTableName(Class<? extends Yopable> target, Config config) {
		String schemaName = getSchemaName(target);
		String tableName = getTableName(target);
		return StringUtils.isBlank(schemaName) ? tableName : schemaName + config.dot() + tableName;
	}

	/**
	 * Get the ID field for a Yopable class.
	 * <br>
	 * For now, Yop only supports one single technical Long ID field, that might have (or not) an @Id annotation.
	 * @param clazz the Yopable class
	 * @param <T> the yopable type
	 * @return the ID field, set accessible.
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
		field.setAccessible(true);
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
		// enum ? Read the strategy
		if (field.getType().isEnum() && field.isAnnotationPresent(Column.class)) {
			switch (field.getAnnotation(org.yop.orm.annotations.Column.class).enum_strategy()) {
				case ORDINAL: return types.getForType(Integer.class);
				case NAME:
				default: return types.getForType(String.class);
			}
		}
		return types.getForType(field.getType());
	}

	/**
	 * Read the target type of a Collection relationship.
	 * <br>
	 * Example : {@code ArrayList<Pojo> → Pojo.class}
	 * <br><br>
	 * This method checks the cache map {@link #FIELD_PARAMETRIZED_TYPES}.
	 * It adds the field type to the cache map if it not yet known.
	 * @param field the field to read
	 * @param <T> the target type
	 * @return the target class
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> getRelationFieldType(Field field) {
		if (! FIELD_PARAMETRIZED_TYPES.containsKey(field)) {
			FIELD_PARAMETRIZED_TYPES.put(
				field,
				(Class<T>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]
			);
		}
		return FIELD_PARAMETRIZED_TYPES.get(field);
	}

	/**
	 * Check if the given field is a column that must not be null.
	 * @param field the column field
	 * @return true if the field has a {@link Column} annotation and {@link Column#not_null()} is true
	 */
	public static boolean isColumnNotNullable(Field field) {
		if(!field.isAnnotationPresent(Column.class)) {
			return false;
		}
		Column annotation = field.getAnnotation(Column.class);
		return annotation.not_null();
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
	public static String uniqueShortened(String alias, Config config) {
		return RandomStringUtils.randomAlphabetic(Math.min(config.aliasMaxLength(), 10));
	}

	/**
	 * Create the fully qualified ID column for a given context.
	 * @param context the target context
	 * @return the qualified ID column
	 */
	public static String getIdColumn(Context<? extends Yopable> context, Config config) {
		return context.getPath(getIdField(context.getTarget()), config);
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
					"Could not instantiate transformer [{}] for [{}#{}]. Returning VoidTransformer.",
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
	 * @return the field value, that might have been transformed using the specified {@link ITransformer}.
	 */
	@SuppressWarnings("unchecked")
	public static Object readField(Field field, Yopable element) {
		Object value = Reflection.readField(field, element);
		if(field.isAnnotationPresent(Column.class)) {
			Column column = field.getAnnotation(Column.class);
			return ITransformer.getTransformer(column.transformer()).forSQL(value, column);
		}
		return value;
	}

	/**
	 * Read the sequence of an {@link Id} field.
	 * <br>
	 * What does it do ?
	 * <ul>
	 *   <li>not @Id or no sequence set → "" </li>
	 *   <li>Id field and sequence is not set to {@link Config#defaultSequence()} → the sequence set on @Id </li>
	 *   <li>Id field and sequence is set to {@link Config#defaultSequence()} → "seq_" + class simple name </li>
	 * </ul>
	 * @param field the field to read
	 * @return the sequence for this field, or an empty String
	 *
	 */
	public static String readSequence(Field field, Config config) {
		if(field.isAnnotationPresent(Id.class) && !StringUtils.isBlank(field.getAnnotation(Id.class).sequence())) {
			String seq = field.getAnnotation(Id.class).sequence();
			if (config.defaultSequence().equals(seq)) {
				return "seq_" + field.getDeclaringClass().getSimpleName();
			}
			return seq;
		}
		return "";
	}

	/**
	 * Is this field a {@link FieldType#COLLECTION} ?
	 * <br>
	 * This method uses a field type cache : {@link ORMUtilCache#FIELD_TYPES}.
	 * @param field the field to check
	 * @return true if a {@link Collection} is assignable from the field type.
	 */
	public static boolean isCollection(Field field) {
		return ORMUtilCache.isOfType(field, FieldType.COLLECTION);
	}

	/**
	 * Is this field a {@link FieldType#YOPABLE} ?
	 * <br>
	 * This method uses a field type cache : {@link ORMUtilCache#FIELD_TYPES}.
	 * @param field the field to check
	 * @return true if a {@link Yopable} is assignable from the field type.
	 */
	public static boolean isYopable(Field field) {
		return ORMUtilCache.isOfType(field, FieldType.YOPABLE);
	}

	/**
	 * Get the joined fields ({@link org.yop.orm.annotations.JoinColumn} and {@link org.yop.orm.annotations.JoinTable}).
	 * <br>
	 * This method uses a field type cache : {@link ORMUtilCache#JOINED_FIELDS}.
	 * @param clazz the given class
	 * @return all the @JoinColumn/@JoinTable fields from the given class
	 */
	public static Collection<Field> getJoinedFields(Class clazz) {
		return ORMUtilCache.getJoinedFields(clazz);
	}

	/**
	 * Get all the fields with either a {@link JoinTable} or {@link JoinColumn} annotation - be they transient or not.
	 * @param clazz the clazz to inspect for @JoinTable/@JoinColumn fields
	 * @return the matching fields
	 */
	public static List<Field> joinedFields(Class clazz) {
		return ListUtils.union(joinTableFields(clazz), joinColumnFields(clazz));
	}

	/**
	 * Get all the fields with either a {@link JoinTable} or {@link JoinColumn} annotation that are not transient.
	 * @param clazz the clazz to inspect for @JoinTable/@JoinColumn fields
	 * @return the matching fields
	 */
	public static List<Field> nonTransientJoinedFields(Class clazz) {
		return ListUtils.union(nonTransientJoinTableFields(clazz), nonTransientJoinColumnFields(clazz));
	}

	/**
	 * Get all the fields with a {@link JoinTable} annotation - be they transient or not.
	 * @param clazz the clazz to inspect for @JoinTable fields
	 * @return the matching fields
	 */
	public static List<Field> joinTableFields(Class clazz) {
		return Reflection.getFields(clazz, JoinTable.class, false);
	}

	/**
	 * Get all the fields with a {@link JoinTable} annotation that are not transient.
	 * @param clazz the clazz to inspect for @JoinTable fields
	 * @return the matching fields
	 */
	public static List<Field> nonTransientJoinTableFields(Class clazz) {
		return Reflection.getFields(clazz, JoinTable.class, true);
	}

	/**
	 * Get all the fields with a {@link JoinColumn} annotation - be they transient or not.
	 * @param clazz the clazz to inspect for @JoinColumn fields
	 * @return the matching fields
	 */
	public static List<Field> joinColumnFields(Class clazz) {
		return Reflection.getFields(clazz, JoinColumn.class, false);
	}

	/**
	 * Get all the fields with a {@link JoinColumn} annotation which target a Yopable - be they transient or not.
	 * @param clazz the clazz to inspect for @JoinColumn fields
	 * @return the matching fields
	 */
	public static List<Field> joinColumnYopableFields(Class clazz) {
		return Reflection.getFields(clazz, JoinColumn.class, false)
			.stream()
			.filter(f -> Yopable.class.isAssignableFrom(f.getType()))
			.collect(Collectors.toList());
	}

	/**
	 * Get all the fields with a {@link JoinColumn} annotation that are not transient.
	 * @param clazz the clazz to inspect for @JoinColumn fields
	 * @return the matching fields
	 */
	public static List<Field> nonTransientJoinColumnFields(Class clazz) {
		return Reflection.getFields(clazz, JoinColumn.class, true);
	}
}
