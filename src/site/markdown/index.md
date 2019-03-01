# YOP !
The Hit & Run ORM :-)

  
  
## What is it ?
YOP is a lightweight ORM wannabe. Hit and run, no session, no bytecode generation, SQL-like syntax.  
Actually, YOP is a tool to manage tree relation propagation at runtime when serializing/unserializing.  
Relation propagation management is mostly achieved using Java method references.  

The main idea is **not using literals** to indicate relations between objects but **getters** :  
- 'Find usages' and 'Refactor' tools can be safely used  
- Auto-completion for method references should be available  

Here are some API examples :

**Create/Update** :
```java
Upsert   
 .from(Library.class)  
 .onto(library)  
 .join(SQLJoin.toN(Library::getBooks).join(SQLJoin.to(Book::getAuthor)))    
 .join(SQLJoin.toN(Library::getEmployees))  
 .checkNaturalID()  
 .execute(connection);
```

**Read** :
```java
Select
 .from(Library.class)
 .join(SQLJoin.toN(Library::getBooks))
 .join(SQLJoin.toN(Library::getEmployees).where(
    Where.compare(
        Employee::getName, 
        Operator.EQ, 
        Path.pathSet(Library::getBooks).to(Book::getAuthor).to(Author::getName)
)))
 .execute(connection);
```

**Read with paging** :
```java
Select
 .from(Library.class)
 .page(10L, 5L)
 .join(SQLJoin.toN(Library::getBooks))
 .join(SQLJoin.toN(Library::getEmployees).where(
    Where.compare(
        Employee::getName, 
        Operator.EQ, 
        Path.pathSet(Library::getBooks).to(Book::getAuthor).to(Author::getName)
)))
 .execute(connection);
```

**Count** :
```java
Select
 .from(Library.class)
 .join(SQLJoin.toN(Library::getBooks))
 .join(SQLJoin.toN(Library::getEmployees).where(
    Where.compare(
        Employee::getName, 
        Operator.EQ, 
        Path.pathSet(Library::getBooks).to(Book::getAuthor).to(Author::getName)
)))
 .count(connection);
```

**Delete** :   
```java
Delete.from(Library.class)
 .join(SQLJoin.toN(Library::getBooks).join(SQLJoin.to(Book::getAuthor)))
 .executeQueries(connection);
```

**Hydrate (fetch relation dynamically)** :  
```java 
Hydrate.from(Book.class).onto(booksWithID).join(Book::getChapters).execute(connection);
```

**Recurse (hydrate relations, recursively)** :   
```java 
Hydrate.from(Book.class).onto(booksWithID).join(Book::getChapters).recurse().execute(connection);
```

**JSON serialization** :  
```java
JSON.from(Library.class)
 .joinAll()
 .joinIDs(SQLJoin.toN(Library::getBooks).join(SQLJoin.to(Book::getAuthor)))
 .joinIDs(SQLJoin.toN(Library::getEmployees).join(SQLJoin.toN(Employee::getProfiles)))
 .register(LocalDateTime.class, (src, typeOfSrc, context) -> new JsonPrimitive("2000-01-01T00:00:00.000"))
 .onto(library)
 .toJSON();
```

**Explicit join using a path of getter references (up to 4 consecutive getters)**  
⚠ Using this method, the compiler cannot warn you about incoherent paths ⚠
```java
Select   
 .from(Library.class)    
 .join(Library::getAuthors, Author::getBooks, Book::getChapters)    
 .join(Library::getEmployees)  
 .checkNaturalID()  
 .execute(connection);
```  
  
### In short
Yop can serialize from/to a database (**ORM**) or to **JSON**.  
Yop queries are serializable to/from JSON.  
Yop tries to generate very standard SQL queries that works on most databases.  
Yop has some requirements on the Database schema (See Yop principles).  
Yop requires at least **Java 8**.  
Yop can be used in conjunction with other ORM tools.  
Yop is easy to bootstrap in a *scratch* main class.  
Yop is ~6000 lines of code.  
Yop has been (poorly) unit tested on :  
- SQLite (Xerial sqlite-jdbc 3.21)  
- MySQL (5.7)  
- PostgreSQL (9.5)   
- Oracle (XE 11g)  
- MS-SQL (2017)  
- IBM Db2 (Docker Hub image ibmcom/db2express-c)
  
  
  
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
- Data objects must implement a Serializable-like interface (Yopable) and use a very limited set of annotations
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