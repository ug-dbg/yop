/**
 * SQL Query management : you'll find all you need here to CRUD your Yopable objects.
 * <br>
 * Yop aims at providing a way to build requests that look like SQL and takes advantage of method references.
 * <br><br>
 * If you are reading this, you will start with the CRUD query builders :
 * <ul>
 *     <li>{@link org.yop.orm.query.sql.Select}</li>
 *     <li>{@link org.yop.orm.query.sql.Upsert} and {@link org.yop.orm.query.batch.BatchUpsert}</li>
 *     <li>{@link org.yop.orm.query.sql.Delete}</li>
 *     <li>{@link org.yop.orm.query.sql.Hydrate}</li>
 * </ul>
 * They share a common logic that must (should :-D) enable you to inline declare clauses :
 * <ul>
 *     <li>from</li>
 *     <li>join</li>
 *     <li>where</li>
 *     <li>onto</li>
 * </ul>
 */
package org.yop.orm.query.sql;