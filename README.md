# YOP !
The Hit & Run ORM :-) 

## What is YOP ?
An ORM tool to build/execute/map SQL requests onto java objects.

## What YOP is not ?
An ORM framework that manages persistent objects through a session.
There are some very good frameworks to do this, way better than this rough DIY tool.

## What are the promises ?
Do some light/easy 'Hit & Run' CRUD in Java, using an SQL-like syntax and method references to manage propagation.  
Examples : 
```
Upsert   
.from(Pojo.class)  
.onto(newPojo)  
.join(JoinSet.to(Pojo::getJopos).join(Join.to(Jopo::getPojo)))    
.join(JoinSet.to(Pojo::getOthers))  
.checkNaturalID()  
.execute(connection);  
```
  
```
Select
.from(Pojo.class)
.where(Where.naturalId(newPojo))
.joinAll()
.execute(connection
```

## What are the requirements ?
* **Java 8** (→ method references are a key requirement of YOP)
* *Sqlite/mysql/postgres/oracle/mssql* database
* Accepting some *strong and not very casual DB constraints* : 
  * 1 table per java object
  * 1 table per relation between java objects (*makes everything easier, actually*)
  * 1 auto incremented ID per class/table
* Using a (very) limited set of mapping annotations and implementing the *Serializable like* Yopable interface. 
* Identifying cycles in your data graph : YOP kinda deals with data trees !
* Handling your connections/transactions yourself

## What does it look like ?
* You annotate your persistent objects using a limited set of annotations
* You use the transient keyword to indicate cycles in your data graphs
* You use an SQL-like java API whose syntax looks like :
```
Select.from(myClass).where(MyClass::GetFieldName()).joinAll().execute(connection);
Upsert.from(myClass).onto(instance).checkNaturalKey().join(Join.to(MyClass::getRelation())).execute(connection);
```

→ The API aims at inlining requests as much as possible.  
→ *transient* keyword cuts the cycles : for instance - the 'joinAll' clause will stop at transient relationships 
but **you always can join on a transient relation explicitly**.  
→ Using Yop, you will NEVER ensure you fully fetched/saved/deleted your data graph if there is a cycle in it. 
But you can do it piece by piece :-)  

## What's that 'acyclic' limitation ?
It is complicated to persist data graphs with cycles in SQL. Some frameworks do it pretty well.  
Then, it is also a bit complicated to serialize data graphs with cycles in json/XML. 
I actually feel it is **quite the same problem**.  
We do enjoy data graph cycles in the JVM memory because of a reference mechanism.  
Since you will mostly want to bring your data out of the Java heap space, why not try to ease it once and for all ?  
Of course you will still be allowed to have cycles in your java objects data graph, but :
- you will have to cut them using 'transient' and CRUD them explicitly
- you will have to think your data as sets of acyclic graphs when you want to CRUD it to SQL/Json

## Is it reliable ?
For now, I guess it is not at all :-D  
You tell me !

## Is it fast ? What is the overhead ?
I have not written/run any benchmark for now.
You tell me !