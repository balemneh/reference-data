# CBP Reference Data Service

## Overview

The CBP Reference Data Service is a centralized, canonical source for reference data with bitemporal history, governance, and multi-channel distribution capabilities. It provides REST APIs, bulk snapshots, and event streaming for reference data management.

## Project Status

### ✅ Completed Features

- **Core Infrastructure**
  - Bitemporal JPA entities (Country, CodeSystem, CodeMapping, ChangeRequest, OutboxEvent)
  - Repository layer with bitemporal support
  - H2 database configuration for CI/CD testing (no Docker required)
  - Maven build system with multi-module structure

- **API Layer**
  - REST controllers with pagination and ETag support
  - CORS configuration for cross-origin requests
  - DTO mapping with MapStruct
  - Security configuration (OAuth2/OIDC ready)

- **Event System**
  - Kafka/Avro event schemas
  - Outbox pattern implementation
  - Event publisher service

- **Admin UI**
  - Angular 20 with TypeScript
  - USWDS 3.13 (US Web Design System)
  - CBP branding and government banner
  - Consolidated navigation
  - Responsive design

- **Testing**
  - All unit tests passing
  - H2 in-memory database for integration tests
  - CI/CD compatible

### ⚠️ In Progress

- **Workflow Integration**
  - Basic change request entities created
  - Needs Camunda/Flowable integration

- **Catalog Integration**
  - Structure ready
  - Needs OpenMetadata integration

### 🔄 TODO

1. Implement Airport, Port, and Carrier entities
2. Implement data loaders (ISO, GENC, IATA, ICAO, CBP)
3. Add Okta authentication
4. Complete workflow integration
5. Re-enable complex integration tests

## Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose (for local services)
- Node.js 20+ (for UI development)

### Local Development

1. **Start infrastructure services:**
```bash
docker-compose up -d postgres redis redpanda
```

2. **Build the project:**
```bash
./mvnw clean package -DskipTests
```

3. **Run tests:**
```bash
./mvnw test
```

4. **Start the API:**
```bash
docker build -t refdata-api -f Dockerfile.api-simple .
docker run -d --name refdata-api --network refdata-network -p 8081:8080 \
  -e SPRING_LIQUIBASE_ENABLED=false \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://refdata-postgres:5432/refdata \
  -e SPRING_DATASOURCE_USERNAME=refdata \
  -e SPRING_DATASOURCE_PASSWORD=refdata123 \
  refdata-api
```

5. **Start the UI:**
```bash
docker build -t refdata-ui ./admin-ui
docker run -d --name refdata-ui --network refdata-network -p 80:80 refdata-ui
```

### Service URLs

- **Admin UI**: http://localhost:80
- **API**: http://localhost:8081
- **API Documentation**: http://localhost:8081/swagger-ui
- **Postgres**: localhost:5433
- **Redis**: localhost:6380
- **Kafka/Redpanda**: localhost:19092

## Project Structure

```
/reference-data
├── reference-core/        # Domain entities, repositories, bitemporal utilities
├── reference-api/         # REST controllers, DTOs, exception handling
├── reference-events/      # Event publishing, Kafka, Avro schemas
├── reference-workflow/    # Change request workflows (Camunda/Flowable)
├── reference-loaders/     # Data loaders for various sources
│   ├── common/           # Shared loader utilities
│   ├── iso/              # ISO country codes loader
│   ├── genc/             # GENC codes loader
│   ├── iata/             # IATA airport codes loader
│   ├── icao/             # ICAO codes loader
│   └── cbp-ports/        # CBP port codes loader
├── translation-service/   # Code translation and mapping service
├── catalog-integration/   # OpenMetadata integration
├── admin-ui/             # Angular admin interface
├── docs/                 # Documentation
└── ops/                  # Deployment and operations files
```

## Testing

The project uses H2 in-memory database for testing, eliminating the need for Docker in CI/CD pipelines.

### Run all tests:
```bash
./mvnw test
```

### Run specific module tests:
```bash
./mvnw test -pl reference-core
./mvnw test -pl reference-api
./mvnw test -pl reference-events
```

### Test Configuration

- **Database**: H2 with PostgreSQL compatibility mode
- **Profile**: `integration-test` for integration tests
- **Disabled Tests**: Some Kafka and complex integration tests are disabled for CI/CD

## API Examples

### Get country by code:
```http
GET /v1/countries?codeSystem=ISO3166-1&code=US
```

### Get dataset records with changes:
```http
GET /v1/datasets/countries/records?changedSinceVersion=2025.08.01
```

### Translate codes between systems:
```http
GET /v1/translate?fromSystem=ISO3166-1&toSystem=CBP-COUNTRY5&code=US&asOf=2025-08-01
```

## Development Guidelines

### Coding Standards

- Java 21 with Spring Boot 3.3.5
- Angular 20 with TypeScript 5.8
- Follow conventional commits for version control
- Maintain bitemporal invariants (no hard deletes)
- Use UUID for entity IDs

### Test Policy

- Write unit tests for all business logic
- Integration tests use H2 database
- Mock external services in tests
- Maintain >80% code coverage

### Security

- OAuth2/OIDC via Okta (configuration required)
- CORS configured for known origins
- Signed webhooks for external callbacks
- No secrets in code (use environment variables)

## CI/CD

The project is configured for enterprise CI/CD environments without Docker-in-Docker support.

### Build Pipeline

```yaml
steps:
  - mvn clean compile
  - mvn test
  - mvn package -DskipTests
```

### Key Features

- H2 database for testing (no Docker required)
- All dependencies resolved from Maven Central
- Tests complete in <5 minutes
- Supports GitHub Actions, Jenkins, GitLab CI

## Contributing

1. Read `CLAUDE.md` for detailed development guidelines
2. Follow the coding standards
3. Write tests for new features
4. Submit PRs with clear descriptions

## Documentation

- [Architecture Overview](docs/architecture.md)
- [CI/CD Configuration](docs/ci-cd-configuration.md)
- [API Documentation](http://localhost:8081/swagger-ui)
- [Development Guidelines](CLAUDE.md)

## License

U.S. Government Work

## Contact

For questions or support, contact the CBP Reference Data team.