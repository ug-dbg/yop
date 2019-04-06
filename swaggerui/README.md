# YOP ! 
[![Build Status](http://hdmcl.no-ip.org:8081/job/yop.dev_rest-test-MySQL/badge/icon)](http://jenkins.y-op.org/job/yop.dev_rest-test-MySQL/)
[![Coverage](http://hdmcl.no-ip.org:8081/job/yop.dev_rest-test-MySQL/ws/swaggerui/target/jacoco.svg)](http://jenkins.y-op.org/job/yop.dev_rest-test-MySQL/lastBuild/jacoco)
  
A SwaggerUI servlet that exposes the resources from [webjars maven module](https://mvnrepository.com/artifact/org.webjars/swagger-ui).
  
The servlet is configurable to set the OpenAPI description URL.  

## Maven
Yop is available on Maven central :  
```xml
<dependency>
    <groupId>org.y-op</groupId>
    <artifactId>swaggerui</artifactId>
    <version>0.9.0</version>
</dependency>