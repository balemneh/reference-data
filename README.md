# CBP Reference Data Service

Centralized, canonical source for reference data with bitemporal history, governance, and multi-channel distribution.

## 🚀 Quick Start

```bash
# Build the project
./mvnw clean package -DskipTests

# Start with Docker Compose
docker-compose up -d

# Or run locally with H2 database
cd reference-api
../mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

API runs on: http://localhost:8082
UI runs on: http://localhost:80

## 📚 Documentation

- **[CLAUDE.md](./CLAUDE.md)** - Primary development reference (start here!)
- **[Frontend README](./frontend/README.md)** - UI configuration and development
- **[Design Specs](./docs/design/)** - UI/UX design documentation

## 🏗️ Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Angular   │────▶│  REST API   │────▶│  PostgreSQL │
│     UI      │     │ Spring Boot │     │ (Bitemporal)│
└─────────────┘     └─────────────┘     └─────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │    Kafka    │
                    │   Events    │
                    └─────────────┘
```

## 🔑 Key Features

- **Bitemporal Data**: Track business time and system time
- **Multi-Channel**: REST APIs, Kafka events, bulk exports
- **Data Governance**: Change request workflows
- **Code Translation**: Map between ISO, GENC, CBP systems
- **US Gov Compliant**: USWDS design, accessibility standards

## 📦 Modules

| Module | Purpose | Status |
|--------|---------|--------|
| reference-core | JPA entities, repositories | ✅ Complete |
| reference-api | REST controllers, DTOs | ✅ Complete |
| reference-events | Kafka/Avro publishing | ✅ Complete |
| reference-loaders | Data ingestion | 🔄 In Progress |
| translation-service | Code mapping | ✅ Complete |
| catalog-integration | OpenMetadata | 🔄 In Progress |
| frontend | Angular UI | ✅ Complete |

## 🧪 Testing

```bash
# Unit tests
./mvnw test

# Integration tests (with H2)
./mvnw verify

# Run specific module
./mvnw test -pl reference-core
```

## 🔧 Development

See **[CLAUDE.md](./CLAUDE.md)** for:
- Development setup
- Coding standards
- Test policies
- Workflow guidelines
- Command reference

## 📄 License

U.S. Government Work - Public Domain