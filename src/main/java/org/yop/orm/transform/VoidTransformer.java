package org.yop.orm.transform;

import org.yop.orm.annotations.Column;

/**
 * A void transforer does nothing and returns the parameter object to transform 'as is'.
 */
public class VoidTransformer implements ITransformer<Object> {

	/** Singleton instance */
	static final VoidTransformer INSTANCE = new VoidTransformer();

	/** Private constructor : please use {@link #INSTANCE} */
	private VoidTransformer() {}

	/**
	 * Does nothing. Return 'what' as is.
	 * <br><br>
	 * {@inheritDoc}
	 */
	@Override
	public Object forSQL(Object what, Column column) {
		return what;
	}

	/**
	 * Does nothing. Return 'fromJDBC' as is.
	 * <br><br>
	 * {@inheritDoc}
	 */
	@Override
	public Object fromSQL(Object fromJDBC, Class<?> into) {
		return fromJDBC;
	}
}
