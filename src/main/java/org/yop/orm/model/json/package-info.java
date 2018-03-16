/**
 * This package contains the YOP objects interfaces for JSON serialization.
 * <br>
 * Basically, the data serialization issues are quite the same for :
 * <ul>
 *     <li>POJO ←→ SQL</li>
 *     <li>POJO ←→ Json</li>
 * </ul>
 * It's all about breaking cycles !
 * <br>
 * Why not try to build on top of the {@link org.yop.orm.model.Yopable} interface ?
 */
package org.yop.orm.model.json;