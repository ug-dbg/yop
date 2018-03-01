package org.yop.orm.util;

import com.google.common.primitives.Primitives;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ClassUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.Column;
import org.yop.orm.annotations.NaturalId;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import sun.reflect.ReflectionFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.yop.orm.util.MessageUtil.concat;

/**
 * Utility class for reflection-based method. <br>
 * YOP mostly relies on reflection to get the field values to persist. <br>
 *
 * Created by ugz on 10/03/15.
 */
@SuppressWarnings("unused")
public class Reflection {

	private static final Logger logger = LoggerFactory.getLogger(Reflection.class);

	/**
	 * Reference implementations for common interfaces
	 */
	private static final Map<Class<?>, Class<?>> KNOWN_IMPLEMENTATIONS = new HashMap<Class<?>, Class<?>>() {{
		this.put(Iterable.class,   ArrayList.class);
		this.put(Collection.class, ArrayList.class);
		this.put(List.class,       ArrayList.class);
		this.put(Set.class,        HashSet.class);
		this.put(Queue.class,      LinkedList.class);
		this.put(Map.class,        HashMap.class);
	}};

	private static final boolean TEST_BOOL   = true;
	private static final byte    TEST_BYTE   = 111;
	private static final char    TEST_CHAR   = '*';
	private static final short   TEST_SHORT  = 42;
	private static final int     TEST_INT    = 1337;
	private static final long    TEST_LONG   = 1337666;
	private static final float   TEST_FLOAT  = 13.37f;
	private static final double  TEST_DOUBLE = 1.337;

	/**
	 * Get all the non transient and non synthetic fields of a class. <br>
	 * Also retrieve the non transient and non synthetic fields from superclasses.
	 * @param type the class
	 * @return the field list
	 */
	public static List<Field> getFields(Class type) {
		return getFields(type, true);
	}

