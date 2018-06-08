# YOP !
The Hit & Run ORM :-)  
Welcome to the sources !


## Illusory notes
- I would like to keep a very limited amount of mapping annotations.
- I would like to keep a very limited amount of generated SQL query types.
- I would like to keep the amount of source code as low as possible.
- I would like to keep the code as documented as possible.

## Content :
- Mapping annotations for data objects
- *Yopable* interface for data objects (because it makes everything easier)
- Some specific Runtime exceptions
- CRUD utility classes in *query* package
- Some basic tools to execute queries and keep track of context/parameters/query.
- Some filthy *"Yopable to SQL schema"* generation tools 
- A basic *JDBC abstraction* (so maybe it could work with Android)
- Some *Reflection* tools, which can do naughty things if driven into a corner

## To do :
- Rationalise : YOP is still very rough. And probably completely dumb.
- Make CRUD utilities available on the Yopable interface ? e.g. Yopable.save(IConnection connection) ?
- Test (a lot !) : see the README file in the *test* directory.