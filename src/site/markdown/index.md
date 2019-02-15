# YOP !
An [ORM](https://en.wikipedia.org/wiki/Object-relational_mapping "Wikipedia → ORM") 
with an 
[OpenAPI](https://en.wikipedia.org/wiki/OpenAPI_Specification "Wikipedia → OpenAPI specification") 
[REST](https://en.wikipedia.org/wiki/Representational_state_transfer "Wikipedia → REST") exposition.

[![Build Status](http://hdmcl.no-ip.org:8081/job/yop-test-MySQL/badge/icon)](http://jenkins.y-op.org/job/yop-test-MySQL/)

## Coverage Status
- [![Coverage](http://hdmcl.no-ip.org:8081/job/yop.dev_rest-test-MySQL/ws/reflection/target/jacoco.svg)](http://jenkins.y-op.org/job/yop.dev_rest-test-MySQL/lastBuild/jacoco) 
[reflection](reflection)
- [![Coverage](http://hdmcl.no-ip.org:8081/job/yop.dev_rest-test-MySQL/ws/orm/target/jacoco.svg)](http://jenkins.y-op.org/job/yop.dev_rest-test-MySQL/lastBuild/jacoco)
[orm](orm)
- [![Coverage](http://hdmcl.no-ip.org:8081/job/yop.dev_rest-test-MySQL/ws/ioc/target/jacoco.svg)](http://jenkins.y-op.org/job/yop.dev_rest-test-MySQL/lastBuild/jacoco)
[ioc](ioc)
- [![Coverage](http://hdmcl.no-ip.org:8081/job/yop.dev_rest-test-MySQL/ws/rest/target/jacoco.svg)](http://jenkins.y-op.org/job/yop.dev_rest-test-MySQL/lastBuild/jacoco)
[rest](rest)
- [![Coverage](http://hdmcl.no-ip.org:8081/job/yop.dev_rest-test-MySQL/ws/swaggerui/target/jacoco.svg)](http://jenkins.y-op.org/job/yop.dev_rest-test-MySQL/lastBuild/jacoco)
[swaggerui](swaggerui)
- [demo](demo)

## What is it ?
YOP is a 3 feature backend stack. Pick up what you need : 
* ORM : No session, no bytecode generation, SQL-like syntax. Method references to manage join clauses.  
* REST webservices for your ORM data objects with an OpenAPI description generation.
* [Swagger UI](https://swagger.io/tools/swagger-ui "Swagger UI website") for your REST webservices.

## Philosophy
YOP is not a framework but a tool. It should do no more than you ask for.  
Boiler plate scenarios should be executed conventionally with no extra line of code.  
A conventional scenario should be easily overridable.  
YOP ORM/REST implementations are very naive and straightforward.
