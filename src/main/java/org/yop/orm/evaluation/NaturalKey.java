package org.yop.orm.evaluation;

import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.query.Where;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.ORMUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Natural Key Evaluation : give me an object reference and I'll build an SQL evaluation for its naturel key fields
 * @param <T> the target type
 */
public class NaturalKey<T extends Yopable> implements Evaluation {

	/** The object from which is taken the Natural ID */
	private T reference;

	/**
	 * Default constructor : give me an object reference so I can read the natural ID !
	 * @param reference the target object reference
	 */
	public NaturalKey(T reference) {
		if(reference == null) {
			throw new YopRuntimeException("Natural key reference cannot be null !");
		}
		this.reference = reference;
	}

	@Override
	public <U extends Yopable> String toSQL(Context<U> context, Parameters parameters) {
		List<Field> naturalKeys = ORMUtil.getNaturalKeyFields(this.reference.getClass());
		return Where.toSQL(
			naturalKeys
				.stream()
				.map(field -> getFieldRestriction(context, field, parameters))
				.collect(Collectors.toList()
		));
	}

	/**
	 * Build a restriction for a field of the natural ID.
	 * @param context    the current context (→ column prefix)
	 * @param field      the field on which the restriction must be built
	 * @param parameters the SQL query parameters (will be populated with the field value)
	 * @return the SQL portion for the given context→field restriction
	 */
	private String getFieldRestriction(Context<?> context, Field field, Parameters parameters) {
		Object ref = ORMUtil.readField(field, this.reference);
		if(ref != null) {
			String name = context.getPath() + "#" + field.getName() + " = " + "?";
			parameters.addParameter(name, ref);
		}
		return this.columnName(field, context) + "=" + (ref == null ? "" : "?");
	}

}
