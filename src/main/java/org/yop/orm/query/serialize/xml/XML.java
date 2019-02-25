package org.yop.orm.query.serialize.xml;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.IJoin;
import org.yop.orm.query.serialize.Serialize;
import org.yop.orm.util.JoinUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * XML : serialize a {@link Yopable} to XML and manage the relation to serialize at runtime.
 * <br><br>
 * Its API aims at being similar to the {@link org.yop.orm.query} APIs.
 * <br>
 * You can set the join relations you want to be serialized : {@link #join(IJoin)}.
 * <br>
 * Example :
 * <pre>
 * {@code
 * XML
 *   .from(Pojo.class)
 *   .join(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo)))
 *   .join(JoinSet.to(Pojo::getOthers).join(JoinSet.to(Other::getPojos)))
 *   .onto(pojo)
 *   .execute();
 * }
 * </pre>
 * This API relies on <a href="http://x-stream.github.io/">X-Stream XML serializer</a>.
 * <br>
 * You can configure the root alias ({@link #rootAlias(String)}) and anything on XStream using {@link #getXstream()}.
 * <br><br>
 * Deserialization can be achieved using static methods (no need to configure joins, they are explicit) :
 * <ul>
 *     <li>{@link #deserialize(String, Class, Class[])}</li>
 *     <li>{@link #deserialize(String, Class, String, Class[])}</li>
 * </ul>
 * @param <T> the target Yopable type to serialize to XML.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class XML<T extends Yopable> implements Serialize<XML, T> {

	/** The target Yopable class to serialize to JSON */
	private Class<T> target;

	/** The Yopable elements to serialize */
	private Yopables<T> elements = new Yopables<>();

	/** Join clauses */
	private final Collection<IJoin<T, ? extends Yopable>> joins = new ArrayList<>();

	/** The Xstream serializer instance */
	private final XStream xstream = new XStream();

	/**
	 * Default constructor. Initializes {@link #xstream}. Please use {@link #from(Class)}.
	 * @param target the serialization target class
	 */
	private XML(Class<T> target) {
		this.target = target;
		XStream.setupDefaultSecurity(this.xstream);
		this.xstream.alias("list", Yopables.class);
		this.xstream.addImplicitCollection(Yopables.class, "yopables");
	}

	/**
	 * Create a new XML serializer instance for the given target.
	 * @param target the serialization target class
	 * @param <T> the serialization target type
	 * @return a new {@link XML} instance
	 */
	public static <T extends Yopable> XML<T> from(Class<T> target) {
		return new XML<>(target);
	}

	/**
	 * Deserialize a collection of T.
	 * @param yopablesXML the serialized elements. Root node must be 'list'.
	 * @param target      the target class
	 * @param allowed     a collection of classes that are allowed to instantiate. If empty : every class is allowed.
	 * @param <T> the target type
	 * @return the deserialized T elements from the XML input
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Yopable> Collection<T> deserialize(
		String yopablesXML,
		Class<T> target,
		Class... allowed) {

		XStream xstream = new XStream();
		XStream.setupDefaultSecurity(xstream);
		if (allowed.length > 0) {
			xstream.allowTypes(ArrayUtils.addAll(allowed, target));
		} else {
			xstream.allowTypesByRegExp(new String[]{".*"});
		}

		xstream.registerConverter(new YopXMLConverter(xstream.getMapper(), target, new ArrayList<>()));
		return (Collection<T>) xstream.fromXML(yopablesXML, target);
	}

	/**
	 * Deserialize a collection of T when the root node name is different from 'list'.
	 * The root node name of the XML input (rootAlias) will be naively replaced with 'list' so it can be deserialized.
	 * @param yopablesXML the serialized elements
	 * @param target      the target class
	 * @param rootAlias   the serialized elements root node name (if different from 'list')
	 * @param allowed     a collection of classes that are allowed to instantiate. If empty : every class is allowed.
	 * @param <T> the target type
	 * @return the deserialized T elements from the XML input
	 */
	public static <T extends Yopable> Collection<T> deserialize(
		String yopablesXML,
		Class<T> target,
		String rootAlias,
		Class... allowed) {

		String xml = yopablesXML;
		xml = StringUtils.replaceFirst(xml, "^" + Pattern.quote("<" + rootAlias + ">"), "<list>");
		xml = StringUtils.replaceFirst(xml, Pattern.quote("</" + rootAlias + ">") + "$", "</list>");
		return deserialize(xml, target, allowed);
	}

	@Override
	public XML<T> onto(T element) {
		this.elements.yopables.add(element);
		return this;
	}

	@Override
	public XML<T> onto(Collection<T> elements) {
		this.elements.yopables.addAll(elements);
		return this;
	}

	@Override
	public <R extends Yopable> XML<T> join(IJoin<T, R> join) {
		this.joins.add(join);
		return this;
	}

	@Override
	public XML<T> join(Collection<IJoin<T, ?>> joins) {
		this.joins.addAll(joins);
		return this;
	}

	@Override
	public XML<T> joinAll() {
		this.joins.clear();
		JoinUtil.joinAll(this.target, this.joins);
		return this;
	}

	@Override
	public XML<T> joinProfiles(String... profiles) {
		if (profiles.length > 0) {
			JoinUtil.joinProfiles(this.target, this.joins, profiles);
		}
		return this;
	}

	@Override
	public String execute() {
		this.xstream.registerConverter(new YopXMLConverter<>(this.xstream.getMapper(), this.target, this.joins));
		return this.xstream.toXML(this.elements);
	}

	/**
	 * Set the root node alias for the output. Default is 'list'.
	 * @param alias the root node alias
	 * @return the current XML serialize request, for chaining purposes
	 */
	public XML<T> rootAlias(String alias) {
		this.xstream.alias(alias, Yopables.class);
		return this;
	}

	/**
	 * Get the underlying {@link XStream} serializer instance, so it can be configured.
	 * @return {@link #xstream}
	 */
	public XStream getXstream() {
		return this.xstream;
	}

	/**
	 * A simple wrapper for {@link #elements} so the root alias can be configured.
	 * @param <T> the target type
	 */
	private static class Yopables<T extends Yopable> {
		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // Xstream reflection marshalling
		private List<T> yopables = new ArrayList<>();
	}
}