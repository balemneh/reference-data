# CBP Reference Data Service

## Overview

The CBP Reference Data Service is a centralized, canonical source for reference data with bitemporal history, governance, and multi-channel distribution capabilities. It provides REST APIs, bulk snapshots, and event streaming for reference data management.

### Key Features

- **Bitemporal Data Management**: Track both business time and system time for complete audit history
- **Multi-Channel Distribution**: REST APIs, Kafka events, and bulk data exports
- **Data Governance**: Change request workflows with approval processes
- **Code Translation**: Map between different coding systems (ISO, GENC, CBP, etc.)
- **Feature Flags**: Runtime configuration management via FF4J
- **US Government Compliance**: USWDS design system and accessibility standards

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

- **Frontend UI**
  - Angular 20 with TypeScript 5.8
  - USWDS 3.13 (US Web Design System)
  - CBP branding and government banner
  - Consolidated navigation
  - Responsive design
  - Feature flag management

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

1. **Data Entities**
   - Implement Airport entity and repository
   - Implement Port entity and repository
   - Implement Carrier entity and repository

2. **Data Loaders**
   - Complete ISO country codes loader
   - Implement GENC codes loader
   - Implement IATA airport codes loader
   - Implement ICAO codes loader
   - Implement CBP port codes loader

3. **Authentication & Security**
   - Integrate Okta OAuth2/OIDC
   - Implement role-based access control (RBAC)
   - Add API key authentication for service accounts

4. **Workflow & Governance**
   - Integrate Camunda or Flowable for workflow management
   - Implement approval chains for change requests
   - Add OPA (Open Policy Agent) for policy enforcement

5. **Testing & Quality**
   - Re-enable complex integration tests
   - Add performance benchmarks
   - Implement contract testing

## Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose (for local services)
- Node.js 20+ (for UI development)

### Local Development

#### Option 1: Using H2 Database (No PostgreSQL Required)

1. **Build the project:**
```bash
./mvnw clean package -DskipTests
```

2. **Run the API with H2 embedded database:**
```bash
cd reference-api
../mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```
- API runs on: http://localhost:8082
- H2 console at: http://localhost:8082/h2-console
- JDBC URL: `jdbc:h2:mem:refdata`
- Username: `sa`
- Password: (leave blank)

3. **Start the Frontend UI:**
```bash
cd frontend
npm install
npm start
```

#### Option 2: Using Local PostgreSQL Installation

1. **Install PostgreSQL locally** (if not already installed):
```bash
# macOS
brew install postgresql@15
brew services start postgresql@15

# Ubuntu/Debian
sudo apt-get install postgresql-15
sudo systemctl start postgresql
```

2. **Create database and user:**
```bash
psql -U postgres
CREATE DATABASE refdata;
CREATE USER refdata_user WITH PASSWORD 'refdata_pass';
GRANT ALL PRIVILEGES ON DATABASE refdata TO refdata_user;
\q
```

3. **Run the API:**
```bash
cd reference-api
../mvnw spring-boot:run \
  -Dspring.datasource.url="jdbc:postgresql://localhost:5432/refdata" \
  -Dspring.datasource.username=refdata_user \
  -Dspring.datasource.password=refdata_pass
```

4. **Start the Frontend UI:**
```bash
cd frontend
npm install
npm start
```

#### Option 3: Using Docker for Dependencies Only

1. **Start only infrastructure services:**
```bash
docker-compose up -d postgres redis redpanda
```

2. **Run the API locally (not in Docker):**
```bash
cd reference-api
../mvnw spring-boot:run
```

3. **Start the Frontend UI:**
```bash
cd frontend
npm install
npm start
```

### Service URLs

#### With H2 (Development)
- **Frontend UI**: http://localhost:4200
- **API**: http://localhost:8082
- **API Documentation**: http://localhost:8082/swagger-ui
- **H2 Console**: http://localhost:8082/h2-console

#### With Docker (Production-like)
- **Frontend UI**: http://localhost:4200
- **API**: http://localhost:8081
- **API Documentation**: http://localhost:8081/swagger-ui
- **Postgres**: localhost:5433
- **Redis**: localhost:6380
- **Kafka/Redpanda**: localhost:19092

## Project Structure

```
reference-data/
├── frontend/              # Angular 20.1 frontend application
│   ├── src/
│   │   ├── app/          # Application components
│   │   ├── assets/       # Static assets
│   │   └── styles/       # Global styles and themes
│   └── package.json      # Frontend dependencies
├── reference-core/        # Domain entities, repositories, bitemporal utilities
│   └── src/main/java/    # JPA entities, repositories
├── reference-api/         # REST controllers, DTOs, exception handling
│   └── src/main/java/    # Controllers, services, mappers
├── reference-events/      # Event publishing, Kafka, Avro schemas
│   └── src/main/java/    # Outbox pattern, publishers
├── reference-workflow/    # Change request workflows (Camunda/Flowable)
│   └── src/main/java/    # Workflow processes
├── reference-loaders/     # Data loaders for various sources
│   ├── common/           # Shared loader utilities
│   ├── iso/              # ISO country codes loader
│   ├── genc/             # GENC codes loader
│   ├── iata/             # IATA airport codes loader
│   ├── icao/             # ICAO codes loader
│   └── cbp-ports/        # CBP port codes loader
├── translation-service/   # Code translation and mapping service
├── catalog-integration/   # OpenMetadata integration
├── config/               # Configuration files
├── docs/                 # Architecture documentation
├── ops/                  # Deployment configurations
│   └── secrets/          # Secret templates
├── docker-compose.yml    # Local development services
├── pom.xml              # Parent Maven configuration
└── CLAUDE.md            # Development guidelines
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
- Angular 20.1 with TypeScript 5.8
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

## Technologies Used

### Backend
- **Java 21** - Latest LTS version
- **Spring Boot 3.3.5** - Microservices framework
- **Spring Data JPA** - Data persistence
- **PostgreSQL** - Primary database
- **H2 Database** - In-memory testing
- **Apache Kafka** - Event streaming
- **Redis** - Caching layer
- **MapStruct 1.6.3** - DTO mapping
- **Liquibase 4.29.2** - Database migrations
- **FF4J** - Feature flag management
- **Maven** - Build management

### Frontend
- **Angular 20.1** - Frontend framework
- **TypeScript 5.8** - Type-safe JavaScript
- **RxJS 7.8** - Reactive programming
- **USWDS 3.13** - US Web Design System
- **Chart.js 4.5** - Data visualization
- **ng2-charts** - Angular chart components

### Infrastructure
- **Docker** - Containerization
- **Docker Compose** - Local orchestration
- **OpenTelemetry** - Observability
- **Micrometer** - Metrics collection

## Contributing

1. Read `CLAUDE.md` for detailed development guidelines
2. Follow the coding standards
3. Write tests for new features
4. Submit PRs with clear descriptions
5. Ensure all tests pass before submitting

## Documentation

- [Architecture Overview](docs/architecture.md)
- [CI/CD Configuration](docs/ci-cd-configuration.md)
- [API Documentation](http://localhost:8081/swagger-ui)
- [Development Guidelines](CLAUDE.md)

## License

U.S. Government Work

## Contact

For questions or support, contact the CBP Reference Data team.