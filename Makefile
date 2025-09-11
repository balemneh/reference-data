.PHONY: help bootstrap up down migrate seed run build test clean install dev ui stop restart ps db-console kafka-console redis-cli

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  %-15s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

bootstrap: ## Install git hooks and check required tools
	@echo "Checking required tools..."
	@command -v java >/dev/null 2>&1 || { echo "Java 21 is required but not installed."; exit 1; }
	@command -v docker >/dev/null 2>&1 || { echo "Docker is required but not installed."; exit 1; }
	@command -v docker-compose >/dev/null 2>&1 || command -v docker compose >/dev/null 2>&1 || { echo "Docker Compose is required but not installed."; exit 1; }
	@command -v mvn >/dev/null 2>&1 || { echo "Maven is required but not installed."; exit 1; }
	@echo "All required tools are installed."
	@if [ -f .env.example ] && [ ! -f .env ]; then cp .env.example .env; echo "Created .env from .env.example"; fi

up: ## Start Docker Compose infrastructure
	docker-compose -f ops/docker-compose.yml up -d

down: ## Stop Docker Compose infrastructure
	docker-compose -f ops/docker-compose.yml down

migrate: ## Run database migrations
	@echo "Running Liquibase migrations..."
	cd reference-core && mvn liquibase:update

seed: ## Seed initial data
	@echo "Seeding initial data..."
	@if [ -f scripts/seed.sh ]; then ./scripts/seed.sh; else echo "Seed script not found"; fi

build: ## Build all modules
	mvn clean package -DskipTests

test: ## Run unit tests only
	mvn test

test-unit: test ## Alias for test

test-integration: ## Run integration tests using Testcontainers
	mvn verify -DskipUnitTests -Dspring.profiles.active=integration-test

test-api: ## Run API integration tests
	mvn test -pl reference-api -Dtest="*IntegrationTest"

test-events: ## Run event publishing integration tests
	mvn test -pl reference-events -Dtest="*IntegrationTest"

test-loaders: ## Run data loader tests
	mvn test -pl reference-loaders/iso,reference-loaders/genc,reference-loaders/iata,reference-loaders/icao,reference-loaders/cbp-ports

test-e2e: ## Run end-to-end tests
	@if [ -d "test/e2e" ]; then \
		cd test/e2e && npm test; \
	else \
		echo "E2E tests not set up yet. Use 'make setup-e2e' first."; \
	fi

test-contract: ## Run contract tests (Spring Cloud Contract)
	mvn test -Dtest="*ContractTest"

test-performance: ## Run performance tests
	@echo "Running performance tests..."
	mvn test -Dtest="*PerformanceTest" -DfailIfNoTests=false

test-all: test test-integration test-contract ## Run all test suites

test-coverage: ## Generate test coverage report
	mvn clean test jacoco:report
	@echo "Coverage report: target/site/jacoco/index.html"

run: build ## Build and run Spring Boot applications
	@echo "Starting Reference API..."
	cd reference-api && mvn spring-boot:run &
	@echo "Services starting... Check logs for status"

clean: ## Clean build artifacts
	mvn clean
	rm -rf target/

logs: ## Show application logs
	docker-compose -f ops/docker-compose.yml logs -f

status: ## Check service status
	@echo "Infrastructure status:"
	docker-compose -f ops/docker-compose.yml ps
	@echo ""
	@echo "Application status:"
	@curl -s http://localhost:8080/actuator/health 2>/dev/null || echo "API not running"

install: ## Install Maven dependencies
	mvn clean install -DskipTests

dev: ## Run in development mode with hot reload
	mvn spring-boot:run -pl reference-api -Dspring-boot.run.profiles=dev

ui: ## Start the Admin UI
	cd admin-ui && npm install && npm start

stop: ## Stop all running services
	docker-compose -f ops/docker-compose.yml stop
	@pkill -f spring-boot:run || true

restart: down up migrate ## Restart infrastructure and migrate

ps: ## Show running Java processes
	@jps -l | grep reference || echo "No reference services running"

db-console: ## Connect to PostgreSQL console
	docker-compose -f ops/docker-compose.yml exec postgres psql -U reference_user -d reference_db

kafka-console: ## Start Kafka console consumer
	docker-compose -f ops/docker-compose.yml exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic reference-events --from-beginning

redis-cli: ## Connect to Redis CLI
	docker-compose -f ops/docker-compose.yml exec redis redis-cli

format: ## Format code with Spotless
	mvn spotless:apply

lint: ## Run linting checks
	mvn spotless:check

coverage: ## Generate test coverage report
	mvn clean test jacoco:report
	@echo "Coverage report generated at target/site/jacoco/index.html"

docs: ## Generate JavaDoc documentation
	mvn javadoc:aggregate
	@echo "Documentation generated at target/site/apidocs/index.html"

package: clean build ## Build deployable packages
	@echo "Packages built in target/ directories"

docker-build: ## Build Docker images
	docker build -t cbp-reference-api:latest -f reference-api/Dockerfile .
	@echo "Docker image built: cbp-reference-api:latest"

load-iso: ## Load ISO country data
	cd reference-loaders/iso && mvn spring-boot:run

load-genc: ## Load GENC data
	cd reference-loaders/genc && mvn spring-boot:run

load-all: load-iso load-genc ## Load all reference data

validate: lint test integration-test ## Run all validations

release: validate package docker-build ## Prepare release artifacts
	@echo "Release artifacts ready"

# Development Environment Setup
setup-dev: bootstrap up-dev migrate seed ## Complete development environment setup

