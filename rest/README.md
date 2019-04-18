# YOP !
[![Build Status](http://hdmcl.no-ip.org:8081/job/yop-test-MySQL/badge/icon)](http://jenkins.y-op.org/job/yop-test-MySQL/)
[![Coverage](http://hdmcl.no-ip.org:8081/job/yop-test-MySQL/ws/rest/target/jacoco.svg)](http://jenkins.y-op.org/job/yop-test-MySQL/lastBuild/jacoco)

Unrestful REST for YOP !

## Maven
Yop is available on Maven central :  
```xml
<dependency>
    <groupId>org.y-op</groupId>
    <artifactId>rest</artifactId>
    <version>0.9.0</version>
</dependency>
```    

## About

This module simply defines an HttpServlet that will scan the @Rest annotated Yopables.
For each one of them, the Servlet will answer the configured path.
Standard behavior for each Yopable : 
- GET
  - configured_path : get all entities
  - configured_path/{id} : get entity by id
- DELETE 
  - configured_path : delete all entities (not confident about that)
  - configured_path/{id} : get entity by id
- PUT
  - configured_path : upsert the given entities (not idempotent)
- UPSERT
  - configured_path : upsert the given entities (not idempotent)
- POST
  - execute the serialized request (Select/Upsert/Delete)  
  
Since PUT idempotence depends on the passed data, use the custom UPSERT method if PUT idempotence is required.

You can define any extra behavior by adding a @Rest method on your Yopable.
A @Rest method can receive, in any order : 
- the request path (@PathParam)
- the request entity (@ContentParam)
- the request parameters
- the request headers
- the underlying connection (configured in the servlet) 
  
Example : 
```java
@Rest(path = "pojo")
public class Pojo extends org.yop.orm.simple.model.Pojo {
	@Rest(path = "search")
	public static String search(IConnection connection, Header[] headers) {
		Pojo first = Select.from(Pojo.class).uniqueResult(connection);
		return JSON.from(Pojo.class).onto(first).toJSON();
	}

	@Rest(path = "search", methods = "POST")
	public static String search(
		IConnection connection,
		Header[] headers,
		@ContentParam String content,
		@PathParam String path,
		NameValuePair[] parameters) {

		Pojo first = Select.from(Pojo.class).uniqueResult(connection);
		return JSON.from(Pojo.class).onto(first).toJSON();
	}
}
```

# Note
For too long have I been reading articles laughing at people who *naively* want to map HTTP methods to CRUD.   
What fools they are, ha ha ha! They did not think about idempotence, complicated bloated stuff and such !  
Well the thing is HTTP is definitely not designed for doing database CRUD webservices :
- HTTP was built to expose/manage resources from a filesystem.
- POST behavior is definitely set apart.
- Idempotence is a feature you will rarely need.
- You can define any custom HTTP method.  
  
So here is what I came up with :
- Keep the POST method specificity for executing any type of serialized request.
- Introduce an UPSERT method with no idempotence requirement.
- PUT *by default* does UPSERT and is not idempotent. Feel free to override.
- GET/DELETE are OK for Select/Delete.  
- **Any conventional behavior can be overloaded.**

