/**
 * <p>
 *     <b>Welcome to YOP :-)</b>
 * </p>
 * <br>
 * YOP is a dirty DIY Object-Relational Mapping tool
 * whose syntax aims at being SQL-like so you can write inlined requests in Java such as :
 * <br>
 * <b>
 * {@code
 *  Upsert
 *   .from(Pojo.class)
 *   .onto(newPojo)
 *   .join(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo)))
 *   .join(JoinSet.to(Pojo::getOthers))
 *   .checkNaturalID()
 *   .execute(connection);
 * }
 * </b>
 * <br>
 * <br>
 * Its core principles are built on some constraints :
 * <ul>
 *     <li>1 technical ID (Long) per Data object</li>
 *     <li>Deal with acyclic graphs of data only</li>
 *     <li>There can be cycles in the Java objects data graph but :
 *          <ul>
 *              <li>they must be 'cut' using 'transient' keyword</li>
 *              <li>transient relations must be explicitly invoked in queries</li>
 *          </ul>
 *     </li>
 * </ul>
 * YOP also tries to write very basic queries so it is not DBMS dependent. (Ha ha ha)
 */
package org.yop.orm;