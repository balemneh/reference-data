# Reference API

This is the backend API for the CBP Reference Data Service. It is a Spring Boot application that provides a RESTful API for managing reference data.

## Features

*   RESTful API for CRUD operations on reference data
*   Integration with Kafka for event-driven communication
*   Integration with Redis for caching
*   Integration with Keycloak for authentication and authorization
*   Database migrations with Liquibase
*   API documentation with Swagger

## Configuration

The application is configured using the `application.yml` file located in `src/main/resources`. The following properties can be configured:

*   `spring.datasource.url`: The URL of the PostgreSQL database
*   `spring.datasource.username`: The username for the database
*   `spring.datasource.password`: The password for the database
*   `spring.kafka.bootstrap-servers`: The comma-separated list of Kafka broker URLs
*   `spring.kafka.producer.properties.schema.registry.url`: The URL of the Kafka schema registry
*   `spring.security.oauth2.resourceserver.jwt.issuer-uri`: The URI of the Keycloak issuer

## Building and Running

The application can be built and run using Maven.

*   **Build the application:**

    ```bash
    mvn clean package
    ```

*   **Run the application:**

    ```bash
    java -jar target/reference-api-*.jar
    ```

## API Documentation

The API documentation is available at [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) when the application is running.
