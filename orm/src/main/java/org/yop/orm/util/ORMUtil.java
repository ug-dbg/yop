package org.yop.orm.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.*;
import org.yop.orm.exception.YopMappingException;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Config;
import org.yop.orm.transform.ITransformer;
import org.yop.reflection.Reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Some utility methods to read ORM annotations, find ID field, column type...
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ORMUtil {

	private static final Logger logger = LoggerFactory.getLogger(ORMUtil.class);

	/** [Yopable class → target name] cache map : {@link Class#getSimpleName()} can be a bit slow */
	private static final Map<Class, String> TARGET_NAMES = new HashMap<>();

	/** [Field → parametrized type] cache map : {@link Field#getGenericType()} can be a bit slow */
	private static final Map<Field, Class> FIELD_PARAMETRIZED_TYPES = new HashMap<>();

	/**
	 * It is useful to know if a {@link org.yop.orm.model.Yopable} field is a Collection or a Yopable. Or neither.
	 */
	enum FieldType {
		COLLECTION, YOPABLE, OTHER;

		public static FieldType fromField(Field field) {
			if (Collection.class.isAssignableFrom(field.getType())) {
				return COLLECTION;
			}
			if (isYopable(field.getType())) {
				return YOPABLE;
			}
			return OTHER;
		}
	}

	/**
	 * A target class is considered 'Yopable'
	 * if it either implements {@link org.yop.orm.model.Yopable} or is {@link Table} annotated.
	 * <br>
	 * (For now, it MUST implement Yopable, but it SHOULD NOT be mandatory).
	 * @param target the target class to check
	 * @return true if the class is considered Yopable
	 */
	public static boolean isYopable(Class target) {
		return org.yop.orm.model.Yopable.class.isAssignableFrom(target) || target.isAnnotationPresent(Table.class);
	}

	/**
	 * Find all the Yopable implementations visible from a given class loader.
	 * <br>
	 * Uses {@link Reflections}
	 * @param classLoader the class loader to use
	 * @return a set of Yopable implementations
	 */
	public static Set<Class> yopables(ClassLoader classLoader) {
		Reflections reflections = new Reflections("", classLoader);
		return Sets.union(
			reflections.getSubTypesOf(org.yop.orm.model.Yopable.class),
			reflections.getTypesAnnotatedWith(Table.class)
		);
	}

	/**
	 * Generate a script (a list of SQL queries)
	 * that can be used to prepare a DB for the Yopable objects of a given package.
	 * <br><b>⚠⚠⚠  i.e. Every table concerned by the package prefix will be dropped in the script ! ⚠⚠⚠ </b>
	 * @param packageName the Yopable classes package name
	 * @param config      the SQL config (dialect, sql separator, use batch inserts...)
	 * @param classLoader   the class loader to use
	 * @return the SQL script, as an ordered list of SQL queries to run.
	 */
	public static List<String> generateScript(String packageName, Config config, ClassLoader classLoader) {
		Set<org.yop.orm.gen.Table> tables = org.yop.orm.gen.Table.findAllInClassPath(packageName, classLoader, config);
		List<String> script = new ArrayList<>();

		// Relation tables must be deleted first
		for (org.yop.orm.gen.Table table : Lists.reverse(new ArrayList<>(tables))) {
			script.add(table.toSQLDROP());
		}

		// Relation tables must be created last. See org.yop.orm.gen.Table#COMPARATOR
		// Also add the other SQL queries (e.g. sequences)
		for (org.yop.orm.gen.Table table : tables) {
			script.add(table.toSQL());
			script.addAll(table.otherSQL());
		}

		return script;
	}

	/**
	 * Get the table name for the given yopable target
	 * (read {@link Table} annotation or return class name to upper case).
	 * @param target the target Yopable implementation
	 * @return the table name for the current context
	 */
	public static String getTableName(Class target) {
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
	public static String getSchemaName(Class target) {
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
	public static String getTableQualifiedName(Class target) {
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
	 * For now this is simply "yop_" + {@link Class#getSimpleName()}.
	 * The 'yop_' prefix prevents from using reserved words.
	 * <br>
	 * Restriction is you cannot use 2 classes with the same name in a request.
	 * I don't feel it is totally absurd.
	 * <br><br>
	 * This method first checks the cache map {@link #TARGET_NAMES}.
	 * It adds the computed value and adds it to the cache map if it was not yet known.
	 * @param target the target class
	 * @return the target class context name
	 */
	public static String getTargetName(Class target) {
		if (!TARGET_NAMES.containsKey(target)) {
			TARGET_NAMES.put(target, "yop_" + target.getSimpleName());
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
	public static String getQualifiedTableName(Class target, Config config) {
		String schemaName = getSchemaName(target);
		String tableName = getTableName(target);
		return StringUtils.isBlank(schemaName) ? tableName : schemaName + config.dot() + tableName;
	}

	/**
	 * Get the ID field for a Yopable class.
	 * <br>
	 * For now, Yop only supports one single technical Long ID field, that might have (or not) an @Id annotation.
	 * @param clazz the Yopable class
	 * @return the ID field, set accessible.
	 * @throws YopMappingException no Yop compatible ID field found or several ones.
	 */
	public static Field getIdField(Class clazz) {
		List<Field> idFields = getFields(clazz, Id.class);
		if(idFields.size() == 0) {
			logger.trace("No @Id field on [{}]. Assuming 'id'", clazz.getName());
			Field field = Reflection.get(clazz, "id");
			if(field != null && Comparable.class.isAssignableFrom(Primitives.wrap(field.getType()))) {
				return field;
			}
			throw new YopMappingException("No Comparable ID field in [" + clazz.getName() + "] !");
		}
		if(idFields.size() > 1) {
			throw new YopMappingException("Several @Id fields ! Only one Comparable Field can be @Id !");
		}
		Field field = idFields.get(0);
		field.setAccessible(true);
		return field;
	}

	/**
	 * Is this field the ID field of its declaring class ?
	 * <br>
	 * @param field the Yopable field to test
	 * @return true if the given field is the ID field of its declaring class
	 */
	@SuppressWarnings("unchecked")
	public static boolean isIdField(Field field) {
		return getIdField(field.getDeclaringClass()) == field;
	}

	/**
	 * Check if the ID for this class is autogen.
	 * @param clazz the target class
	 * @return true if no @Id field (ID is considered autogen) or @Id with autoincrement or non empty sequence.
	 */
	public static boolean isAutogenId(Class clazz) {
		Field idField = getIdField(clazz);
		Id id = idField.getAnnotation(Id.class);
		return id == null || id.autoincrement() || StringUtils.isNotBlank(id.sequence());
	}

	/**
	 * Get all the non synthetic fields of a class. <br>
	 * Also retrieve the non transient and non synthetic fields from superclasses.
	 * @param type         the class
	 * @param nonTransient true to exclude transient fields
	 * @return the field list
	 */
	public static List<Field> getFields(Class type, boolean nonTransient) {
		return Reflection
			.getFields(type)
			.stream()
			.filter(field -> (isNotTransient(field) || !nonTransient))
			.collect(Collectors.toList());
	}

	/**
	 * Get all the non transient and non synthetic fields of a class, with a given annotation. <br>
	 * Also retrieve the non transient and non synthetic fields from superclasses.
	 * @param type           the class
	 * @param withAnnotation the annotation the field must declare to be eligible
	 * @return the field list
	 */
	public static List<Field> getFields(Class type, Class<? extends Annotation> withAnnotation) {
		return getFields(type, withAnnotation, true);
	}

	/**
	 * Get all the non transient and non synthetic @Column fields,
	 * including the id field, even if it has no @Column annotation.
	 * <br>
	 * Also retrieve the non transient and non synthetic column fields from superclasses.
	 * @param type the target class
	 * @return the @Column field list
	 */
	public static Set<Field> getColumnFields(Class type) {
		Set<Field> fields = new HashSet<>(getFields(type, Column.class, true));
		fields.add(getIdField(type));
		return fields;
	}

	/**
	 * Get all non synthetic fields of a class, with a given annotation. <br>
	 * Also retrieve non synthetic fields from superclasses.
	 * @param type           the class
	 * @param withAnnotation the annotation the field must declare to be eligible
	 * @param nonTransient   if true, transient fields are excluded
	 * @return the field list
	 */
	public static List<Field> getFields(Class type, Class<? extends Annotation> withAnnotation, boolean nonTransient) {
		return Reflection
			.getFields(type, withAnnotation)
			.stream()
			.filter(field -> (isNotTransient(field) || !nonTransient))
			.collect(Collectors.toList());
	}

	/**
	 * Get the ID column name for a Yopable
	 * @param clazz the yopable class
	 * @return the ID column name or "ID" if the id field has no @Column annotation
	 */
	public static String getIdColumn(Class clazz) {
		Field field = getIdField(clazz);
		return field.isAnnotationPresent(Column.class) ? field.getAnnotation(Column.class).name() : "ID";
	}

	/**
	 * Read the column name for a given field, using @Column annotation
	 * @param columnField the field to read
	 * @return {@link Column#name()} or the upper case field name if no @Column or no specified name on @Column.
	 */
	public static String getColumnName(Field columnField) {
		Column annotation = columnField.getAnnotation(Column.class);
		return annotation == null || StringUtils.isEmpty(annotation.name())
			? columnField.getName().toUpperCase()
			: annotation.name();
	}

	/**
	 * Read the column length for a given field, using @Column annotation
	 * @param columnField the field to read
	 * @param config      the default value is read from config if length is not configured in @Column.
	 * @return {@link Column#length()} (if > 0) or the default value parameter
	 */
	public static Integer getColumnLength(Field columnField, Config config) {
		return getColumnLength(columnField.getAnnotation(Column.class), config);
	}

	/**
	 * Read the column length for a given field, using @Column annotation
	 * @param config the default value is read from config if length is not configured in @Column.
	 * @return {@link Column#length()} (if > 0) or the default value parameter
	 */
	public static Integer getColumnLength(Column annotation, Config config) {
		return annotation == null || annotation.length() <= 0 ? config.defaultColumnLength() : annotation.length();
	}

	/**
	 * Get the column data type for a given field.
	 * @param field the column field
	 * @return the field type, unless an enum type (read {@link Column#enum_strategy()} → Integer/String)
	 */
	public static Class getColumnType(Field field) {
		// enum ? Read the strategy
		if (field.getType().isEnum() && field.isAnnotationPresent(Column.class)) {
			switch (field.getAnnotation(org.yop.orm.annotations.Column.class).enum_strategy()) {
				case ORDINAL: return Integer.class;
				case NAME:
				default: return String.class;
			}
		}
		return field.getType();
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
		if (ORMUtil.getIdField(field.getDeclaringClass()) == field) {
			return true;
		}
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
		for(Field field : getFields(clazz, true)){
			if(field.isAnnotationPresent(NaturalId.class) && isNotTransient(field)){
				fields.add(field);
				field.setAccessible(true);
			}
		}
		return fields;
	}

	/**
	 * Generate an unique shortened alias for the given one
	 * @param alias the alias that is too long. Unused for now.
	 * @return an unique alphabetic alias
	 */
	@SuppressWarnings("unused")
	public static String uniqueShortened(String alias, Config config) {
		return RandomStringUtils.randomAlphabetic(Math.min(config.aliasMaxLength(), 10));
	}

	/**
	 * Create the fully qualified ID column for a given context.
	 * @param context the target context
	 * @return the qualified ID column
	 */
	public static String getIdColumn(Context context, Config config) {
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
	 * Check if a field has the transient keyword or a {@link YopTransient} annotation. <br>
	 * @param field the field to check
	 * @return false if either the transient keyword or the YopTransient annotation is set
	 */
	public static boolean isNotTransient(Field field){
		return !Modifier.isTransient(field.getModifiers()) && !field.isAnnotationPresent(YopTransient.class);
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
	 * @return true if field type is considered {@link FieldType#YOPABLE}.
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
}
