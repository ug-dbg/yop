package org.yop.orm.model;

import org.yop.orm.sql.SQLPart;
import org.yop.orm.util.MessageUtil;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;

public class Alias<T> extends SQLPart implements Comparable<Alias<T>> {

	private Field field;
	private String alias;

	public Alias(Field field, String alias) {
		super(alias);
		this.field = field;
		this.alias = alias;
	}

	@SuppressWarnings("unchecked")
	public Class<T> getTarget() {
		return (Class) this.field.getDeclaringClass();
	}

	@Override
	public int compareTo(Alias<T> o) {
		return Reflection.fieldToString(this.field).compareTo(Reflection.fieldToString(o.field));
	}

	public static class Aliases<T> extends SQLPart {
		private Map<Field, Alias<T>> aliases = new HashMap<>();

		public Aliases(List<Alias<T>> aliases) {
			super(MessageUtil.join(",", aliases));
			aliases.forEach(alias -> this.aliases.put(alias.field, alias));
		}

		public SQLPart equals(Aliases<T> other, BiFunction<CharSequence, CharSequence, SQLPart> equals) {
			List<SQLPart> fieldEquals = new ArrayList<>();

			for (Alias<T> alias : this.aliases.values()) {
				fieldEquals.add(equals.apply(alias.alias, other.get(alias)));
			}

			return SQLPart.join(" AND ", fieldEquals);
		}

		private String get(Alias alias) {
			return this.aliases.get(alias.field).alias;
		}
	}
}
