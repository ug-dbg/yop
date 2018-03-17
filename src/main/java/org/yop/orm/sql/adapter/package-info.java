/**
 * SQL Connection/Request/Cursor <b>abstraction</b>.
 * <br>
 * (e.g. you have a grip onto your SQL database other than JDBC)
 * <br><br>
 * This is kind of an adapter pattern.
 * <br><br>
 * The reason for this is making a <i>very small</i> step toward Android.
 * <br><br>
 * You will find in this package the 3 interfaces to implement so YOP can work :-)
 * <br>
 * See {@link org.yop.orm.sql.adapter.jdbc} for the JDBC implementation.
 */
package org.yop.orm.sql.adapter;