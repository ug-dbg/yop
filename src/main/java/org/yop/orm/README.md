# YOP !

## Principles :
- 1 object → 1 table
- 1 relation → 1 join table
- no foreign key in object table
- one unique constraint can be provided for an object → equals/hashcode & natural id
- no bytecode generation
- no lazy/eager in mapping : the CRUD API must provide an easy way to deal with fetch strategies
- the CRUD API should provide an easy way to add/remove relations.
- the CRUD API should provide an easy way to deal with graphs of objects.
- The CRUD API should provide an easy way to reference the fields/accessors where constraints are set so the IDE can easy find usages.
- data can be CRUD using the natural key if the technical ID is unknown
- Save and Update must be achieved through the same API
- generated queries must be simple and human readable
- user must keep control over the queries executions order

## To do :
- @Relation annotation (when deleting an object, which relation tables can be cleaned ?)
- Save
- Delete
- Robust table aliasing / Context propagation
- Robust cycle breaker → no cycle mapped ! Use 'transient' ! → easy JSON/XML serialize
- Schema management
- SQL query parameters
- First level cache ?