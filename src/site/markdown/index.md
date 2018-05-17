# YOP !
The Hit & Run ORM :-)

## What is it ?
YOP is a lightweight ORM wannabe. Hit and run, no session, no bytecode generation, SQL-like syntax.

Example :
```
Upsert   
.from(Pojo.class)  
.onto(newPojo)  
.join(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo)))    
.join(JoinSet.to(Pojo::getOthers))  
.checkNaturalID()  
.execute(connection);  
```

Yop has strong constraints on the Database schema.
**Java 8** is required.

## General observations
- I don't like SQL but it is incredibly efficient.
- I don't like ORM but I cannot write specific requests. And I still don't like SQL.
- My difficulties with ORM mostly come from the unawareness of its mechanisms.
- One of the greatest difficulty in ORM comes from cycles in the data graph.
- One of the greatest difficulty in data representation comes from cycles in the data graph.
- One-to-one, One-to-many, Many-to-many relationships are confusing - even though they do make sense in real world.
- It is very hard to keep control over the fetch strategy at runtime.

## Principles :
- 1 object → 1 table
- <strike>1 relation → 1 join table</strike>
- <strike>No foreign key in object table</strike>
- Limited set of mapping annotations
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
- Tests (a lot)