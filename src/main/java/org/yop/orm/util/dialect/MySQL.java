package org.yop.orm.util.dialect;

import org.yop.orm.util.ORMTypes;

/**
 * MySQL dialect {@link ORMTypes} extension.
 * @see <a href="https://www.mysql.com/">https://www.mysql.com/</a>
 */
public class MySQL extends ORMTypes {

	public static final ORMTypes INSTANCE = new MySQL();

	private MySQL() {
		super("varchar");
	}
}
