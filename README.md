# CBP Reference Data Service

This is a full-stack application that provides a centralized service for managing reference data. It consists of a Java/Spring Boot backend, an Angular frontend, and several backing services.

## Project Overview

The project is a multi-module Maven project. The backend is built with Java 21, Spring Boot, and Spring Cloud. It uses PostgreSQL as its database, with Liquibase for database migrations. It also integrates with Kafka for event-driven communication and Redis for caching. Keycloak is used for authentication and authorization.

The frontend is an Angular application that uses the US Web Design System (USWDS) for its UI components.

The entire application is containerized using Docker and can be orchestrated with Docker Compose.

## Getting Started

### Prerequisites

*   Java 21
*   Maven
*   Docker
*   Docker Compose

### Build and Run

1.  **Build the project:**

    ```bash
    make build
    ```

2.  **Start the application and its dependencies:**

    ```bash
    make up
    ```

3.  **The application will be available at [http://localhost:4200](http://localhost:4200).**

### Development

*   **Run the backend in development mode:**

    ```bash
    make dev
    ```

*   **Run the frontend in development mode:**

    ```bash
    cd frontend
    npm start
    ```

### Testing

*   **Run all tests:**

    ```bash
    make test-all
    ```

*   **Run unit tests:**

    ```bash
    make test
    ```

*   **Run integration tests:**

    ```bash
    make test-integration
    ```

## Additional Information

*   **API Documentation:** The API documentation is available at [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) when the application is running.
*   **Kafka UI:** The Kafka UI is available at [http://localhost:8082](http://localhost:8082) when the application is running.
