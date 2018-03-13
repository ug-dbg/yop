# Testing YOP !
Yop is a naughty 'hit and run' tool but it definitely wants to be tested.

## General observations
- It is difficult to write some assertions on generated queries.
- I don't really want to rely on an underlying DBMS for my tests, but what else can I do ?
- An SQLite DB is simply a file.

## Principles :
- Your test class can extend **DBMSSwitch**, which should be helpful.
- DBMSSwitch selects a target DBMS - reading some system variables, see 'yop.test.dbms'.
- Default target is a **'delete on exit' SQLite DB**, so you can build and test YOP from a very minimal environnement.
- Declare a package prefix in your tests and DBMSSwitch clears and prepares the database schema for your Yopables. 

# Cheat sheet
To set the target DBMS custom credentials, use : -Dyop.test.dbms.user="[username]" -Dyop.test.dbms.pwd="[password]".
- Default user name : yop
- Default password  : yop

Here are some VM arguments that can be helpful for some DMBS : 
- *SQLite* → Ø
- *MySQL* → -Dyop.test.dbms=mysql
- *Postgres* → -Dyop.test.dbms=postgres
- *MS-SQL* → -Dyop.test.dbms=mssql -Dyop.sql.separator=$
- *Oracle* → -Dyop.test.dbms=oracle -Dyop.sql.sequences=true -Dyop.sql.separator=# -Dyop.alias.max.length=29

## To do :
- A lot more tests !!!
- Testing other DBMS ?