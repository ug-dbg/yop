# YOP !

YOP is an **[ORM](https://en.wikipedia.org/wiki/Object-relational_mapping "Wikipedia → ORM") tool**.  
And since I am such a nice guy, a [REST](https://en.wikipedia.org/wiki/Representational_state_transfer "Wikipedia → REST") stack is built on top of it.

## Status
[![Build Status](http://hdmcl.no-ip.org:8081/job/yop.dev_rest-test-MySQL/badge/icon)](http://jenkins.y-op.org/job/yop.dev_rest-test-MySQL/)  
  
Modules : 
- [![Coverage](http://hdmcl.no-ip.org:8081/job/yop.dev_rest-test-MySQL/ws/reflection/target/jacoco.svg)](http://jenkins.y-op.org/job/yop.dev_rest-test-MySQL/lastBuild/jacoco) 
[reflection](reflection/README.md "The reflection module README")
- [![Coverage](http://hdmcl.no-ip.org:8081/job/yop.dev_rest-test-MySQL/ws/orm/target/jacoco.svg)](http://jenkins.y-op.org/job/yop.dev_rest-test-MySQL/lastBuild/jacoco)
[orm](orm/README.md "The orm module README")
- [![Coverage](http://hdmcl.no-ip.org:8081/job/yop.dev_rest-test-MySQL/ws/ioc/target/jacoco.svg)](http://jenkins.y-op.org/job/yop.dev_rest-test-MySQL/lastBuild/jacoco)
[ioc](ioc/README.md "The ioc module README")
- [![Coverage](http://hdmcl.no-ip.org:8081/job/yop.dev_rest-test-MySQL/ws/rest/target/jacoco.svg)](http://jenkins.y-op.org/job/yop.dev_rest-test-MySQL/lastBuild/jacoco)
[rest](rest/README.md "The rest module README")
- [![Coverage](http://hdmcl.no-ip.org:8081/job/yop.dev_rest-test-MySQL/ws/swaggerui/target/jacoco.svg)](http://jenkins.y-op.org/job/yop.dev_rest-test-MySQL/lastBuild/jacoco)
[swaggerui](swaggerui/README.md "The swaggerui module README")
- [demo](demo/README.md "The demo module README")
  
## Maven
Yop is available on Maven central :  
```xml
<dependency>
    <groupId>org.y-op</groupId>
    <artifactId>yop</artifactId>
    <version>0.8.0</version>
</dependency>
```  
  
## About
Yop is an **ORM tool** with a conventional REST webservice Servlet.
Webservices are described using 
[OpenAPI specifications](https://en.wikipedia.org/wiki/OpenAPI_Specification "Wikipedia → OpenAPI specification")

## Structure :  
The [ORM](orm) module brings a set of query builders with an SQL like syntax : 

```
Select   
 .from(Library.class)    
 .join(Library::getAuthors, Author::getBooks, Book::getChapters)    
 .join(Library::getEmployees)  
 .execute(connection);
  ```
  
The [REST](rest) module brings a set of annotation to directly expose the data objects as REST resources : 
```
@Rest(
  path="book",
  summary = "Rest resource for books !",
  description = "A collection of sheets of paper bound together to hinge at one edge."
)
@Table(name="book")
public class Book implements Yopable {}
``` 

A REST servlet can expose the data objects as REST resources : 
```
Wrapper wrapper = Tomcat.addServlet(context, YopRestServlet.class.getSimpleName(), new YopRestServlet());

// The data objects packages exposed as REST resources
wrapper.addInitParameter(YopRestServlet.PACKAGE_INIT_PARAM, "org.yop");

// The datasource JNDI name (or you can override the 'getConnection' method)
wrapper.addInitParameter(YopRestServlet.DATASOURCE_JNDI_INIT_PARAM, "datasource");

// The exposition path for the data objects REST resources
context.addServletMappingDecoded("/yop/rest/*", YopRestServletWithConnection.class.getSimpleName());
```

The [OpenAPI](https://www.openapis.org/ "Open API initiative") description of data objects can be generated
and exposed using a Servlet : 
```
Wrapper wrapper = Tomcat.addServlet(context, OpenAPIServlet.class.getSimpleName(), new OpenAPIServlet());

// The data objects packages exposed as REST resources
wrapper.addInitParameter(OpenAPIServlet.PACKAGE_INIT_PARAM, "org.yop");

// The exposition path for the data objects REST resources
wrapper.addInitParameter(OpenAPIServlet.EXPOSITION_PATH_PARAM, "/yop/rest");

// The exposition path for the generated OpenAPI description of the data objects REST resources
context.addServletMappingDecoded("/yop/openapi", OpenAPIServlet.class.getSimpleName());
```

## Miscellaneous / Philosophy
- Data objects describe their REST and/or ORM features.  
- CRUD behavior in REST services is conventional.  
- Data objects carry any extra CRUD behavior (i.e beyond conventional) to be exposed in REST services.  
- Explicit CRUD can be achieved using the orm module in an SQL like syntax.  
- [DAO pattern](https://en.wikipedia.org/wiki/Data_access_object "Wikipedia → DAO") sucks.  
- [DTO pattern](https://en.wikipedia.org/wiki/Data_transfer_object "Wikipedia → DTO") sucks.  
- YOP naively aims at being a straightforward **Model-Driven ORM/REST** stack.  