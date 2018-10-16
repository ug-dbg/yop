# YOP !
An ORM with an OpenAPI REST exposition

[![Build Status](http://hdmcl.no-ip.org:8081/job/yop-test-MySQL/badge/icon)](http://jenkins.y-op.org/job/yop-test-MySQL/)
[![Coverage ORM](http://hdmcl.no-ip.org:8081/job/yop-test-MySQL/ws/orm/target/jacoco.svg)](http://jenkins.y-op.org/job/yop-test-MySQL/lastBuild/jacoco)
[![Coverage REST](http://hdmcl.no-ip.org:8081/job/yop-test-MySQL/ws/rest/target/jacoco.svg)](http://jenkins.y-op.org/job/yop-test-MySQL/lastBuild/jacoco)
[![Coverage SWAGGERUI](http://hdmcl.no-ip.org:8081/job/yop-test-MySQL/ws/swaggerui/target/jacoco.svg)](http://jenkins.y-op.org/job/yop-test-MySQL/lastBuild/jacoco)

## What is it ?
YOP is a 3 feature backend stack. Pick up what you need : 
* ORM : No session, no bytecode generation, SQL-like syntax. Method references to manage join clauses.  
* REST exposition for your ORM pojos with an OpenAPI description.
* Swagger UI for your REST exposition.

## Philosophy
YOP is not a framework but a tool. It should do no more than you ask for.  
Boiler plate scenarios should be executed conventionally with no extra line of code.  
A conventional scenario should be easily overridable.  
