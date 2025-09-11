# CLAUDE.md

> **Purpose**: Give Claude Code everything it needs to reliably develop and maintain the **CBP Reference Data Service** using the hybrid architecture in our design doc. This file contains goals, constraints, workflows, commands, file map, coding standards, test policy, security posture, and agent-specific guardrails.
>
> **Read me first**: When you start a session, **read this file end‑to‑end**, then follow the workflows below. Always **plan → get approval → implement → test → commit**.

---

## 0) Project summary

- **Mission**: Centralize canonical reference data (countries, ports, airports, carriers, units, languages, crosswalks) with bitemporal history, governance, and multi‑channel distribution (REST, bulk snapshots, events).
- **Chosen architecture**: Centralized **Postgres bitemporal** core + **Kafka outbox/backbone** for replay + **catalog‑driven distribution** (OpenMetadata → generated views/UDFs/dbt) + optional **graph sidecar** for complex crosswalks. See `docs/architecture.md` and the canvas for diagrams.
- **Implementation status**:
  - ✅ `reference-core`: JPA entities (Country, CodeSystem, CodeMapping, ChangeRequest, OutboxEvent), repositories, bitemporal utilities
  - ✅ `reference-api`: Spring controllers, DTOs (MapStruct), CORS config, security config
  - ✅ `reference-events`: Outbox publisher, Kafka config, Avro schemas
  - ⚠️ `reference-workflow`: Basic change request entities (needs Camunda/Flowable integration)
  - ✅ `reference-loaders/*`: Directory structure ready for ISO/GENC/IATA/ICAO/CBP loaders
  - ✅ `translation-service`: `/translate` endpoints implemented with caching
  - 🔄 `catalog-integration`: Structure ready (needs OpenMetadata integration)
  - ✅ `admin-ui`: Angular 20 with USWDS 3.13, government banner, CBP branding, navigation consolidated

---

## 1) Guardrails for Claude (you must follow these)

1. **Plan first**: Before editing files, produce a concrete plan with affected files, functions, schema changes, and a test approach. Get confirmation.
2. **Small, reversible steps**: Prefer a series of small PRs. After each step, run the targeted tests, then commit with a clear message.
3. **Never** hand‑edit generated code or migrations that are marked as generated. Use the generator path documented here.
4. **Preserve bitemporal invariants**: No hard deletes; updates create new versions. Maintain `valid_from/valid_to` and `recorded_at` correctly.
5. **Keep compatibility**: Don't change public DTOs/events without updating versioning, OpenAPI, Avro schemas, and compatibility tests.
6. **Secrets & data**: Use `.env.example`; never commit real secrets or production connection strings.
7. **Ask for permission on risky tools**: destructive bash, `kubectl delete`, cloud modifiers, or data‑dropping migrations require explicit approval.
8. **Prefer local caches & idempotency** in consumers; outbox events must be exactly‑once with dedupe keys.

---

## 2) Local development setup

**Prereqs**: Java 21, Docker, Docker Compose, Node 20 (for `admin-ui`), `gh` CLI, `jq`, `make`. Optional: `asdf`.

**Quick start**

```bash
# Maven build (use existing local build)
./mvnw clean package -DskipTests -Dmaven.test.skip=true

# Docker deployment
docker-compose up -d postgres redis redpanda  # Start infrastructure
docker build -t refdata-ui ./admin-ui         # Build UI image
docker build -t refdata-api -f Dockerfile.api-simple .  # Build API image

# Run containers
docker run -d --name refdata-ui --network refdata-network -p 80:80 refdata-ui
docker run -d --name refdata-api --network refdata-network -p 8081:8080 \
  -e SPRING_LIQUIBASE_ENABLED=false \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://refdata-postgres:5432/refdata \
  -e SPRING_DATASOURCE_USERNAME=refdata \
  -e SPRING_DATASOURCE_PASSWORD=refdata123 \
  refdata-api
```

**Service URLs (local)**

