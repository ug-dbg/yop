# YOP ! 
[![Build Status](http://hdmcl.no-ip.org:8081/job/yop-test-MySQL/badge/icon)](http://jenkins.y-op.org/job/yop-test-MySQL/)
[![Coverage](http://hdmcl.no-ip.org:8081/job/yop-test-MySQL/ws/target/jacoco.svg)](http://jenkins.y-op.org/job/yop-test-MySQL/lastBuild/jacoco)
  
The 'Hit & Run' Object-Relational-Mapping tool :-) 

## What is YOP ?
An ORM tool to build/execute/map SQL requests onto java objects.

## What YOP is not ?
An ORM framework that manages persistent objects through a session.
There are some very good frameworks to do this, way better than this rough DIY tool.  
And using YOP complementary to another ORM does not sound completely absurd.

## What are the promises ?
Do some light/easy 'Hit & Run' CRUD in Java, using an SQL-like syntax and method references to manage propagation.  
Refactoring, finding usages and auto-completion from the IDE can be used to write/update requests.    
Examples : 
```
Upsert   
.from(Library.class)  
.onto(library)  
.join(SQLJoin.toN(Library::getBooks).join(SQLJoin.to(Book::getAuthor)))    
.join(SQLJoin.toN(Library::getEmployees))  
.checkNaturalID()  
.execute(connection);  
```
  
```
Collection<Book> booksFromDB = Select
.from(Book.class)
.join(SQLJoin.to(Book::getAuthor).where(Where.compare(Author::getName, Operator.LIKE, "%Roger%")))
.execute(connection)
```  
  
```
Book bookFromDB = Select
.from(Book.class)
.where(Where.naturalId(book))
.joinAll()
.uniqueResult(connection)
```  
  
```
// Using this lighter 'join' syntax, the compiler cannot warn you about incoherent paths
Select   
 .from(Library.class)    
 .join(Library::getAuthors, Author::getBooks, Book::getChapters)    
 .join(Library::getEmployees)  
 .checkNaturalID()  
 .execute(connection);
  ```

YOP *could* be able to run on **Android**.  
See the [yop-android-poc](https://github.com/ug-dbg/yop-android-poc) for an infamous POC.

## What are the requirements ?
* **Java 8** (→ method references are a key requirement of YOP)
* *Sqlite/mysql/postgres/oracle/mssql* database
* For now Yop does not support primary key
* Using a (very) limited set of mapping annotations and implementing the *Serializable like* Yopable interface. 
* Identifying cycles in your data graph.
* Handling your connections/transactions yourself


## What does it look like ?
Yop is available on Maven central : 
```xml
<dependency>
    <groupId>org.y-op</groupId>
    <artifactId>yop</artifactId>
    <version>0.8.0</version>
</dependency>
```

* You annotate your persistent objects using a limited set of annotations
* You use the transient keyword to indicate cycles in your data graphs
* You use an SQL-like java API whose syntax looks like :
```
Select.from(myClass).where(MyClass::GetFieldName()).joinAll().execute(connection);
Upsert.from(myClass).onto(instance).checkNaturalKey().join(Join.to(MyClass::getRelation())).execute(connection);
```

→ The API aims at inlining requests as much as possible.  
→ *transient* keyword cuts the cycles : the 'joinAll' clause will stop at transient relationships 
but **you always can join on a transient relation explicitly**.  
→ Using Yop, you will NEVER ensure you fully fetched/saved/deleted your data graph if there is a cycle in it. 
But you can do it piece by piece :-)  

## What's that 'acyclic' limitation ?
It is complicated to persist data graphs with cycles in SQL. Some frameworks do it pretty well.  
Then, it is also a bit complicated to serialize data graphs with cycles in json/XML. 
I actually feel it is **quite the same problem**.  
We do enjoy data graph cycles in the JVM memory because of a reference mechanism.  
Since you will mostly want to serialize this data, why not try to ease it once and for all ?  
Of course you will still be allowed to have cycles in your java objects data graph, but :
* you will have to cut them using 'transient' and CRUD them explicitly
* you will have to *think* your data as sets of acyclic graphs when you want to CRUD it to SQL/Json  
  
And there is a **recurse** option on the **Hydrate** API that can recursively fetch cyclic relations using sub-queries. 

## Is it reliable ?
For now, I guess it is not at all :-D  
You tell me ! Critics and contributions are welcome.

## Is it fast ? What is the overhead ?
I have not written/run any benchmark for now.
It has not been specially written to be fast.  
There is certainly plenty of room for optimization.  
You tell me ! Critics and contributions are welcome.