package org.yop.orm.transform;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yop.orm.annotations.Column;
import org.yop.orm.exception.YopMappingException;

/**
 * A transformer implementation for Strings whose length must be checked manually.
 */
public class AbbreviateTransformer implements ITransformer<String> {

	private static final Logger logger = LoggerFactory.getLogger(AbbreviateTransformer.class);

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * If the string length is {@literal >} column.length : cut, throw an exception or do nothing, given the length strategy.
	 */
	@Override
	public Object forSQL(String s, Column column) {
		int maxLength = column.length();
		if(s != null && s.length() > maxLength) {
			switch (column.length_strategy()) {
				case EXCEPTION: this.tooLongException(s, column.name(), column.length());
				case CUT: return this.cut(s, maxLength - 1, column);
				case NONE:
				default:  return s;
			}
		}
		return s;
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * Do nothing, cast to string and return.
	 */
	@Override
	public String fromSQL(Object fromJDBC, Class into) {
		return (String) fromJDBC;
	}

	private String cut(String s, int maxLength, Column column) {
		logger.debug("Cutting too long string [{}] at length [{}] for column [{}]", s, maxLength, column.name());
		return StringUtils.abbreviate(s, maxLength);
	}

	private void tooLongException(String s, String columnName, int maxLength) {
		String message = "[" + s + "] is too long for [" + columnName + "] whose max length is [" + maxLength + "]";
		throw new YopMappingException(message);
	}
}