setup-e2e: ## Set up end-to-end testing framework
	@echo "Setting up E2E testing framework..."
	@if [ ! -d "test/e2e" ]; then \
		mkdir -p test/e2e; \
		cd test/e2e && npm init -y; \
		npm install --save-dev playwright @playwright/test; \
		npx playwright install; \
		echo "E2E tests initialized. Add test files to test/e2e/"; \
	else \
		echo "E2E tests already set up"; \
	fi

# Enhanced Infrastructure Commands
up-dev: ## Start development infrastructure (without application containers)
	docker-compose -f ops/docker-compose.yml up -d postgres kafka zookeeper schema-registry keycloak redis opensearch opensearch-dashboards kafka-ui

up-full: ## Start full infrastructure including OpenMetadata
	docker-compose -f ops/docker-compose.yml up -d

up-minimal: ## Start minimal infrastructure (postgres, kafka, redis only)
	docker-compose -f ops/docker-compose.yml up -d postgres kafka zookeeper schema-registry redis

# Database Operations
db-reset: ## Reset database and run migrations
	@echo "Resetting database..."
	docker-compose -f ops/docker-compose.yml exec postgres psql -U reference_user -d reference_db -c "DROP SCHEMA IF EXISTS reference_data CASCADE; DROP SCHEMA IF EXISTS keycloak CASCADE;"
	$(MAKE) migrate

db-backup: ## Create database backup
	@echo "Creating database backup..."
	@mkdir -p backups
	docker-compose -f ops/docker-compose.yml exec postgres pg_dump -U reference_user reference_db > backups/reference_db_$(shell date +%Y%m%d_%H%M%S).sql
	@echo "Backup created in backups/ directory"

db-restore: ## Restore database from backup (requires BACKUP_FILE variable)
	@if [ -z "$(BACKUP_FILE)" ]; then \
		echo "Usage: make db-restore BACKUP_FILE=backups/filename.sql"; \
		exit 1; \
	fi
	docker-compose -f ops/docker-compose.yml exec -T postgres psql -U reference_user -d reference_db < $(BACKUP_FILE)

# Docker Operations
docker-clean: ## Clean up Docker containers, volumes, and images
	@echo "Cleaning up Docker resources..."
	docker-compose -f ops/docker-compose.yml down -v --remove-orphans
	docker system prune -f
	docker volume prune -f

docker-logs: ## Show logs for all containers
	docker-compose -f ops/docker-compose.yml logs -f

docker-ps: ## Show running containers
	docker-compose -f ops/docker-compose.yml ps

# Code Quality
quality: lint format test-coverage ## Run all code quality checks

security-scan: ## Run security vulnerability scan
	mvn org.owasp:dependency-check-maven:check
	@echo "Security report: target/dependency-check-report.html"

audit: ## Run dependency audit
	mvn versions:display-dependency-updates
	npm audit --prefix admin-ui

# Performance & Monitoring
benchmark: ## Run performance benchmarks
	@echo "Running performance benchmarks..."
	@if command -v k6 >/dev/null 2>&1; then \
		k6 run test/performance/load-test.js; \
	else \
		echo "k6 not installed. Install from https://k6.io/docs/getting-started/installation/"; \
	fi

health-check: ## Check health of all services
	@echo "Checking service health..."
	@curl -s http://localhost:8080/actuator/health | jq '.' || echo "API not responding"
	@curl -s http://localhost:9200/_cluster/health | jq '.' || echo "OpenSearch not responding"
	@curl -s http://localhost:8585/api/v1/system/status | jq '.' || echo "OpenMetadata not responding"

# Data Operations
load-sample-data: seed ## Load sample reference data
	@echo "Sample data loaded"

load-production-data: ## Load production reference data (requires approval)
	@echo "WARNING: This will load production data"
	@read -p "Are you sure? (y/N): " confirm && [ "$$confirm" = "y" ] || exit 1
	$(MAKE) load-all

# Cleanup
clean-all: clean docker-clean ## Clean everything (code artifacts and Docker)
	rm -rf backups/
	rm -rf logs/
	rm -rf test/e2e/node_modules/

# Utility
watch-logs: ## Watch application logs in real-time
	@if pgrep -f "spring-boot:run" > /dev/null; then \
		tail -f reference-api/target/spring.log; \
	else \
		echo "No Spring Boot applications running"; \
	fi

ports: ## Show all used ports
	@echo "Infrastructure ports:"
	@echo "  PostgreSQL:      5432"
	@echo "  Kafka:           9092"
	@echo "  Schema Registry: 8081"
	@echo "  Kafka UI:        8082"
	@echo "  Keycloak:        8085"
	@echo "  Redis:           6379"
	@echo "  OpenSearch:      9200"
	@echo "  OpenSearch UI:   5601"
	@echo "  OpenMetadata:    8585"
	@echo ""
	@echo "Application ports:"
	@echo "  Reference API:   8080"
	@echo "  Admin UI:        4200"

# Help enhancement
help-dev: ## Show development workflow help
	@echo "Common development workflows:"
	@echo ""
	@echo "  1. Initial setup:"
	@echo "     make setup-dev"
	@echo ""
	@echo "  2. Daily development:"
	@echo "     make up-dev && make run"
	@echo ""
	@echo "  3. Run tests:"
	@echo "     make test-all"
	@echo ""
	@echo "  4. Debug issues:"
	@echo "     make health-check && make watch-logs"
	@echo ""
	@echo "  5. Reset environment:"
	@echo "     make clean-all && make setup-dev"