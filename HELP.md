# Getting Started

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.5/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.5/maven-plugin/build-image.html)
* [Spring Boot Testcontainers support](https://docs.spring.io/spring-boot/4.0.5/reference/testing/testcontainers.html#testing.testcontainers)
* [Testcontainers Postgres Module Reference Guide](https://java.testcontainers.org/modules/databases/postgres/)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.0.5/reference/actuator/index.html)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/4.0.5/reference/using/devtools.html)
* [Flyway Migration](https://docs.spring.io/spring-boot/4.0.5/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
* [JOOQ Access Layer](https://docs.spring.io/spring-boot/4.0.5/reference/data/sql.html#data.sql.jooq)
* [Testcontainers](https://java.testcontainers.org/)
* [Vaadin](https://vaadin.com/docs)

### Guides

The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
* [Creating CRUD UI with Vaadin](https://spring.io/guides/gs/crud-with-vaadin/)

### Testcontainers support

This project
uses [Testcontainers at development time](https://docs.spring.io/spring-boot/4.0.5/reference/features/dev-services.html#features.dev-services.testcontainers).

Testcontainers has been configured to use the following Docker images:

* [`postgres:latest`](https://hub.docker.com/_/postgres)

Please review the tags of the used images and set them to the same as you're running in production.

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the
parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

