package org.yop.orm.model;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.util.ORMUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

public class ID<T> implements Comparable<ID<T>> {

	private Class<T> target;
	private Map<Field, Comparable> idFields = new TreeMap<>(Comparator.comparing(Field::getName));

	private ID(Class<T> target, Function<Field, Comparable> reader) {
		this.target = target;
		ORMUtil.getIdFields(this.target).forEach(f -> this.idFields.put(f, reader.apply(f)));
	}

	public Comparable get(Field idField) {
		return this.idFields.get(idField);
	}

	public static <T> Comparable id(Class<T> target, Function<Field, Comparable> reader) {
		if (! isComposite(target)) {
			return reader.apply(ORMUtil.getIdField((Class) target));
		}
		return new ID<>(target, reader);
	}

	public static <T> ID<T> id(Class<T> target) {
		return new ID<>(target, f -> null);
	}

	public static <T> Comparable id(T obj) {
		if (obj == null) {
			return null;
		}

		Class clazz = obj.getClass();
		return isComposite(clazz) ? compositeID(obj) : (Comparable) Reflection.readField(ORMUtil.getIdField(clazz), obj);
	}

	public static <T> ID<T> compositeID(T obj) {
		if (obj == null) {
			return null;
		}
		return new ID<>(Reflection.classOf(obj), f -> (Comparable) Reflection.readField(f, obj));
	}

	public static boolean isComposite(Class<?> target) {
		return ORMUtil.getIdFields(target).size() > 1;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || this.getClass() != o.getClass()) {
			return false;
		}
		ID<?> id = (ID<?>) o;
		return Objects.equals(this.target, id.target) && Objects.equals(this.idFields, id.idFields);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.target, this.idFields);
	}

	@Override
	public int compareTo(ID<T> o) {
		if (o == null) {
			return 1;
		}
		if (!Objects.equals(this.target, o.target)) {
			throw new YopRuntimeException("Incomparable IDs for [" + this.target + "] and [" + o.target + "]");
		}
		CompareToBuilder builder = new CompareToBuilder();
		this.idFields.keySet().forEach(k -> builder.append(this.idFields.get(k), o.idFields.get(k)));
		return builder.toComparison();
	}
}
