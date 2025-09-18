# Change Request Workflow Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [User Guide](#user-guide)
4. [API Reference](#api-reference)
5. [Administrator Guide](#administrator-guide)
6. [Developer Guide](#developer-guide)

## Overview

The Change Request Workflow system provides a comprehensive approval process for all reference data modifications in the CBP Reference Data Service. This ensures data quality, provides audit trails, and enables controlled changes to critical reference data.

### Key Features
- **Approval Workflow**: All changes require approval before being applied
- **Bulk Operations**: Support for mass imports with validation
- **Audit Trail**: Complete history of all changes and approvals
- **Event-Driven**: Real-time notifications of state changes
- **Rollback Capability**: Ability to revert failed changes
- **Bitemporal History**: Track both business and system time

### System Components
- **Backend Services**: Spring Boot microservices with Java 21
- **Frontend Application**: Angular 20.1 with USWDS 3.13
- **Database**: PostgreSQL with bitemporal tables
- **Events**: Kafka-based event streaming
- **Security**: OAuth2/OIDC via Okta

## Architecture

### High-Level Architecture
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Angular UI    │────▶│  Spring Boot    │────▶│   PostgreSQL    │
│    (Frontend)   │     │     (API)       │     │   (Database)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │                           │
                               ▼                           ▼
                        ┌─────────────────┐     ┌─────────────────┐
                        │     Kafka       │     │  Audit Tables   │
                        │    (Events)     │     │   (History)     │
                        └─────────────────┘     └─────────────────┘
```

### Database Schema
The system uses bitemporal tables to track changes over time:

```sql
-- Change Request Table
CREATE TABLE change_requests (
    id UUID PRIMARY KEY,
    cr_number VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    current_values JSONB,
    proposed_changes JSONB,
    requester_id VARCHAR(100),
    approved_by VARCHAR(100),
    rejected_by VARCHAR(100),
    implemented_by VARCHAR(100),
    submitted_at TIMESTAMP,
    approved_at TIMESTAMP,
    rejected_at TIMESTAMP,
    implemented_at TIMESTAMP,
    business_justification TEXT,
    approval_comments TEXT,
    rejection_reason TEXT,
    metadata JSONB
);

-- Bulk Import Tables
CREATE TABLE bulk_import_batches (
    id UUID PRIMARY KEY,
    batch_number VARCHAR(50) UNIQUE,
    file_name VARCHAR(255),
    file_type VARCHAR(10),
    file_checksum VARCHAR(64),
    data_type VARCHAR(50),
    total_records INTEGER,
    valid_records INTEGER,
    invalid_records INTEGER,
    processing_status VARCHAR(20),
    created_by VARCHAR(100),
    created_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE TABLE bulk_import_staging (
    id UUID PRIMARY KEY,
    batch_id UUID REFERENCES bulk_import_batches(id),
    change_request_id UUID REFERENCES change_requests(id),
    row_number INTEGER,
    operation_type VARCHAR(20),
    source_data JSONB,
    validated_data JSONB,
    validation_status VARCHAR(20),
    validation_errors JSONB,
    processing_status VARCHAR(20),
    processing_errors TEXT
);
```

## User Guide

### Creating a Change Request

#### Single Record Changes

1. **Navigate to Countries Page**
   - Go to Reference Data → Countries
   - Find the country you want to modify

2. **Initiate Change**
   - Click "Edit" for updates or "Add" for new countries
   - Make your changes in the form
   - Click "Request Country Update" or "Request Country Creation"

3. **Provide Business Justification**
   - Enter detailed business justification (minimum 10 characters)
   - Select urgency level if applicable
   - Click "Submit Request"

4. **Track Your Request**
   - Note the Change Request ID (e.g., CR-2025-123456)
   - Navigate to Change Requests to track status

#### Bulk Import

1. **Access Bulk Import Wizard**
   - Go to Import/Export → Bulk Import Wizard

2. **Upload File**
   - Drag and drop CSV or JSON file
   - Select import mode (Create Only, Update Only, or Upsert)
   - Click "Next"

3. **Review Validation**
   - Review validation results
   - Fix any errors in your source file if needed
   - Download validation report for reference

4. **Preview Changes**
   - Review the data that will be imported
   - Check for any warnings
   - Click "Next" to proceed

5. **Submit for Approval**
   - Enter business justification
   - Select urgency level
   - Click "Submit for Approval"

### Approving Change Requests

1. **Access Approval Dashboard**
   - Navigate to Change Requests → Pending Approvals
   - Use filters to find specific requests

2. **Review Changes**
   - Click on a change request to view details
   - Review the diff showing current vs proposed values
   - Check the business justification

3. **Take Action**
   - **Approve**: Click "Approve" and optionally add comments
   - **Reject**: Click "Reject" and provide rejection reason
   - **Request More Info**: Use comments to ask for clarification

4. **Bulk Operations**
   - Select multiple requests using checkboxes
   - Click "Bulk Approve" or "Bulk Reject"
   - Provide comments for all selected items

## API Reference

### Change Request Endpoints

#### Create Change Request
```http
POST /v1/countries
Content-Type: application/json

{
  "countryData": {
    "countryName": "New Country",
    "iso2Code": "NC",
    "iso3Code": "NCY"
  },
  "reason": "Adding new country per UN resolution"
}

Response: 202 Accepted
{
  "id": "uuid",
  "crNumber": "CR-2025-123456",
  "status": "PENDING"
}
```

#### Get Change Request
```http
GET /v1/countries/change-requests/{id}

Response: 200 OK
{
  "id": "uuid",
  "crNumber": "CR-2025-123456",
  "status": "PENDING",
  "operationType": "CREATE",
  "title": "Create new country: New Country",
  "submittedBy": "user123",
  "currentValues": null,
  "proposedChanges": {...}
}
```

#### Approve Change Request
```http
POST /v1/countries/change-requests/{id}/approve
Content-Type: application/json

{
  "userId": "approver123",
  "comments": "Verified against UN documentation"
}

Response: 200 OK
```

#### Reject Change Request
```http
POST /v1/countries/change-requests/{id}/reject
Content-Type: application/json

{
  "userId": "approver123",
  "reason": "ISO codes do not match standard"
}

Response: 200 OK
```

#### Apply Change Request
```http
POST /v1/countries/change-requests/{id}/apply

Response: 200 OK
```

### Bulk Import Endpoints

#### Initiate Bulk Import
```http
POST /api/v1/bulk-import/initiate
Content-Type: multipart/form-data

Parameters:
- file: CSV or JSON file
- userId: User initiating import
- dataType: COUNTRY, PORT, or AIRPORT
- sourceSystem: Source system name
- description: Optional description

Response: 201 Created
{
  "batchId": "uuid",
  "message": "Bulk import initiated successfully",
  "success": true
}
```

#### Validate Bulk Import
```http
POST /api/v1/bulk-import/validate/{batchId}

Response: 200 OK
{
  "batchId": "uuid",
  "validCount": 95,
  "invalidCount": 5,
  "warningCount": 2
}
```

#### Process Bulk Import
```http
POST /api/v1/bulk-import/process/{batchId}

Response: 200 OK
{
  "batchId": "uuid",
  "status": "COMPLETED",
  "processedRecords": 95
}
```

### Event Publishing

The system publishes events for all state transitions:

```json
{
  "eventId": "uuid",
  "eventType": "CHANGE_REQUEST_CREATED",
  "changeRequestId": "uuid",
  "userId": "user123",
  "timestamp": "2025-01-15T10:30:00Z",
  "metadata": {
    "entityType": "COUNTRY",
    "operationType": "CREATE"
  }
}
```

Event Types:
- `CHANGE_REQUEST_CREATED`
- `CHANGE_REQUEST_APPROVED`
- `CHANGE_REQUEST_REJECTED`
- `CHANGE_REQUEST_APPLIED`
- `CHANGE_REQUEST_CANCELLED`
- `BULK_IMPORT_INITIATED`
- `BULK_IMPORT_COMPLETED`

## Administrator Guide

### Configuration

#### Application Properties
```yaml
# Change Request Configuration
change-request:
  auto-apply: false
  require-two-approvals: false
  approval-timeout-hours: 72

# Bulk Import Configuration
bulk-import:
  max-file-size-mb: 50
  max-records: 10000
  validation-batch-size: 100

# Event Publishing
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

### Monitoring

#### Key Metrics
- Change requests per hour/day
- Average approval time
- Rejection rate
- Bulk import success rate
- Event publishing lag

#### Health Checks
```http
GET /actuator/health

{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "kafka": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### Troubleshooting

#### Common Issues

1. **Change Request Stuck in PENDING**
   - Check if approvers have necessary permissions
   - Verify no validation errors in logs
   - Check event publishing status

2. **Bulk Import Fails Validation**
   - Download validation report for details
   - Check file format matches expected schema
   - Verify no duplicate records

3. **Events Not Publishing**
   - Check Kafka connectivity
   - Verify outbox table for stuck events
   - Review error logs for serialization issues

## Developer Guide

### Local Development Setup

```bash
# Start dependencies
docker-compose up -d postgres kafka redis

# Run backend
./mvnw spring-boot:run

# Run frontend
cd frontend
npm install
npm start
```

### Running Tests

```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify -Pintegration-tests

# Frontend tests
cd frontend
npm test

# E2E tests
npm run e2e
```

### Adding New Entity Types

1. **Create Entity and Repository**
```java
@Entity
@Table(name = "new_entity_v")
public class NewEntity implements Bitemporal {
    // Implementation
}

@Repository
public interface NewEntityRepository extends BitemporalRepository<NewEntity> {
    // Custom queries
}
```

2. **Create Service**
```java
@Service
public class NewEntityChangeRequestService {
    public ChangeRequest createChangeRequest(NewEntityDto dto, String operation, String userId, String reason) {
        // Implementation
    }
}
```

3. **Update Controller**
```java
@PostMapping("/new-entities")
public ResponseEntity<ChangeRequestDto> createNewEntity(@RequestBody ChangeRequestCreateDto dto) {
    // Implementation
}
```

4. **Add Validation Rules**
```java
public class NewEntityValidator {
    public ValidationResult validate(NewEntityDto dto) {
        // Validation logic
    }
}
```

### Extension Points

The system provides several extension points for customization:

1. **Custom Validators**: Implement `DataValidator` interface
2. **Event Handlers**: Subscribe to change request events
3. **Approval Rules**: Implement `ApprovalRule` interface
4. **Export Formats**: Add new export format handlers

### Performance Considerations

- Use pagination for large result sets
- Implement caching for frequently accessed data
- Use batch operations for bulk updates
- Monitor database query performance
- Optimize JSON serialization for large payloads

### Security Best Practices

- Always validate user permissions before operations
- Sanitize all user input
- Use parameterized queries
- Implement rate limiting
- Audit all data modifications
- Encrypt sensitive data in transit and at rest

## Appendix

### Glossary

- **Change Request (CR)**: A formal request to modify reference data
- **Bitemporal**: Tracking both business time and system time
- **Bulk Import**: Mass data upload with validation
- **Outbox Pattern**: Reliable event publishing mechanism
- **USWDS**: US Web Design System for government applications

### References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Angular Documentation](https://angular.io/docs)
- [USWDS Guidelines](https://designsystem.digital.gov/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

### Support

For issues or questions:
- GitHub Issues: https://github.com/balemneh/reference-data/issues
- Email: support@cbp.dhs.gov
- Internal Wiki: http://wiki.cbp.dhs.gov/reference-data