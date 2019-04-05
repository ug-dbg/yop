package org.yop.orm.transform;

import org.yop.orm.annotations.Column;
import org.yop.orm.sql.Config;
import org.yop.orm.util.TransformUtil;

/**
 * A fall back transformer is used when the {@link java.sql.ResultSet#getObject(int, Class)} fails.
 */
public class FallbackTransformer implements ITransformer<Object> {

	/** Singleton instance */
	static final FallbackTransformer INSTANCE = new FallbackTransformer();

	/** Private constructor : please use {@link #INSTANCE} */
	private FallbackTransformer() {}

	/**
	 * See {@link TransformUtil#transform(Object, Class)}.
	 * @param fromJDBC incoming object
	 * @param into     expected target type
	 * @return what transformed into... or not
	 */
	@Override
	public Object fromSQL(Object fromJDBC, Class into) {
		return TransformUtil.transform(fromJDBC, into);
	}

	/**
	 * Does nothing. Return 'what' as is.
	 * <br><br>
	 * {@inheritDoc}
	 */
	@Override
	public Object forSQL(Object what, Column column, Config config) {
		return what;
	}
}
