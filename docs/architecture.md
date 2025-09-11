# CBP Reference Data Service - Architecture

## Executive Summary

The CBP Reference Data Service is a centralized, bitemporal data management system designed to provide canonical reference data with full historical tracking, governance workflows, and multi-channel distribution capabilities. Built on Spring Boot 3.3.5 with Java 21 for the backend and Angular 20.1 for the admin UI, it implements a microservices architecture with event-driven communication and comprehensive data lineage tracking.

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         External Consumers                       │
│  (Applications, Analytics, Data Warehouses, External Systems)    │
└───────────┬────────────────────────────────────┬────────────────┘
            │                                    │
    ┌───────▼────────┐                          │
    │   Admin UI     │                          │
    │  (Angular 20)  │                          │
    └───────┬────────┘                          │
            │                                    │
            ▼                                    ▼
    ┌───────────────┐                   ┌─────────────────┐
    │   REST API    │                   │  Event Stream   │
    │  (Spring Web) │                   │    (Kafka)      │
    └───────┬───────┘                   └────────┬────────┘
            │                                     │
            ▼                                     ▼
┌──────────────────────────────────────────────────────────────────┐
│                    Reference Data Platform                        │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │     API      │  │  Translation │  │   Workflow    │          │
│  │   Module     │  │   Service    │  │    Module     │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                  │                  │                   │
│  ┌──────▼──────────────────▼──────────────────▼──────┐          │
│  │              Core Domain Module                     │          │
│  │  (Entities, Repositories, Bitemporal Logic)        │          │
│  └──────────────────────┬──────────────────────────┘          │
│                         │                                        │
│  ┌──────────────────────▼──────────────────────────┐          │
│  │           PostgreSQL (Bitemporal Store)          │          │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐│          │
│  │  │ Countries  │  │  Languages │  │Organizations││          │
│  │  └────────────┘  └────────────┘  └────────────┘│          │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐│          │
│  │  │  Carriers  │  │   Mappings │  │   Outbox   ││          │
│  │  └────────────┘  └────────────┘  └────────────┘│          │
│  └──────────────────────────────────────────────────┘          │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Loaders    │  │   Catalog    │  │    Events     │          │
│  │  (ISO, GENC) │  │ Integration  │  │   Publisher   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└──────────────────────────────────────────────────────────────────┘
```

## Core Design Patterns

### 1. Bitemporal Data Model

Every entity extends the `Bitemporal` base class, providing two temporal dimensions:

- **Valid Time**: When the fact was true in the real world (`valid_from`, `valid_to`)
- **Transaction Time**: When the fact was recorded in the system (`recorded_at`)

```java
@MappedSuperclass
public abstract class Bitemporal {
    @Id
    private UUID id;
    private Long version;
    private LocalDate validFrom;
    private LocalDate validTo;
    private LocalDateTime recordedAt;
    private String recordedBy;
    private String changeRequestId;
    private Boolean isCorrection;
    private String metadata; // JSONB
}
```

**Benefits:**
- Complete audit trail
- Point-in-time reconstruction
- Regulatory compliance
- No data loss on updates

### 2. Outbox Pattern for Event Streaming

Guarantees exactly-once delivery of events through transactional outbox:

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Service   │────▶│   Outbox    │────▶│    Kafka    │
│   (Write)   │     │   (Table)    │     │   (Topic)   │
└─────────────┘     └─────────────┘     └─────────────┘
      │                    ▲                    │
      └────────────────────┘                    │
       Same Transaction                         │
                                                ▼
                                         ┌─────────────┐
                                         │  Consumers  │
                                         └─────────────┘
```

### 3. CQRS with Materialized Views

- **Write Model**: Normalized bitemporal tables
- **Read Model**: Current views, denormalized projections
- **Event Sourcing**: Full history via outbox events

### 4. Workflow-Driven Changes

All data modifications go through approval workflows:

```
Change Request → Validation → Policy Check → Review → Apply → Notify
                     │            │             │        │       │
                     ▼            ▼             ▼        ▼       ▼
                  Schema      OPA Rules    Human/Auto  Outbox  Events
```

## Module Architecture

### Core Module (`reference-core`)
- **Purpose**: Domain models and business logic
- **Key Components**:
  - JPA Entities (Country, Language, Organization, Carrier, CodeMapping)
  - Bitemporal base class and utilities
  - Repository interfaces with temporal queries
  - Domain services

