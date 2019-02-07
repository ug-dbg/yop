package org.yop.orm.util;

import com.google.common.primitives.Primitives;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.ReflectionException;
import sun.reflect.ReflectionFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.yop.orm.util.MessageUtil.concat;

/**
 * Utility class for reflection-based method. <br>
 * YOP mostly relies on reflection to get the field values to persist. <br>
 */
@SuppressWarnings("WeakerAccess")
public class Reflection {

	private static final Logger logger = LoggerFactory.getLogger(Reflection.class);

	private static final boolean TEST_BOOL   = true;
	private static final byte    TEST_BYTE   = 111;
	private static final char    TEST_CHAR   = '*';
	private static final short   TEST_SHORT  = 42;
	private static final int     TEST_INT    = 1337;
	private static final long    TEST_LONG   = 1337666;
	private static final float   TEST_FLOAT  = 13.37f;
	private static final double  TEST_DOUBLE = 1.337;

	/**
	 * Find a class for a given class name. Do not throw checked exception.
	 * @param name         the class name
	 * @param classLoaders the class loaders to use. First match returns. Use {@link Class#forName(String)} if no match.
	 * @param <T> the target type
	 * @return the class name
	 * @throws ReflectionException instead of {@link ClassNotFoundException}
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> forName(String name, ClassLoader... classLoaders) {
		try {
			for (ClassLoader classLoader : classLoaders) {
				try {
					return (Class<T>) classLoader.loadClass(name);
				} catch (ClassNotFoundException e) {
					logger.debug("Class [{}] not found in class loader [{}]", name, classLoader, e);
				}
			}
			return (Class<T>) Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new ReflectionException("Could not find class for name [" + name + "]", e);
		}
	}

	/**
	 * Read the package name of a class. Never return null.
	 * @param clazz the class whose package is to read
	 * @return the package name or an empty string if either clazz or its package is null
	 */
	public static String packageName(Class<?> clazz) {
		return (clazz == null || clazz.getPackage() == null) ? "" : clazz.getPackage().getName();
	}


