# YOP !
The Hit & Run ORM :-)

## General observations
- I don't like SQL but it is incredibly efficient.
- I don't like ORM but I cannot write specific requests. And I still don't like SQL.
- My difficulties with ORM mostly come from the unawareness of its mechanisms.
- One of the greatest difficulty in ORM comes from cycles in the datagraph.
- One of the greatest difficulty in data representation comes from cycles in the datagraph.
- One-to-one, One-to-many, Many-to-many relationships are confusing - even though they do make sense in real world.
- It is very hard to keep control over the fetch strategy at runtime.

## Principles :
- 1 object → 1 table
- 1 relation → 1 join table
- No foreign key in object table
- One unique constraint can be provided for an object → equals/hashcode & natural id
- 'transient' keyword → cut cycles in the datagraph
- No bytecode generation
- No lazy/eager in mapping : the CRUD API must provide an easy way to deal with fetch strategies
- The CRUD API should provide an easy way to add/remove relations.
- The CRUD API should provide an easy way to deal with graphs of objects.
- The CRUD API should provide an easy way to reference the fields/accessors where constraints are set so the IDE can easily find usages.
- Data can be CRUD using the natural key if the technical ID is unknown
- Save and Update must be achieved through the same API
- Generated queries must be simple and human readable (ha ha ha)
- User must keep control over the queries executions order

## To do :
- <strike>@Relation annotation (when deleting an object, which relation tables can be cleaned ?)</strike> → ON DELETE CASCADE ?
- Robust table aliasing / Context propagation
- <strike>Robust cycle breaker</strike> → no cycle mapped ! Use 'transient' ! → easy JSON/XML serialize
- Cycle detector ?
- Schema management
- First level cache : Improve !