/**
 * Transformer mechanism. Can be useful when you have tricky data types.
 * <br>
 * How does it work ?
 * <ul>
 * <li>set a transformer using {@link org.yop.orm.annotations.Column} annotation</li>
 * <li>it will be used when setting a field value as a parameter</li>
 * <li>it will be used when reading a JDBC value from a query</li>
 * <li>a fall back transformer is used when {@link java.sql.ResultSet#getObject(int, Class)} fails
 * <li>Default transformer is {@link org.yop.orm.transform.VoidTransformer}, which does nothing</li>
 * </ul>
 */
package org.yop.orm.transform;