	/**
	 * Get an existing method on a given class, with the given parameters.
	 * <br>
	 * This is a call to {@link Class#getDeclaredMethod(String, Class...)}.
	 * <br>
	 * If the method does not exist, simply return null, do not throw any {@link NoSuchMethodException}.
	 * @param clazz          the class of the target method
	 * @param name           the name of the method
	 * @param parameterTypes the method parameter types.
	 * @return the target method, accessible for your convenience, or null if no method found
	 */
	public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
		try {
			Method method = clazz.getDeclaredMethod(name, parameterTypes);
			method.setAccessible(true);
			return method;
		} catch (NoSuchMethodException e) {
			logger.debug("No method [{}]#[{}] with parameter types {}", clazz, name, parameterTypes, e);
		}
		return null;
	}

	/**
	 * Find the given annotation instance on the given target class or from its class hierarchy.
	 * @param target     the target class
	 * @param annotation the annotation class
	 * @param <A> the annotation type
	 * @return the annotation instance or null if no annotation found on target class or any of its superclasses.
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A getAnnotation(Class target, Class<A> annotation) {
		Class i = target;
		while (i != null && i != Object.class) {
			if (i.isAnnotationPresent(annotation)) {
				return (A) i.getAnnotation(annotation);
			}
			i = i.getSuperclass();
		}
		return null;
	}

	/**
	 * Get all the non synthetic fields of a class. <br>
	 * Also retrieve the non synthetic fields from superclasses.
	 * @param type the target class
	 * @return the field list
	 */
	public static List<Field> getFields(Class type) {
		List<Field> result = new ArrayList<>();

		Class<?> i = type;
		while (i != null && i != Object.class) {
			for (Field field : ReflectionCache.getDeclaredFields(i)) {
				if (!field.isSynthetic()) {
					field.setAccessible(true);
					result.add(field);
				}
			}
			i = i.getSuperclass();
		}

		return result;
	}

	/**
	 * Get all non synthetic fields of a class, with a given annotation. <br>
	 * Also retrieve non synthetic fields from superclasses.
	 * @param type           the class
	 * @param withAnnotation the annotation the field must declare to be eligible
	 * @return the field list
	 */
	public static List<Field> getFields(Class type, Class<? extends Annotation> withAnnotation) {
		List<Field> result = new ArrayList<>();

		Class<?> i = type;
		while (i != null && i != Object.class) {
			for (Field field : ReflectionCache.getDeclaredFields(i)) {

				if (!field.isSynthetic() && field.isAnnotationPresent(withAnnotation)) {
					field.setAccessible(true);
					result.add(field);
				}
			}
			i = i.getSuperclass();
		}

		return result;
	}

	/**
	 * A 'toString' method for logging a field with its source class.
	 * <br>
	 * e.g : Field from SomeClass → com.package.SomeClass#fieldName
	 * @param field the field to use. Can be null.
	 * @return [fullyQualifiedClassName]#[fieldName] or "null" if the field is null.
	 */
	public static String fieldToString(Field field) {
		return field == null ? "null" : (field.getDeclaringClass().getName() + "#" + field.getName());
	}

	/**
	 * Read the value of a field on a target object.
	 * <br>
	 * This method does not throw any {@link IllegalAccessException} !
	 * @param field the field to read
	 * @param onto  the target object where to read the field
	 * @return the field value
	 * @throws ReflectionException if the field could not be read for any reason.
	 */
	public static Object readField(Field field, Object onto) {
		try {
			return field.get(onto);
		} catch (IllegalAccessException | RuntimeException e) {
			throw new ReflectionException(
				"Could not read [" + Reflection.fieldToString(field) + "] on [" + onto + "]",
				e
			);
		}
	}

	/**
	 * Read the value of a field on a target object.
	 * <br>
	 * This method does not throw any {@link IllegalAccessException} !
	 * @param fieldName the name of the field to read
	 * @param onto      the target object where to read the field
	 * @return the field value
	 * @throws ReflectionException if the field could not be read or does not exist.
	 */
	public static Object readField(String fieldName, Object onto) {
		try {
			Field field = get(onto.getClass(), fieldName);
			if (field == null) {
				throw new ReflectionException("No field [" + fieldName + "] in [" + onto.getClass().getName() + "]");
			}
			return field.get(onto);
		} catch (IllegalAccessException | RuntimeException e) {
			throw new ReflectionException(
				"Could not read [" + fieldName + "] on [" + onto + "]",
				e
			);
		}
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
			for (Field field : ReflectionCache.getDeclaredFields(i)) {
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
	 * Set the value value of a field for a given instance.
	 * <br>
	 * This method only throws {@link RuntimeException} exceptions.
	 * @param field the field to set
	 * @param onto  the target instance
	 * @param value the field value to set
	 * @throws ReflectionException exception with context, if any exception (Illegal Access or Runtime) occurs.
	 */
	public static void set(Field field, Object onto, Object value) {
		try {
			field.set(onto, value);
		} catch (IllegalAccessException | RuntimeException e) {
			throw new ReflectionException(
				"Unable to set " +
				"field [" + field.getDeclaringClass() + "#" + field.getName() + "] " +
				"value [" + value + "] " +
				"onto  [" + onto + "]",
				e
			);
		}
	}

	/**
	 * Set the value value of a field for a given instance, from another instance.
	 * <br>
	 * This method only throws {@link RuntimeException} exceptions.
	 * @param field the field to set
	 * @param from the source instance
	 * @param onto the target instance
	 * @throws ReflectionException exception with context, if any exception (Illegal Access or Runtime) occurs.
	 */
	public static void setFrom(Field field, Object from, Object onto) {
		try {
			set(field, onto, Reflection.readField(field, from));
		} catch (RuntimeException e) {
			throw new ReflectionException(
				"Unable to set " +
				"field [" + field.getDeclaringClass() + "#" + field.getName() + "] " +
				"from [" + from + "] " +
				"onto [" + onto + "]",
				e
			);
		}
	}

	/**
	 * Return the type parameter for a 1-arg generic field. <br>
	 * Throws an exception if not a 1-arg generic field.
	 *
	 * @param field the generic field.
	 * @return the type.
	 */
	public static Type get1ArgParameter(Field field){
		Type genericType = field.getGenericType();
		if (! (genericType instanceof ParameterizedType)) {
			throw new ReflectionException(concat(
				"Field [", fieldToString(field), "] is not generic. Unsupported."
			));
		}

		ParameterizedType type = (ParameterizedType) genericType;
		Type[] typeParameter = type.getActualTypeArguments();

		if(typeParameter.length != 1){
			throw new ReflectionException(concat(
				"Field [", fieldToString(field), "] has [", typeParameter.length, "] parameters. Unsupported."
			));
		}
		return
			typeParameter[0] instanceof ParameterizedType
			? ((ParameterizedType)typeParameter[0]).getRawType()
			: typeParameter[0];
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
			throw new ReflectionException(
				"Unable to create instance of [" + clazz + "]. Does it have a no-arg constructor?",
				e
			);
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
	 * @throws ReflectionException if no field matches the getter
	 */
	public static <T, R> Field findField(Class<T> clazz, Function<T, R> getter) {
		Class<?> fieldType = null;
		try {
			List<Field> fields = getFields(clazz);
			T instance = newInstanceNoArgs(clazz);

			for (Field field : fields) {
				fieldType = field.getType();
				Object testValue = newInstanceUnsafe(fieldType);
				set(field, instance, testValue);
				R fieldValue = getter.apply(instance);

				if(testValue == fieldValue && ! ClassUtils.isPrimitiveOrWrapper(fieldType)) {
					return field;
				}

				if(primitiveCheck(field, getter, instance, testValue, fieldValue)) {
					return field;
				}
			}

		} catch (RuntimeException e) {
			throw new ReflectionException(
				"Unable to find field from [" + clazz + "] " +
				"for the given accessors ! Last field type was [" + fieldType + "]",
				e
			);
		}
		throw new ReflectionException("Unable to find field from [" + clazz + "] for the given accessors !");
	}

	/**
	 * Check a primitive field value against a setter, twice, using {@link #primitiveTestValue(Class, int)}
	 * with a salt of 1.
	 * @param field      the field to check
	 * @param getter     the getter to use
	 * @param instance   the instance holding the field
	 * @param testValue  the test value, from the setter
	 * @param fieldValue the actual field value
	 * @param <T> the type holding the setter
	 * @param <R> the target type
	 * @return true if the getter actually returned the field value, twice
	 * @throws ReflectionException could not read the field
	 */
	private static <T, R> boolean primitiveCheck(
		Field field,
		Function<T, R> getter,
		T instance,
		Object testValue,
		R fieldValue) {

		if(ClassUtils.isPrimitiveOrWrapper(field.getType()) && testValue != null && testValue.equals(fieldValue)) {
			Object confirmValue = primitiveTestValue(field.getType(), 1);
			set(field, instance, confirmValue);
			return getter.apply(instance).equals(confirmValue);
		}
		return false;
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
	 * @throws ReflectionException if no field matches the getter
	 */
	public static <T, R> Field findField(Class<T> clazz, BiConsumer<T, R> setter) {
		try {
			List<Field> fields = getFields(clazz);
			T instance = newInstanceNoArgs(clazz);

			for (Field field : fields) {
				Class<?> fieldType = field.getType();

				try {
					@SuppressWarnings("unchecked")
					R testValue = (R) newInstanceUnsafe(fieldType);
					setter.accept(instance, testValue);

					Object fieldValue = Reflection.readField(field, instance);
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

		} catch (RuntimeException e) {
			throw new ReflectionException("Unable to find field from [" + clazz + "] for the given accessors !", e);
		}
		throw new ReflectionException("Unable to find field from [" + clazz + "] for the given accessors !");
	}

	/**
	 * Find the target class of a field which is a collection.
	 *
	 * @param field the (collection) field
	 * @param <T> target type
	 * @return the field target for the given collection field
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> getCollectionTarget(Field field) {
		return (Class<T>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
	}

	/**
	 * Find the target class of a field be it a collection or not.
	 *
	 * @param field the field
	 * @param <T> target type
	 * @return the field target
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> getTarget(Field field) {
		if (Collection.class.isAssignableFrom(field.getType())) {
			return getCollectionTarget(field);
		}
		return (Class<T>) field.getType();
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
	 * @param setter the setter (lambda bi consumer)
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
	 * @throws ReflectionException fake primitive or Wrapper
	 * @throws ReflectionException Unsafe was not able to instantiate the type T.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T newInstanceUnsafe(Class<T> clazz) {
		if (ClassUtils.isPrimitiveOrWrapper(clazz)) {
			return (T) primitiveTestValue(clazz, 0);
		}

		Class<? extends T> target = implementationOf(clazz);

		try {
			return newInstanceNoArgs(target);
		} catch (RuntimeException e) {
			logger.trace("Unable to use no-arg constructor of [" + clazz.getName() + "]. Fallback to Unsafe...", e);
		}

		try {
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
	 * @param salt      the amount to add to the reference test value (e.g. second test)
	 * @return the test value for the primitive type
	 */
	private static Object primitiveTestValue(Class<?> primitive, int salt) {
		Class<?> unwrapped = primitive.isPrimitive() ? primitive : Primitives.unwrap(primitive);
		boolean confirm = salt > 0;

		if(boolean.class.equals(unwrapped)) {return confirm != TEST_BOOL;}
		if(byte.class.equals(unwrapped))    {return TEST_BYTE   + salt;}
		if(char.class.equals(unwrapped))    {return TEST_CHAR   + salt;}
		if(short.class.equals(unwrapped))   {return TEST_SHORT  + salt;}
		if(int.class.equals(unwrapped))     {return TEST_INT    + salt;}
		if(long.class.equals(unwrapped))    {return TEST_LONG   + salt;}
		if(float.class.equals(unwrapped))   {return TEST_FLOAT  + salt;}
		if(double.class.equals(unwrapped))  {return TEST_DOUBLE + salt;}

		throw new ReflectionException("Primitive class [" + primitive.getName() + "] is not really primitive !");
	}

	/**
	 * Check if a type is concrete (a.k.a can be instantiated)
	 * @param clazz the class object of the type to check
	 * @param <T> the type to check
	 * @return true if clazz is neither an interface nor an abstract class
	 */
	static <T> boolean isConcrete(Class<T> clazz) {
		return !(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()));
	}

	/**
	 * Returns the first known implementation of a class.
	 * <br>
	 * It can be itself if the class {@link #isConcrete(Class)}.
	 * @param clazz the class whose implementation is sought
	 * @param <T> the class generic type
	 * @return the first implementation found, self if concrete, null if no known implementation
	 */
	private static <T> Class<? extends T> implementationOf(Class<T> clazz) {
		return ReflectionCache.implementationOf(clazz);
	}

	/**
	 * Get the constructor for the given class with the given other class as single parameter.
	 * <br>
	 * This method does not throw {@link NoSuchMethodException}. It returns null if no match.
	 * <br>
	 * This method does not wrap/unwrap primitive types : withParameter must be the exact type !
	 * <br>
	 * @param on            which class whose constructor we search
	 * @param withParameter the constructor single parameter class
	 * @param <T> the constructor target type
	 * @return the constructor, set accessible, or null if no constructor matches.
	 */
	public static <T> Constructor<T> getConstructor(Class<T> on, Class<?> withParameter) {
		try {
			Constructor<T> constructor = on.getDeclaredConstructor(withParameter);
			constructor.setAccessible(true);
			return constructor;
		} catch (NoSuchMethodException e) {
			logger.trace("Could not find constructor [{}]([{}])", on.getName(), withParameter.getName(), e);
		}
		return null;
	}
}
