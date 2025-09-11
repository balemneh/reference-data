# Repository Guidelines

## Project Structure & Modules
- `reference-core`: Domain entities, repositories, bitemporal utilities.
- `reference-api`: REST controllers, DTOs, security, OpenAPI.
- `reference-events`: Outbox publisher, Kafka/Avro schemas.
- `reference-workflow`: Change request workflows (integration pending).
- `reference-loaders/*`: Source loaders (iso, genc, iata, icao, cbp-ports).
- `translation-service`: Code translation endpoints and rules.
- `catalog-integration`: OpenMetadata integration scaffolding.
- `admin-ui`: Angular 20 admin interface.
- `ops`: Docker Compose, DB, Kafka, Keycloak, Redis.
- `docs`, `scripts`, `test/e2e`.

## Build, Test, and Dev
- Build backend: `make build` (Maven clean package, skip tests).
- Run unit tests: `make test` or `./mvnw test`.
- Module tests: `./mvnw test -pl reference-core` (similarly for other modules).
- Integration tests: `make test-integration` (H2/Testcontainers profile).
- Start infra (dev): `make up-dev` or minimal: `make up-minimal`.
- Run API (dev): `make dev` (Spring Boot on `reference-api`).
- Start UI: `make ui` (installs and starts Angular app).
- Format/lint: `make format` and `make lint`.

## Coding Style & Naming
- Java 21, Spring Boot 3.x; prefer 4-space indent, 120 col.
- DTOs via MapStruct; no hard deletes (bitemporal invariants).
- IDs: UUID; natural keys as constrained attributes.
- Packages: `gov.dhs.cbp.reference.*`; tests mirror source package.
- Java classes: `PascalCase`; methods/fields: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Angular/TS follows Angular style guide; components/services `kebab-case` files.

## Testing Guidelines
- Frameworks: JUnit 5, Spring Boot Test; H2 for DB tests.
- Naming: unit `*Test`, integration `*IntegrationTest`, contract `*ContractTest`.
- Coverage: target ≥80% (Jacoco). Generate: `make coverage`.
- Run suites: `make test-all`; focused: `make test-api`, `make test-events`, `make test-loaders`.

## Commit & PR Guidelines
- Commits: Conventional Commits (e.g., `feat: add country endpoint`).
- Before PR: run `make lint`, `make test`, and update docs if needed.
- PRs include: clear description, linked issue, test evidence (logs), and screenshots for UI changes.
- Keep changes scoped per module; avoid breaking public DTOs/events without versioning.

## Security & Configuration
- Never commit secrets; copy `.env.example` to `.env` locally.
- CORS and OAuth2/OIDC are configured; provide env vars when running containers.
- Use `ops/docker-compose.yml` targets to run local dependencies.