- UI (Admin): [http://localhost:80](http://localhost:80) ✅ Running with consolidated navigation
- API: [http://localhost:8081](http://localhost:8081) ✅ Running (Liquibase disabled for tests)
- OpenAPI UI: [http://localhost:8081/swagger-ui](http://localhost:8081/swagger-ui)
- Postgres: localhost:5433 ✅ Running
- Redis: localhost:6380 ✅ Running
- Kafka/Redpanda: localhost:19092 ✅ Running
- Okta: Configured via environment (see .env.example for OKTA_* variables)

**Environment**

- Copy `.env.example` → `.env`. Use safe defaults. Real secrets go to your secret manager outside this repo.

---

## 3) Commands Claude may run (safe allowlist)

> Add these to your Claude Code allowlist. If a command is not listed, ask for approval.

- **Read‑only**: `git status`, `git diff`, `git log`, `./gradlew tasks`, `./gradlew test`, `./mvnw -q -DskipITs`, `docker compose ps`, `psql -V`.
- **Edit/build**: `./gradlew build -x test`, `./gradlew :reference-api:test`, `./mvnw -q package -DskipTests`, `npm run -w admin-ui dev`.
- **DB/Testcontainers**: `./gradlew integrationTest`, `./gradlew :reference-events:test`.
- **Infra (non‑destructive)**: `docker compose up -d`, `docker compose logs -f`, `docker compose stop`.
- **Codegen**: `./gradlew openApiGenerate`, `./gradlew avroGenerate`, custom `./scripts/refsynth/generate.sh`.
- **Lint/format**: `./gradlew spotlessApply`, `npm run -w admin-ui lint`.
- **Utility**: `jq`, `awk`, `sed -n`, `grep -R`.

> **Never without approval**: `kubectl delete`, `terraform apply`, dropping DBs, force‑push to protected branches, rotating secrets.

---

## 4) Repository map

```
/ (root)
  CLAUDE.md                  ← this file
  /docs                      ← architecture, ADRs, diagrams
  /config
    application-*.yml        ← Spring profiles
    opa/                     ← OPA policies for CR checks
    avro/                    ← event schemas
  /reference-core            ← domain, JPA, bitemporality utils
  /reference-api             ← controllers, DTOs, exception model, OpenAPI config
  /reference-events          ← outbox, publisher, schema registry
  /reference-workflow        ← Camunda/Flowable processes, listeners
  /reference-loaders         ← iso/, genc/, iata/, icao/, cbp-ports/
  /translation-service       ← translate endpoints, rules, caches
  /catalog-integration       ← OpenMetadata client + RefSynth codegen
  /admin-ui                  ← Angular 20 admin UI (uses USWDS 3.13)
  /ops
    docker-compose.yml       ← postgres, kafka/redpanda, schema-registry, redis, openmetadata
    migrations/              ← liquibase/flyway changelogs
  /scripts                   ← dev scripts (seed, check, refsynth)
  /test                      ← end‑to‑end and contract tests
```

**Key files to study first**

- `reference-core/src/main/java/.../Bitemporal.java` – helpers to write/read `*_v` tables.
- `reference-events/.../OutboxPublisher.java` – transactional outbox implementation.
- `reference-api/.../CountriesController.java` – exemplar REST patterns (ETags, pagination).
- `reference-loaders/iso/...` – source ingestion + staging + diff.
- `translation-service/.../TranslateController.java` – translation contract & rule execution.
- `catalog-integration/.../RefSynthRunner.java` – generates views/UDF/dbt from catalog tags.

---

## 5) Coding standards

- **Backend**: Java 21, Spring Boot 3.3.5, Spring Data JPA, MapStruct 1.6.3, Liquibase 4.29.2.
- **Frontend**: Angular 20.1, TypeScript 5.8, RxJS 7.8, USWDS 3.13 (US Web Design System).
  - **UI Features**: Government banner, CBP logo & branding, navy gradient header (#003366 → #005a9c)
  - **Components**: Countries, Change Requests workflow
- **Events**: Kafka 3.8.0 with Avro 1.12.0 + Schema Registry (compatibility = BACKWARD).
- **HTTP**: OpenAPI via springdoc. Use ETags, conditional GET, `Cache-Control`. Use problem+json error shape with `traceId`.
- **Bitemporal model**: versioned `*_v` tables + `*_current` views. Never hard‑delete. Write change as an insert with new `version`.
- **ID rules**: Stable UUID surrogate keys. Natural keys (ISO codes etc) are attributes with unique constraints per `code_system`.
- **Crosswalks**: `code_mapping` holds system↔system with `valid_*`, `rule_id`, `confidence`.
- **Validation**: Hibernate Validator on DTOs; database constraints mirror DTO invariants.
- **Security**: OAuth2/OIDC via Okta; mTLS for internal services; signed webhooks (HMAC) for external callbacks.
- **Search**: Full-text search via Postgres (pg_trgm, GIN indexes); no separate search infrastructure needed.
- **Observability**: OpenTelemetry tracing; logs are structured JSON; metrics via Micrometer.
- **Commit style**: Conventional Commits; keep changesets small; include "why" in body.

---

## 6) Test policy (run these after each change)

- **Unit** (`test`): entities, services, rules; aim for fast feedback.
- **Integration** (`integrationTest`): H2 in-memory database for CI/CD; verify outbox → topic, ETags, pagination.
- **Contract**: Spring Cloud Contract for REST; schema‑compat tests for Avro.
- **Approval tests**: loaders produce deterministic diffs from fixture drops.
- **Replay tests**: reconstruct "as‑of" snapshots and compare to expected.

**Test Configuration**
- **Database**: H2 in-memory with PostgreSQL compatibility mode (no Docker required)
- **Kafka**: Tests requiring Kafka are disabled for CI/CD (marked with @Disabled)
- **Spring Context**: Uses main application configuration with H2 overrides

**Shortcuts**

```bash
./mvnw clean test                     # all unit tests
./mvnw test -pl reference-core        # core module tests
./mvnw test -pl reference-api         # API tests (integration tests disabled)
./mvnw test -pl reference-events      # event tests (Kafka tests disabled)
```

---

## 7) Common workflows for Claude

### 7.1 Explore → Plan → Code → Commit

1. **Explore** relevant files. Do **not** write code yet. Summarize findings.
2. **Plan**: propose a stepwise approach and tests. Ask for confirmation.
3. **Code**: implement incrementally; keep diffs small; run focused tests.
4. **Commit**: descriptive message; open PR if requested. Include screenshots/logs for UIs or CLI output.

### 7.2 Add a new dataset (e.g., languages)

- Create JPA entities & `*_v` table + `*_current` view.
- Add loader module: staging schema, normalization, diff logic.
- Add workflow steps and OPA rules (policies) for approvals.
- Expose REST endpoints; add OpenAPI definitions and contract tests.
- Publish events; add schema; ensure outbox wiring & topic ACLs.
- Seed sample data; add bulk snapshot inclusion; update docs.

### 7.3 Add a new code system or derived mapping

- Register code system in `code_system` with owner, description.
- Add `mapping_rule` and implementation; generate `code_mapping` rows.
- Expose `/translate` pathway; add tests for split/merge and deprecated codes.
- Emit `mapping-changes` events and update consumer templates in `catalog-integration`.

### 7.4 Generate consumer artifacts from catalog tags

- Pull tags/lineage from OpenMetadata.
- Run RefSynth to generate per‑engine views/UDF/dbt models.
- Register artifacts back in the catalog; open PRs for target repos if needed.

### 7.5 Create a deprecation window

- Mark mapping with future `valid_to`; add deprecation notice event.
- Update policy to block re‑use; provide migration SQL templates.

---

## 8) API & events (contracts)

- **REST examples**

```http
GET /v1/countries?codeSystem=ISO3166-1&code=US
GET /v1/datasets/countries/records?changedSinceVersion=2025.08.01
GET /v1/translate?fromSystem=ISO3166-1&toSystem=CBP-COUNTRY5&code=US&asOf=2025-08-01
```

- **Event shape**: see `config/avro/` for `UPSERT`/`DELETE`/`CODE_MAPPING_UPSERT`. Always include `datasetVersion`, `effectiveFrom/to`, `crId`.

---

## 9) Data & migration policy

- Use Liquibase/Flyway changelogs; one PR = one migration; CI applies on ephemeral DB first.
- Never modify old changelogs; create a new one. Provide rollback if feasible.
- Large backfills: run via loader jobs with batch size limits; throttle to avoid topic lag.

---

## 10) Security & compliance

- Default‑deny RBAC for curation actions; read is open to authenticated roles.
- All change requests are audited (`who/what/when/why`).
- Webhooks signed with HMAC; rotate secrets; store only hashes of source drops.
- For local dev, configure Okta application credentials in `.env` (see `.env.example`).

---

## 11) Claude‑specific configuration

### 11.1 CLAUDE.md placement & tuning

- Keep this file concise and **iteratively tuned** as our workflows evolve. Use the `#` prefix in Claude Code to propose additions; commit them.
- Duplicate/override `CLAUDE.md` in submodules (e.g., under `reference-loaders/`) when local conventions differ.
- A `~/.claude/CLAUDE.md` may hold your personal shortcuts; do not commit secrets.

### 11.2 Allowed tools baseline

- Always allow: `Edit`, `ReadFile`, `WriteFile`.
- Allow `Bash` for the commands listed in §3. Ask before anything else.
- Add `gh` CLI for GitHub operations (issue/PR). Add project MCP servers in `.mcp.json` (see below).

### 11.3 Custom slash commands (add under `.claude/commands/`)

Create the following files:

``

```
Please analyze and fix the GitHub issue: $ARGUMENTS.

Steps:
1) Use `gh issue view $ARGUMENTS` to read the issue.
2) Search code for relevant files; propose a plan; ask approval.
3) Implement minimal diff; write tests; run only affected suites.
4) Commit with Conventional Commit message.
5) Open PR via `gh pr create` with summary and test output.
```

``

```
Add a new dataset named $ARGUMENTS to the canonical store.
Deliverables: entities + migrations + loader + REST endpoints + events + tests + docs.
Follow §7.2 in CLAUDE.md. Ask for confirmation before schema changes.
```

``

```
Generate consumer views/UDF/dbt models from OpenMetadata tags.
1) Run catalog-integration RefSynth.
2) Stage artifacts under /generated.
3) Open PRs or write to /ops/sql/*.md with engine-specific install instructions.
```

### 11.4 MCP configuration (optional but recommended)

Create `.mcp.json` at repo root:

```json
{
  "mcpServers": {
    "github": {"command": "mcp-github", "args": []},
    "puppeteer": {"command": "mcp-puppeteer", "args": []},
    "sentry": {"command": "mcp-sentry", "args": []},
    "openmetadata": {"command": "mcp-openmetadata", "args": ["--url","http://localhost:8585"]}
  }
}
```

> Use MCP for access‑controlled systems (clouds, prod data) instead of raw CLIs. Keep `.mcp.json` in the repo to share tools safely.

### 11.5 Headless mode (CI/automation)

Example GitHub Action job using headless mode:

```yaml
- name: Claude Code subjectivity linter
  run: |
    claude -p "Review this diff for naming, comments, misleading code. Output SARIF." \
      --input-from git --output-format stream-json > claude-lint.json
```

---

## 12) Current issues & next steps

### Known issues

- ⚠️ **Integration tests**: Some complex integration tests disabled due to Spring context conflicts
- ⚠️ **Missing entities**: Airport, Port, and Carrier entities not yet implemented
- ℹ️ **Kafka tests**: Disabled for CI/CD environments (require embedded Kafka)

### Completed features

- ✅ Full Angular UI with consolidated navigation (no duplicates)
- ✅ H2 database configuration for CI/CD testing (no Docker required)
- ✅ All unit tests passing
- ✅ CORS configuration for API access
- ✅ Docker deployment for all services
- ✅ Bitemporal JPA entities and repositories
- ✅ REST controllers with pagination and ETags
- ✅ Kafka/Avro event schemas
- ✅ Maven build system (replaced Gradle references)

### Next priorities

1. Implement Airport, Port, and Carrier entities
2. Implement data loaders (ISO, GENC, IATA, etc.)
3. Add authentication with Okta
4. Complete workflow integration
5. Re-enable integration tests with proper configuration

---

## 13) Glossary / invariants

- **Bitemporal**: `valid_from/valid_to` (business time) vs `recorded_at` (system time). Corrections set `is_correction=true` and a new `version`.
- **Outbox**: DB table written in a TX with the change → publisher relays to Kafka exactly once.
- **Code system**: Named set of codes (e.g., `ISO3166-1`, `CBP-COUNTRY5`). Never reuse codes within a system.
- **Crosswalk**: Mapping between code systems; may be 1\:N with confidence and validity windows.

---

## 14) When in doubt

- Ask for a **plan** and **explicit approval**.
- Default to **non‑destructive** operations and **feature flags**.
- Prefer adding **tests** over expanding the blast radius.
- Keep this file updated—propose changes with `#` in Claude Code and commit after review.