### API Module (`reference-api`)
- **Purpose**: REST API endpoints
- **Key Features**:
  - RESTful controllers with OpenAPI documentation
  - ETags for caching
  - Pagination with HATEOAS links
  - Problem+JSON error responses
  - OAuth2/OIDC security

### Events Module (`reference-events`)
- **Purpose**: Event streaming infrastructure
- **Components**:
  - Outbox publisher with retry logic
  - Kafka producers
  - Avro schema definitions
  - Dead letter queue handling

### Translation Service (`translation-service`)
- **Purpose**: Code mapping and translation
- **Features**:
  - Multi-directional translations
  - Batch processing
  - Confidence scoring
  - Redis caching
  - Deprecation handling

### Workflow Module (`reference-workflow`)
- **Purpose**: Change management workflows
- **Components**:
  - Camunda BPMN processes
  - OPA policy integration
  - Task management
  - Audit logging

### Catalog Integration (`catalog-integration`)
- **Purpose**: Metadata-driven artifact generation
- **Features**:
  - OpenMetadata integration
  - SQL view generation for multiple engines
  - UDF generation
  - dbt model generation

### Loaders (`reference-loaders/*`)
- **Purpose**: Data ingestion from authoritative sources
- **Supported Sources**:
  - ISO 3166 countries
  - GENC geopolitical entities
  - ISO 639 languages
  - Organization codes
- **Features**:
  - Diff detection
  - Batch processing
  - Validation rules
  - Staging tables

### Admin UI (`admin-ui`)
- **Purpose**: Web-based administration interface
- **Technology**: Angular 20.1 with TypeScript 5.8
- **Features**:
  - Data browsing and search
  - Change request management
  - Workflow visualization
  - Real-time updates via WebSocket
- **UI Framework**: USWDS 3.13 (US Web Design System) for government compliance

## Data Flow Patterns

### 1. Reference Data Ingestion

```
External Source → Loader → Staging → Diff → Validation → Workflow → Core Tables → Events
```

### 2. Translation Request

```
Client Request → Translation Service → Cache Check → Rule Engine → Response
                                            │
                                            ▼
                                     Core Tables (if miss)
```

### 3. Temporal Query

```
Query with As-Of Date → Repository → Bitemporal Filter → Result Set → DTO Mapping → Response
```

## Technology Stack

### Core Technologies
- **Backend Language**: Java 21
- **Backend Framework**: Spring Boot 3.3.5
- **Frontend Framework**: Angular 20.1
- **Frontend Language**: TypeScript 5.8
- **Build Tools**: Maven 3.9+ (backend), Angular CLI 20.1 (frontend)
- **Database**: PostgreSQL 15+ with JSONB

### Spring Ecosystem
- Spring Data JPA
- Spring Web
- Spring Security
- Spring Batch
- Spring Kafka

### Frontend Stack
- Angular 20.1
- TypeScript 5.8.2
- RxJS 7.8
- USWDS 3.13 (US Web Design System)
- Zone.js 0.15

### Infrastructure
- **Container Runtime**: Docker
- **Orchestration**: Docker Compose (dev), Kubernetes (prod)
- **Message Broker**: Kafka/Redpanda
- **Cache**: Redis
- **Search**: OpenSearch/Elasticsearch
- **Identity**: Keycloak

### Data & Integration
- **Schema Registry**: Confluent/Apicurio
- **Workflow**: Camunda 7
- **Policy**: Open Policy Agent (OPA)
- **Catalog**: OpenMetadata
- **Monitoring**: Prometheus + Grafana
- **Tracing**: OpenTelemetry
- **Mapping**: MapStruct 1.6.3
- **Migration**: Liquibase 4.29.2
- **Event Streaming**: Kafka 3.8.0 with Avro 1.12.0

## Security Architecture

### Authentication & Authorization
- OAuth2/OIDC via Keycloak
- JWT tokens with refresh
- Role-based access control (RBAC)
- API key support for service accounts

### Data Security
- Encryption at rest (PostgreSQL TDE)
- Encryption in transit (TLS 1.3)
- Column-level encryption for PII
- Audit logging for all changes

### Network Security
- mTLS for service-to-service
- API Gateway with rate limiting
- WAF for public endpoints
- Network segmentation

## Deployment Architecture

### Development Environment
```yaml
docker-compose:
  - postgres
  - kafka
  - redis
  - keycloak
  - opensearch
```

