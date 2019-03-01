package org.yop.orm.query.serialize.xml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.join.IJoin;
import org.yop.orm.query.serialize.xml.annotations.YopXMLTransient;
import org.yop.orm.util.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An XStream converter for Yopable objects.
 * <br>
 * We use {@link Path} to keep a reference to the current {@link IJoin} and check if we should serialize any further.
 * @param <Root> the object graph root type.
 */
class YopXMLConverter<Root extends Yopable> extends ReflectionConverter {
	/** The current serialization path in the join graph */
	private Path path = Paths.get("/");

	/** The joins that must be serialized */
	private Set<Path> joins = new HashSet<>();

	/**
	 * Create a converter for the given root and the join graph.
	 * <br>
	 * Joins will be converted to paths using {@link #toPaths(IJoin, Class, Path, Set)}.
	 * <br>
	 * Uses {@link YopReflectionProvider} as XStream reflection provider.
	 * @param mapper the XStream mapper ({@link XStream#getMapper()}
	 * @param root   the serialization root class
	 * @param joins  the serialization join graph
	 */
	YopXMLConverter(Mapper mapper, Class<Root> root, Collection<IJoin<Root, ? extends Yopable>> joins) {
		super(mapper, new YopReflectionProvider());
		for (IJoin<Root, ? extends Yopable> join : joins) {
			toPaths(join, root, this.path, this.joins);
		}
	}

	@Override
	public boolean canConvert(Class type) {
		return Yopable.class.isAssignableFrom(type);
	}

	@Override
	public void marshal(Object original, HierarchicalStreamWriter writer, MarshallingContext context) {
		super.marshal(original, writer, context);
	}

	@Override
	protected boolean shouldUnmarshalTransientFields() {
		return true;
	}

	@Override
	protected boolean shouldUnmarshalField(Field field) {
		return shouldSerialize(field);
	}

	@Override
	protected void marshallField(MarshallingContext context, Object newObj, Field field) {
		if (! Yopable.class.isAssignableFrom(Reflection.getTarget(field))) {
			super.marshallField(context, newObj, field);
		} else {
			this.path = this.path.resolve(field.getName());
			if (this.joins.contains(this.path)) {
				super.marshallField(context, newObj, field);
			}
			this.path = this.path.getParent();
		}
	}

	/**
	 * A field is to be serialized if not static and not {@link YopXMLTransient}.
	 * 'transient' fields WILL be serialized.
	 * @param field the field to check
	 * @return true if the field should be serialized
	 */
	private static boolean shouldSerialize(Field field) {
		return (!Modifier.isStatic(field.getModifiers())) && !field.isAnnotationPresent(YopXMLTransient.class);
	}

	/**
	 * Recusrively transform the current join and its {@link IJoin#getJoins()} into a collection {@link Path}.
	 * @param currentJoin  the current join
	 * @param currentClass the current class
	 * @param currentPath  the current path
	 * @param output       the target paths
	 */
	@SuppressWarnings("unchecked")
	private static void toPaths(IJoin<?, ?> currentJoin, Class<?> currentClass, Path currentPath, Set<Path> output) {
		Field field = currentJoin.getField((Class) currentClass);
		Class nextClass = currentJoin.getTarget(field);
		Path nextPath = currentPath.resolve(field.getName());
		output.add(nextPath);

		for (IJoin<?, ? extends Yopable> nextJoin : currentJoin.getJoins()) {
			field = nextJoin.getField(nextClass);
			nextPath = nextPath.resolve(field.getName());
			output.add(nextPath);

			nextClass = nextJoin.getTarget(field);
			for (IJoin<?, ? extends Yopable> furtherJoin : nextJoin.getJoins()) {
				toPaths(furtherJoin, nextClass, nextPath, output);
			}
		}
	}

	/**
	 * A custom XStream field dictionary that uses {@link Reflection#getFields(Class)}.
	 */
	private static class YopFieldDictionary extends FieldDictionary {
		@Override
		public Iterator fieldsFor(Class cls) {
			return Reflection.getFields(cls).iterator();
		}
	}

	/**
	 * A custom XStream reflection provider that uses a {@link YopFieldDictionary} and {@link #shouldSerialize(Field)}.
	 */
	static class YopReflectionProvider extends PureJavaReflectionProvider {
		private YopReflectionProvider() {
			super(new YopFieldDictionary());
		}

		@Override
		protected boolean fieldModifiersSupported(Field field) {
			return shouldSerialize(field);
		}
	}

}