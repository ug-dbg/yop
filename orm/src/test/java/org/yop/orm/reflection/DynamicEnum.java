package org.yop.orm.reflection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.reflection.Reflection;
import sun.reflect.ConstructorAccessor;
import sun.reflect.FieldAccessor;
import sun.reflect.ReflectionFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * From :
 * <a href="https://www.niceideas.ch/roller2/badtrash/entry/java-create-enum-instances-dynamically">Jerome Kehrli</a>
 * <br>
 * Thanks :)
 */
public class DynamicEnum {

	private static final Logger logger = LoggerFactory.getLogger(DynamicEnum.class);

	private static ReflectionFactory reflectionFactory = ReflectionFactory.getReflectionFactory();

	private static void setFailsafeFieldValue(
		Field field,
		Object target,
		Object value)
		throws NoSuchFieldException, IllegalAccessException {

		// let's make the field accessible
		field.setAccessible(true);

		// next we change the modifier in the Field instance to not be final anymore,
		// thus tricking reflection into letting us modify the static final field
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		int modifiers = modifiersField.getInt(field);

		// blank out the final bit in the modifiers int
		modifiers &= ~Modifier.FINAL;
		modifiersField.setInt(field, modifiers);

		FieldAccessor fa = reflectionFactory.newFieldAccessor(field, false);
		fa.set(target, value);
	}

	private static void blankField(
		Class<?> enumClass,
		String fieldName)
		throws NoSuchFieldException, IllegalAccessException {

		for (Field field : Class.class.getDeclaredFields()) {
			if (field.getName().contains(fieldName)) {
				AccessibleObject.setAccessible(new Field[]{field}, true);
				setFailsafeFieldValue(field, enumClass, null);
				break;
			}
		}
	}

	private static void cleanEnumCache(Class<?> enumClass) throws NoSuchFieldException, IllegalAccessException {
		blankField(enumClass, "enumConstantDirectory"); // Sun (Oracle?!?) JDK 1.5/6
		blankField(enumClass, "enumConstants"); // IBM JDK
	}

	private static ConstructorAccessor getConstructorAccessor(
		Class<?> enumClass,
		Class<?>[] additionalParameterTypes)
		throws NoSuchMethodException {

		Class<?>[] parameterTypes = new Class[additionalParameterTypes.length + 2];
		parameterTypes[0] = String.class;
		parameterTypes[1] = int.class;
		System.arraycopy(additionalParameterTypes, 0, parameterTypes, 2, additionalParameterTypes.length);
		return reflectionFactory.newConstructorAccessor(enumClass.getDeclaredConstructor(parameterTypes));
	}

	private static Object makeEnum(
		Class<?> enumClass,
		String value,
		int ordinal,
		Class<?>[] additionalTypes,
		Object[] additionalValues)
		throws ReflectiveOperationException {

		Object[] parms = new Object[additionalValues.length + 2];
		parms[0] = value;
		parms[1] = ordinal;
		System.arraycopy(additionalValues, 0, parms, 2, additionalValues.length);
		return enumClass.cast(getConstructorAccessor(enumClass, additionalTypes).newInstance(parms));
	}

	/**
	 * Add an enum instance to the enum class given as argument
	 *
	 * @param <T>      the type of the enum (implicit)
	 * @param enumType the class of the enum to be modified
	 * @param enumName the name of the new enum instance to be added to the class.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Enum<?>> void addEnum(Class<T> enumType, String enumName) {
		// 0. Sanity checks
		if (!Enum.class.isAssignableFrom(enumType)) {
			throw new RuntimeException("class [" + enumType + "] is not an instance of Enum");
		}

		// 1. Lookup "$VALUES" holder in enum class and get previous enum instances
		Field valuesField = null;
		Field[] fields = enumType.getDeclaredFields();
		for (Field field : fields) {
			if (field.getName().contains("$VALUES")) {
				valuesField = field;
				break;
			}
		}
		AccessibleObject.setAccessible(new Field[]{valuesField}, true);

		if (valuesField == null) {
			throw new RuntimeException("Unable to find values field for enum [" + enumType.getClass() + "]");
		}

		try {
			// 2. Copy it
			T[] previousValues = (T[]) valuesField.get(enumType);
			List<T> values = new ArrayList<>(Arrays.asList(previousValues));

			// 3. build new enum
			T newValue = (T) makeEnum(
				enumType,         // The target enum class
				enumName,         // THE NEW ENUM INSTANCE TO BE DYNAMICALLY ADDED
				values.size(),
				new Class<?>[]{}, // could be used to pass values to the enum constuctor if needed
				new Object[]{}    // could be used to pass values to the enum constuctor if needed
			);

			// 4. add new value
			values.add(newValue);

			// 5. Set new values field
			setFailsafeFieldValue(valuesField, null, values.toArray((T[]) Array.newInstance(enumType, 0)));

			// 6. Clean enum cache
			cleanEnumCache(enumType);
		} catch (ReflectiveOperationException | RuntimeException e) {
			logger.error("Error adding custom [{}] enum value onto [{}]", enumName, enumType);
			throw new YopRuntimeException("Error adding custom enum value !", e);
		}
	}

	/**
	 * Get the member values map of a field annotation.
	 * <br>
	 * With the field annotation values map, you can dynamically change an annotation field value !
	 * @param sourceClass     the source class, where the field is.
	 * @param getter          the field getter
	 * @param annotationClass the annotation class
	 * @param <T> the source class type
	 * @return the field annotation member values.
	 * @throws ReflectiveOperationException Something went bad. Field does not exist, could not read annotation...
	 */
	@SuppressWarnings("unchecked")
	public static <T> Map<String, Object> getFieldAnnotationValues(
		Class<T> sourceClass,
		Function<T, ?> getter,
		Class<? extends Annotation> annotationClass)
		throws ReflectiveOperationException {

		Field field = Reflection.findField(sourceClass, getter);
		if (field == null) {
			throw new NoSuchFieldException("No field found for getter on [" + sourceClass.getName() + "] !");
		}

		Object annotation = field.getAnnotation(annotationClass);
		Field h = annotation.getClass().getSuperclass().getDeclaredField("h");
		h.setAccessible(true);
		Object enumField = h.get(annotation);
		Field memberValues = enumField.getClass().getDeclaredField("memberValues");
		memberValues.setAccessible(true);
		return (Map) memberValues.get(enumField);
	}
}