### Production Environment
```
┌─────────────────────────────────────────────┐
│              Load Balancer                   │
└────────────────┬────────────────────────────┘
                 │
    ┌────────────▼────────────┐
    │      API Gateway        │
    │   (Kong/Istio/Zuul)     │
    └────────────┬────────────┘
                 │
    ┌────────────▼────────────────────────┐
    │       Kubernetes Cluster              │
    │                                       │
    │  ┌─────────────┐  ┌─────────────┐   │
    │  │  API Pods   │  │ Worker Pods  │   │
    │  │  (3+ replicas)│ │  (2+ replicas)│  │
    │  └─────────────┘  └─────────────┘   │
    │                                       │
    │  ┌─────────────────────────────┐    │
    │  │    StatefulSets              │    │
    │  │  - Kafka                     │    │
    │  │  - PostgreSQL (Primary)      │    │
    │  │  - Redis                     │    │
    │  └─────────────────────────────┘    │
    └───────────────────────────────────────┘
                 │
    ┌────────────▼────────────┐
    │   PostgreSQL Replica     │
    │    (Read-only standby)   │
    └─────────────────────────┘
```

## Performance & Scalability

### Caching Strategy
1. **Application Cache**: Spring Cache with Redis
2. **HTTP Cache**: ETags and Cache-Control headers
3. **Database Cache**: PostgreSQL buffer cache
4. **CDN**: Static content and API responses

### Scaling Patterns
- **Horizontal Scaling**: Stateless services behind load balancer
- **Read Replicas**: PostgreSQL streaming replication
- **Event Partitioning**: Kafka topic partitioning by entity type
- **Batch Processing**: Spring Batch with parallel steps

### Performance Targets
- API Response: < 100ms (p95)
- Bulk Operations: 10,000 records/second
- Event Publishing: < 10ms latency
- Translation Cache Hit: > 95%

## Monitoring & Observability

### Metrics (Prometheus)
- Business metrics (records created, translations performed)
- Technical metrics (response times, error rates)
- Infrastructure metrics (CPU, memory, disk)

### Logging (ELK Stack)
- Structured JSON logging
- Correlation IDs for request tracing
- Audit logs for compliance

### Tracing (OpenTelemetry)
- Distributed tracing across services
- Database query performance
- External API call monitoring

### Alerting
- PagerDuty integration for critical alerts
- Slack notifications for warnings
- Email digests for reports

## Disaster Recovery

### Backup Strategy
- **Database**: Daily full backups, hourly incrementals
- **Events**: Kafka log retention (7 days)
- **Configuration**: Git-backed ConfigMaps

### Recovery Targets
- **RPO**: < 1 hour
- **RTO**: < 4 hours
- **Availability**: 99.9% uptime

### Failover Procedures
1. Database failover to standby
2. Service migration to secondary region
3. DNS update for API endpoints
4. Event replay from backup

## Development Workflow

### Local Development
```bash
make bootstrap  # Install dependencies
make up        # Start infrastructure
make migrate   # Run migrations
make seed      # Load sample data
make run       # Start backend services
make ui        # Start Angular admin UI (ng serve)
make test      # Run tests
```

### CI/CD Pipeline
```
Git Push → GitHub Actions → Build → Test → Security Scan → Deploy to Dev → Integration Tests → Deploy to Staging → Smoke Tests → Deploy to Prod
```

### Release Process
1. Feature branch development
2. Pull request with reviews
3. Automated testing
4. Staging deployment
5. UAT sign-off
6. Production release
7. Post-deployment verification

## Future Enhancements

### Short Term (Q1-Q2)
- GraphQL API layer
- Real-time WebSocket subscriptions
- Advanced search with Elasticsearch
- Multi-tenancy support

### Medium Term (Q3-Q4)
- Machine learning for mapping suggestions
- Blockchain for audit trail
- Graph database for complex relationships
- Mobile SDK

### Long Term (Next Year)
- Global multi-region deployment
- AI-powered data quality checks
- Predictive analytics
- Self-service data onboarding

## Conclusion

The CBP Reference Data Service provides a robust, scalable, and maintainable solution for managing canonical reference data. Its bitemporal architecture ensures complete historical tracking, while the event-driven design enables real-time data distribution. The modular structure allows for independent scaling and deployment of components, and the comprehensive security and governance features ensure compliance with regulatory requirements.