	/**
	 * Get all the non synthetic fields of a class. <br>
	 * Also retrieve the non transient and non synthetic fields from superclasses.
	 * @param type         the class
	 * @param nonTransient true to exclude transient fields
	 * @return the field list
	 */
	private static List<Field> getFields(Class type, boolean nonTransient) {
		List<Field> result = new ArrayList<>();

		Class<?> i = type;
		while (i != null && i != Object.class) {
			for (Field field : i.getDeclaredFields()) {
				if (!field.isSynthetic() && (isNotTransient(field) || !nonTransient)) {
					field.setAccessible(true);
					result.add(field);
				}
			}
			i = i.getSuperclass();
		}

		return result;
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
	 * Get all non synthetic fields of a class, with a given annotation. <br>
	 * Also retrieve non synthetic fields from superclasses.
	 * @param type           the class
	 * @param withAnnotation the annotation the field must declare to be eligible
	 * @param nonTransient   if true, transient fields are excluded
	 * @return the field list
	 */
	public static List<Field> getFields(Class type, Class<? extends Annotation> withAnnotation, boolean nonTransient) {
		List<Field> result = new ArrayList<>();

		Class<?> i = type;
		while (i != null && i != Object.class) {
			for (Field field : i.getDeclaredFields()) {

				if (!field.isSynthetic() && (isNotTransient(field) || !nonTransient)
				&& field.isAnnotationPresent(withAnnotation)) {
					field.setAccessible(true);
					result.add(field);
				}
			}
			i = i.getSuperclass();
		}

		return result;
	}

	/**
	 * Get a field from a class or superclass using the field name. <br>
	 *
	 * @param type the class
	 * @param name the field name
	 * @return the found field or null.
	 */
	public static Field get(Class type, String name){
		Class<?> i = type;
		while (i != null && i != Object.class) {
			for (Field field : i.getDeclaredFields()) {
				if (!field.isSynthetic() && StringUtils.equals(field.getName(), name)) {
					field.setAccessible(true);
					return field;
				}
			}
			i = i.getSuperclass();
		}
		return null;
	}

	/**
	 * Get all the non transient, non synthetic fields of a class or its superclass that have the @Column annotation. <br>
	 * @param clazz the class
	 * @return the indexed fields
	 */
	public static List<Field> getIndexedFields(Class clazz){
		List<Field> fields = new ArrayList<>();
		for(Field field : getFields(clazz)){
			if(field.isAnnotationPresent(Column.class) && isNotTransient(field)){
				fields.add(field);
				field.setAccessible(true);
			}
		}
		return fields;
	}

	/**
	 * Get all the non transient, non synthetic fields of a class or its superclass that have the @NaturalId annotation. <br>
	 * @param clazz the class
	 * @return the natural key fields
	 */
	public static List<Field> getNaturalKeyFields(Class clazz){
		List<Field> fields = new ArrayList<>();
		for(Field field : getFields(clazz)){
			if(field.isAnnotationPresent(NaturalId.class) && isNotTransient(field)){
				fields.add(field);
				field.setAccessible(true);
			}
		}
		return fields;
	}

	/**
	 * Get all the non transient, non synthetic, non iterable fields of a class or its superclass that have the @Column annotation. <br>
	 * !! If an exception occurs, this method does not log and returns an empty list !! <br>
	 * @param clazz the object class
	 * @return the indexed non null fields
	 */
	public static List<Field> getIndexedNonIterableFields(Class clazz){
		List<Field> fields = new ArrayList<>();
		try {
			for (Field field : getFields(clazz)) {
				if(Iterable.class.isAssignableFrom(field.getType())){
					continue;
				}
				if (field.isAnnotationPresent(Column.class) && isNotTransient(field)) {
					fields.add(field);
					field.setAccessible(true);
				}
			}
		}catch (Exception e){
			return new ArrayList<>();
		}
		return fields;
	}

	/**
	 * State if a field is non transient, non synthetic, non iterable fields of a class or its superclass and have the @Column annotation. <br>
	 * @param clazz the class
	 * @param fieldName the field name
	 * @return true if the given field exists and is non transient, non synthetic, non iterable fields of a class or its superclass and have the @Column annotation
	 */
	public static boolean isIndexedNonIterable(Class clazz, String fieldName){
		for(Field f : getIndexedNonIterableFields(clazz)){
			if(StringUtils.equals(fieldName, f.getName())){
				return true;
			}
		}
		return false;
	}

	/**
	 * Get all the non transient, non synthetic, non iterable and not null fields of an object or its superclass that have the @Column annotation. <br>
	 * !! If an exception occurs, this method does not log and returns an empty list !! <br>
	 * @param o the object
	 * @return the indexed non null fields
	 */
	public static List<Field> getIndexedNonNullNonIterableFields(Object o){
		List<Field> fields = new ArrayList<>();
		if(o == null){
			return fields;
		}
		Class clazz = o.getClass();
		try {
			for (Field field : getFields(clazz)) {
				if (field.get(o) == null) {
					continue;
				}
				if(Iterable.class.isAssignableFrom(field.getType())){
					continue;
				}
				if (field.isAnnotationPresent(Column.class) && isNotTransient(field)) {
					fields.add(field);
					field.setAccessible(true);
				}
			}
		}catch (Exception e){
			return new ArrayList<>();
		}
		return fields;
	}

	/**
	 * Get all the non transient, non synthetic Yopable field of class and its superclass. <br>
	 * @param clazz the class
	 * @return the yopable fields
	 */
	public static List<Field> getYopableFields(Class clazz){
		List<Field> fields = new ArrayList<>();
		for(Field field : getFields(clazz)){
			if(Yopable.class.isAssignableFrom(field.getType()) && isNotTransient(field)){
				fields.add(field);
				field.setAccessible(true);
			}
		}
		return fields;
	}

	/**
	 * Get all the non transient, non synthetic and non Yopable field of class and its superclass. <br>
	 * @param clazz the class
	 * @return the yopable fields
	 */
	public static List<Field> getNonYopableFields(Class clazz){
		List<Field> fields = new ArrayList<>();
		for(Field field : getFields(clazz)){
			if(!Yopable.class.isAssignableFrom(field.getType()) && isNotTransient(field)){
				fields.add(field);
				field.setAccessible(true);
			}
		}
		return fields;
	}

	/**
	 * Get all the non transient, non synthetic Iterable (with a given type) field of class and its superclass. <br>
	 * @param clazz the class
	 * @return the yopable fields
	 */
	public static List<Field> getYopableIterableFields(Class clazz){
		List<Field> fields = new ArrayList<>();
		for(Field field : getFields(clazz)){
			if(Iterable.class.isAssignableFrom(field.getType())
					&& isNotTransient(field)
					&& Yopable.class.isAssignableFrom((Class<?>) get1ArgParameter(field))
					){
				field.setAccessible(true);
				fields.add(field);
			}
		}
		return fields;
	}

	/**
	 * Get all the non transient, non synthetic Iterable field of class and its superclass. <br>
	 * @param clazz the class
	 * @return the yopable fields
	 */
	public static List<Field> getIterableFields(Class clazz){
		List<Field> fields = new ArrayList<>();
		for(Field field : getFields(clazz)){
			if(Iterable.class.isAssignableFrom(field.getType()) && isNotTransient(field)){
				field.setAccessible(true);
				fields.add(field);
			}
		}
		return fields;
	}

	/**
	 * Return the type parameter for a 1-arg generic field. <br />
	 * Throws an exception if not a 1-arg generic field.
	 *
	 * @param field the generic field.
	 * @return the type.
	 */
	public static Type get1ArgParameter(Field field){
		ParameterizedType type = (ParameterizedType) field.getGenericType();
		Type[] typeParameter = type.getActualTypeArguments();

		if(typeParameter.length != 1){
			throw new YopRuntimeException(concat("Persisted field [", field.getName(), "] has [", typeParameter.length, "] parameters. Unsupported."));
		}
		return typeParameter[0];
	}

	/**
	 * Check if a field has the transient keyword. <br>
	 * @param field the field to check
	 * @return false if the transient keyword
	 */
	public static boolean isNotTransient(Field field){
		return !Modifier.isTransient(field.getModifiers());
	}

	/**
	 * Instantiate a new object using 0-arg constructor. <br>
	 * @param clazz the object class to instantiate
	 * @param <T> the object type
	 * @return a new instance of the object
	 */
	public static <T> T newInstanceNoArgs(Class<T> clazz){
		try {
			Constructor<T> c = clazz.getDeclaredConstructor();
			c.setAccessible(true);
			return c.newInstance();
		} catch (Exception e) {
			throw new YopRuntimeException("Unable to create instance of [" + clazz + "]. Does it have a no-arg constructor?", e);
		}
	}

	/**
	 * Find a field on a class whose value is returned by the given getter operation.
	 * <br>
	 * <b>The field can be transient !</b>
	 * @param clazz  the class holding the field and getter
	 * @param getter the getter operation
	 * @param <T> the class type
	 * @param <R> the field type
	 * @return the field found.
	 * @throws YopRuntimeException if no field matches the getter
	 */
	public static <T, R> Field findField(Class<T> clazz, Function<T, R> getter) {
		try {
			List<Field> fields = getFields(clazz, false);
			T instance = newInstanceNoArgs(clazz);

			for (Field field : fields) {
				Class<?> fieldType = field.getType();
				Object testValue = newInstanceUnsafe(fieldType);
				field.set(instance, testValue);
				R fieldValue = getter.apply(instance);

				if(testValue == fieldValue) {
					return field;
				}

				if(ClassUtils.isPrimitiveOrWrapper(fieldType) && testValue != null && testValue.equals(fieldValue)) {
					return field;
				}
			}

		} catch (IllegalAccessException | RuntimeException e) {
			throw new YopRuntimeException("Unable to find field from [" + clazz + "] for the given accessors !", e);
		}
		throw new YopRuntimeException("Unable to find field from [" + clazz + "] for the given accessors !");
	}

	/**
	 * Find a field on a class whose value is returned by the given getter operation.
	 * <br>
	 * <b>The field can be transient !</b>
	 * @param clazz  the class holding the field and getter
	 * @param setter the setter operation
	 * @param <T> the class type
	 * @param <R> the field type
	 * @return the field found.
	 * @throws YopRuntimeException if no field matches the getter
	 */
	public static <T, R> Field findField(Class<T> clazz, BiConsumer<T, R> setter) {
		try {
			List<Field> fields = getFields(clazz, false);
			T instance = newInstanceNoArgs(clazz);

			for (Field field : fields) {
				Class<?> fieldType = field.getType();

				try {
					@SuppressWarnings("unchecked")
					R testValue = (R) newInstanceUnsafe(fieldType);
					setter.accept(instance, testValue);

					Object fieldValue = field.get(instance);
					if(testValue == fieldValue) {
						return field;
					}

					if(ClassUtils.isPrimitiveOrWrapper(fieldType) && testValue != null && testValue.equals(fieldValue)) {
						return field;
					}
				} catch (RuntimeException e) {
					logger.trace("Wrong field for setter ! Next guess maybe :-)", e);
				}
			}

		} catch (IllegalAccessException | RuntimeException e) {
			throw new YopRuntimeException("Unable to find field from [" + clazz + "] for the given accessors !", e);
		}
		throw new YopRuntimeException("Unable to find field from [" + clazz + "] for the given accessors !");
	}

	/**
	 * Find the target class of a field which is a collection.
	 *
	 * @param field the (collection) field
	 * @param <S> source type
	 * @param <T> target type
	 * @return the field type for the given getter
	 */
	@SuppressWarnings("unchecked")
	public static <S, T> Class<T> getCollectionTarget(Field field) {
		return (Class<T>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
	}

	/**
	 * Find the target class of a getter.
	 * @param source the source class, holding the field
	 * @param getter the getter (lambda function)
	 * @param <S> source type
	 * @param <T> target type
	 * @return the field type for the given getter
	 */
	@SuppressWarnings("unchecked")
	public static <S, T> Class<T> getGetterTarget(
		Class<S> source,
		Function<S, T> getter) {

		return (Class<T>) findField(source, getter).getType();
	}

	/**
	 * Find the target class of a setter.
	 * @param source the source class, holding the field
	 * @param setter the setter (lambda bi consummer)
	 * @param <S> source type
	 * @param <T> target type
	 * @return the field type for the given setter
	 */
	@SuppressWarnings("unchecked")
	public static <S, T> Class<T> getSetterTarget(
		Class<S> source,
		BiConsumer<S, T> setter) {

		return (Class<T>) findField(source, setter).getType();
	}

	/**
	 * Find the target class of a getter when the field is a collection.
	 * Example :
	 * <br>
	 * Considering <i> {@code Set<Pojo> getChildren()}</i>
	 * this function will return the Pojo class for the lambda <i>{@code ::getChildren}</i>
	 *
	 * @param source the source class, holding the field
	 * @param getter the getter (lambda function)
	 * @param <S> source type
	 * @param <T> target type
	 * @return the field type for the given getter
	 */
	@SuppressWarnings("unchecked")
	public static <S, T> Class<T> getGetterCollectionTarget(
		Class<S> source,
		Function<S, ? extends Collection<T>> getter) {

		return (Class<T>) ((ParameterizedType) findField(source, getter).getGenericType()).getActualTypeArguments()[0];
	}

	/**
	 * Find the target class of a setter when the field is a collection.
	 * Example :
	 * <br>
	 * Considering <i>{@code void setChildren(Set<Pojo>)}</i>
	 * this function will return the Pojo class for the lambda <i>{@code ::setChildren}</i>
	 *
	 * @param source the source class, holding the field
	 * @param setter the setter (lambda function)
	 * @param <S> source type
	 * @param <T> target type
	 * @return the field type for the given setter
	 */
	@SuppressWarnings("unchecked")
	public static <S, T> Class<T> getSetterCollectionTarget(
		Class<S> source,
		BiConsumer<S, ? extends Collection<T>> setter) {

		return (Class<T>) ((ParameterizedType) findField(source, setter).getGenericType()).getActualTypeArguments()[0];
	}

	/**
	 * Return a new instance of a given class, for field testing purposes (see {@link #findField(Class, Function)}.
	 * <ul>
	 *     <li>Check if primitive or Wrapper → return test value (e.g {@link #TEST_LONG}</li>
	 *     <li>Find a concrete implementation of T if required (T is interface or abstract)</li>
	 *     <li>No arg constructor → new instance</li>
	 *     <li>Use the infamous Unsafe → new instance</li>
	 * </ul>
	 * @param clazz the class of the type to instantiate
	 * @param <T> the type to instantiate
	 * @return a new instance of the type T
	 * @throws YopRuntimeException fake primitive or Wrapper
	 * @throws YopRuntimeException Unsafe was not able to instantiate the type T.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T newInstanceUnsafe(Class<T> clazz) {
		if (ClassUtils.isPrimitiveOrWrapper(clazz)) {
			return (T) primitiveTestValue(clazz);
		}

		Class<? extends T> target = implementationOf(clazz);

		try {
			return newInstanceNoArgs(target);
		} catch (RuntimeException e) {
			logger.trace("Unable to use no-arg constructor of [" + clazz.getName() + "]. Fallback to Unsafe...", e);
		}

		try {
			@SuppressWarnings("unchecked")
			Constructor<T> silentConstructor = (Constructor<T>) ReflectionFactory
				.getReflectionFactory()
				.newConstructorForSerialization(target, Object.class.getDeclaredConstructor());
			silentConstructor.setAccessible(true);
			return silentConstructor.newInstance();
		} catch (ReflectiveOperationException | RuntimeException e) {
			throw new RuntimeException("Unable to unsafe create instance of [" + clazz.getName() + "] !", e);
		}
	}

	/**
	 * Returns a test value (see {@link #TEST_LONG} for instance) for the given primitive type.
	 * @param primitive the primitive type or Wrapper.
	 * @return the test value for the primitive type
	 */
	private static Object primitiveTestValue(Class<?> primitive) {
		Class<?> unwrapped = primitive.isPrimitive() ? primitive : Primitives.unwrap(primitive);

		if(boolean.class.equals(unwrapped)) {return TEST_BOOL;}
		if(byte.class.equals(unwrapped))    {return TEST_BYTE;}
		if(char.class.equals(unwrapped))    {return TEST_CHAR;}
		if(short.class.equals(unwrapped))   {return TEST_SHORT;}
		if(int.class.equals(unwrapped))     {return TEST_INT;}
		if(long.class.equals(unwrapped))    {return TEST_LONG;}
		if(float.class.equals(unwrapped))   {return TEST_FLOAT;}
		if(double.class.equals(unwrapped))  {return TEST_DOUBLE;}

		throw new YopRuntimeException("Primitive class [" + primitive.getName() + "] is not really primitive !");
	}

	/**
	 * Check if a type is concrete (a.k.a can be instantiated)
	 * @param clazz the class object of the type to check
	 * @param <T> the type to check
	 * @return true if clazz is neither an interface nor an abstract class
	 */
	private static <T> boolean isConcrete(Class<T> clazz) {
		return !(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()));
	}

	/**
	 * Returns the first known implementation of a class.
	 * <br>
	 * It can be itself if the class {@link #isConcrete(Class)}.
	 * <br>
	 * Several strategies :
	 * <ul>
	 *     <li>concrete → return the class itself</li>
	 *     <li>{@link #KNOWN_IMPLEMENTATIONS} has a reference implementation → go for it</li>
	 *     <li>
	 *         else → use {@link Reflections} to find all the subtypes in the whole context.
	 *         <br>
	 *         Take the first concrete sub type, add it to the {@link #KNOWN_IMPLEMENTATIONS} and return !
	 *     </li>
	 * </ul>
	 * @param clazz the class whose implementation is seeked
	 * @param <T> the class generic type
	 * @return the first implementation found, self if concrete, null if no knwon implementation
	 */
	@SuppressWarnings("unchecked")
	private static <T> Class<? extends T> implementationOf(Class<T> clazz) {
		if(isConcrete(clazz)) {
			return clazz;
		}

		if(KNOWN_IMPLEMENTATIONS.containsKey(clazz)){
			return (Class<? extends T>) KNOWN_IMPLEMENTATIONS.get(clazz);
		}

		Set<Class<? extends T>> subTypes = new Reflections().getSubTypesOf(clazz);
		Class<? extends T> impl = subTypes.stream().filter(Reflection::isConcrete).findFirst().orElse(null);
		KNOWN_IMPLEMENTATIONS.put(clazz, impl);
		return impl;
	}

	public static Object transformInto(Object what, Class<?> into) {
		if(what == null || what.getClass().isAssignableFrom(into)) {
			return what;
		}

		if(String.class.equals(into)) {
			return String.valueOf(what);
		}

		if(what instanceof String) {
			if (Instant.class.isAssignableFrom(into)) {
				return Instant.parse((CharSequence) what);
			}
			if (Date.class.isAssignableFrom(into)) {
				return new Date(Instant.parse((CharSequence) what).toEpochMilli());
			}
			if (Calendar.class.isAssignableFrom(into)) {
				Calendar instance = Calendar.getInstance();
				instance.setTime(new Date(Instant.parse((CharSequence) what).toEpochMilli()));
			}
		}

		try {
			return into.cast(what);
		} catch (ClassCastException e) {
			logger.trace("Could not cast[" + what + "] into [" + into.getName() + "]", e);
		}

		try {
			Method valueOf = into.getDeclaredMethod("valueOf", String.class);
			return valueOf.invoke(null, String.valueOf(what));
		} catch (NoSuchMethodException e) {
			logger.trace("Could not find valueOf(String) on [" + into.getName() + "]", e);
		} catch (IllegalAccessException | InvocationTargetException e) {
			logger.trace("Could not invoke valueOf(String) on [" + into.getName() + "]", e);
		}

		try {
			Constructor<?> constructor = into.getDeclaredConstructor(what.getClass());
			return constructor.newInstance(what);
		} catch (NoSuchMethodException e) {
			logger.trace("Could not find valueOf(String) on [" + into.getName() + "]", e);
		} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
			logger.trace("Could not invoke valueOf(String) on [" + into.getName() + "]", e);
		}

		return what;
	}
}
