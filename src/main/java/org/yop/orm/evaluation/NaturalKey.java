package org.yop.orm.evaluation;

import com.google.common.base.Joiner;
import org.yop.orm.exception.YopRuntimeException;
import org.yop.orm.model.Yopable;
import org.yop.orm.query.Context;
import org.yop.orm.sql.Constants;
import org.yop.orm.sql.Parameters;
import org.yop.orm.util.ORMUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

public class NaturalKey<T extends Yopable> implements Evaluation {

	private T reference;

	public NaturalKey(T reference) {
		if(reference == null) {
			throw new YopRuntimeException("Natural key reference cannot be null !");
		}
		this.reference = reference;
	}

	@Override
	public <U extends Yopable> String toSQL(Context<U> context, Parameters parameters) {
		List<Field> naturalKeys = ORMUtil.getNaturalKeyFields(this.reference.getClass());
		return Joiner.on(" AND ").join(
			naturalKeys
				.stream()
				.map(field -> getFieldRestriction(context, field, parameters))
				.collect(Collectors.toList()
		));
	}

	private String getFieldRestriction(Context<?> context, Field field, Parameters parameters) {
		try {
			Object ref = field.get(this.reference);
			if(ref != null) {
				String name = context.getPath() + "#" + field.getName() + " = " + "?";
				parameters.addParameter(name, ref);
			}
			return this.columnName(field, context) + "=" + (ref == null ? "" : "?");
		} catch (IllegalAccessException e) {
			throw new YopRuntimeException(
				"Could not read [" + field.getDeclaringClass() + "#" + field.getName()+ "] on [" + this.reference + "] !"
			);
		}
	}

}
