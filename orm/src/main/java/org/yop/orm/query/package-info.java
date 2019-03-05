/**
 * Query management : you'll find all you need here to create queries on {@link org.yop.orm.model.Yopable} objects.
 * <br>
 * {@link org.yop.orm.query.AbstractRequest} is the root class for all queries.
 * <br>
 * A query should have :
 * <ul>
 *     <li>A {@link org.yop.orm.query.Context}, i.e. a link to the target class</li>
 *     <li>Joins. See {@link org.yop.orm.query.AbstractRequest#joins} and {@link org.yop.orm.query.join.IJoin}</li>
 * </ul>
 * <br>
 * There are :
 * <ul>
 *     <li>SQL queries : e.g. {@link org.yop.orm.query.sql.Select}, {@link org.yop.orm.query.sql.Upsert}</li>
 *     <li>Serialize queries : e.g. {@link org.yop.orm.query.serialize.json.JSON}</li>
 * </ul>
 */
package org.yop.orm.query;