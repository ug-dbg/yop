# YOP !
The Hit & Run ORM :-)

  
  
## What is it ?
YOP is a lightweight ORM wannabe. Hit and run, no session, no bytecode generation, SQL-like syntax.  
Actually, YOP is a tool to manage tree relation propagation at runtime when serializing/unserializing.  
Relation propagation management is mostly achieved using Java method references.  

The main idea is **not using literals** to indicate relations between objects but **getters** (→ 'Find usages' in your IDE).

Here are some API examples :

**Create/Update** :
<pre>  
```Upsert   
.from(Pojo.class)  
.onto(newPojo)  
.join(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo)))    
.join(JoinSet.to(Pojo::getOthers))  
.checkNaturalID()  
.execute(connection);```
</pre>

**Read** :
<pre>  
```Select
.from(Pojo.class)
.join(JoinSet.to(Pojo::getJopos))
.join(JoinSet.to(Pojo::getOthers).where(
    Where.compare(Other::getName, Operator.EQ, Path.pathSet(Pojo::getJopos).to(Jopo::getName))
))
.execute(connection);```
</pre>


**Delete** :   
<pre>  
```Delete.from(Pojo.class)
.join(JoinSet.to(Pojo::getOthers).join(Join.to(Other::getExtra)))
.executeQueries(connection);```
</pre>

**Hydrate (fetch relation dynamically)** :  
<pre>
```Hydrate.from(Pojo.class).onto(objectsWithID).fetchSet(Pojo::getJopos).execute(connection);```
</pre>

**Recurse (hydrate relations, recursively)** :   
<pre>  
```Recurse.from(Pojo.class).onto(objectsWithID).joinAll().execute(connection);```
</pre>

**JSON serialization** :  
<pre>  
```JSON.from(Pojo.class)
.joinAll()
.joinIDs(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo)))
.joinIDs(JoinSet.to(Pojo::getOthers).join(JoinSet.to(Other::getPojos)))
.register(LocalDateTime.class, (src, typeOfSrc, context) -> new JsonPrimitive("2000-01-01T00:00:00.000"))
.onto(pojos)
.toJSON();```
</pre>

Yop can serialize from/to a database (**ORM**) or to **JSON**.  
Yop tries to generate very standard SQL queries that works on most databases.  
Yop has some requirements on the Database schema (See Yop principles).  
Yop requires at least **Java 8**.  
Yop can be used in conjunction with other ORM tools.  
Yop is easy to bootstrap in a *scratch* main class.  
Yop is ~5000 lines of code.  
Yop has been (poorly) unit tested on :  
- SQLite (Xerial sqlite-jdbc 3.21)  
- MySQL (5.7)  
- PostgreSQL (9.5)   
- Oracle (XE 11g)  
- MS-SQL (2017)  
  
  
  
## General observations
- I don't like SQL but it is incredibly efficient.
- I don't like ORM but I cannot write specific requests. And I still don't like SQL.
- Using an ORM, tt is very hard to keep control over the fetch strategy at runtime.
- My difficulties with ORM mostly come from the unawareness of its mechanisms.
- One of the greatest difficulty in ORM comes from cycles in the data graph.
- One of the greatest difficulty in data representation comes from cycles in the data graph.
- One-to-one, One-to-many, Many-to-many relationships are confusing - even though they do make sense in real world.
  
  
  
## Yop Principles : 
- 1 object/table → 1 technical ID
- Limited set of mapping annotations
- There can be a natural ID on an object and the Upsert method can check it before inserting
- 'transient' keyword → cut cycles in the data graph
- No session
- No bytecode generation
- No lazy/eager in mapping : the CRUD API must provide an easy way to deal with fetch strategies
- The CRUD API should provide an easy way to add/remove relations.
- The CRUD API should provide an easy way to deal with graphs of objects.
- The CRUD API should provide an easy way to reference the fields/accessors where constraints are set so the IDE can easily find usages
- Data can be CRUD using the natural key if the technical ID is unknown
- Save and Update must be achieved through the same API
- Generated queries must be simple and human readable (ha ha ha)
- User must keep control over the queries executions order
  
  
  
## To do :
- Schema management
- Test / correct / improve
- Test with / Support other DBMS
  
  
  
![Well, this one is a bit rude](images/orm_snowman.jpg)