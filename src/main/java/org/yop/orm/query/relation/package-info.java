/**
 * Relation management. Generate SQL join clauses for a relation between 2 java classes.
 * <br>
 * See the common {@link org.yop.orm.query.relation.Relation} interface.
 * <br>
 * Considering the Field annotation (@JoinColumn/@JoinTable), the 'factory' method of the Relation interface
 * returns either a {@link org.yop.orm.query.relation.JoinColumnRelation}
 * or a {@link org.yop.orm.query.relation.JoinTableRelation}.
 * <br>
 */
package org.yop.orm.query.relation;