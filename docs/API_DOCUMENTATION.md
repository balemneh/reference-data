# CBP Reference Data Service - API Documentation

## Version: 1.0.0
**Base URL**: `https://api.reference.cbp.dhs.gov/v1`

## Authentication
All API endpoints require OAuth2 authentication via Okta.

```http
Authorization: Bearer {access_token}
```

## Table of Contents
1. [Countries API](#countries-api)
2. [Change Requests API](#change-requests-api)
3. [Bulk Import API](#bulk-import-api)
4. [Events API](#events-api)
5. [Error Handling](#error-handling)

---

## Countries API

### Get Country by ID
Retrieves a specific country by its unique identifier.

```http
GET /v1/countries/{id}
```

**Parameters:**
- `id` (path, required): UUID of the country

**Response:** `200 OK`
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "countryName": "United States",
  "iso2Code": "US",
  "iso3Code": "USA",
  "numericCode": "840",
  "activeFlag": true,
  "validFrom": "2020-01-01",
  "validTo": null
}
```

### Search Countries
Search for countries by name with pagination.

```http
GET /v1/countries/search?name={name}&page={page}&size={size}
```

**Parameters:**
- `name` (query, required): Country name to search
- `page` (query, optional): Page number (default: 0)
- `size` (query, optional): Page size (default: 20)

**Response:** `200 OK`
```json
{
  "content": [...],
  "totalElements": 195,
  "totalPages": 10,
  "pageNumber": 0,
  "pageSize": 20
}
```

### Get Countries by System Code
Retrieves countries for a specific code system.

```http
GET /v1/countries?systemCode={systemCode}&page={page}&size={size}
```

**Parameters:**
- `systemCode` (query, required): Code system (e.g., ISO3166-1, CBP-COUNTRY5)
- `page` (query, optional): Page number (default: 0)
- `size` (query, optional): Page size (default: 20)

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "uuid",
      "countryName": "United States",
      "countryCode": "US",
      "codeSystem": "ISO3166-1"
    }
  ],
  "totalElements": 195,
  "totalPages": 10
}
```

### Create Country (Change Request)
Creates a change request to add a new country.

```http
POST /v1/countries
Content-Type: application/json
```

**Request Body:**
```json
{
  "countryData": {
    "countryName": "New Country",
    "iso2Code": "NC",
    "iso3Code": "NCY",
    "numericCode": "999",
    "activeFlag": true
  },
  "reason": "Adding new country per UN resolution 12345"
}
```

**Response:** `202 Accepted`
```json
{
  "id": "uuid",
  "crNumber": "CR-2025-123456",
  "status": "PENDING",
  "operationType": "CREATE",
  "title": "Create new country: New Country"
}
```

### Update Country (Change Request)
Creates a change request to update an existing country.

```http
PUT /v1/countries/{id}
Content-Type: application/json
```

**Parameters:**
- `id` (path, required): UUID of the country to update

**Request Body:**
```json
{
  "countryData": {
    "id": "uuid",
    "countryName": "Updated Country Name",
    "iso2Code": "UC",
    "iso3Code": "UPD"
  },
  "reason": "Updating country name per official decree"
}
```

**Response:** `202 Accepted`

### Delete Country (Change Request)
Creates a change request to deactivate a country.

```http
DELETE /v1/countries/{id}?reason={reason}
```

**Parameters:**
- `id` (path, required): UUID of the country to delete
- `reason` (query, required): Business justification for deletion

**Response:** `202 Accepted`

---

## Change Requests API

### Get Pending Change Requests
Retrieves all pending change requests with pagination.

```http
GET /v1/countries/change-requests?page={page}&size={size}
```

**Parameters:**
- `page` (query, optional): Page number (default: 0)
- `size` (query, optional): Page size (default: 20)

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "uuid",
      "crNumber": "CR-2025-123456",
      "status": "PENDING",
      "operationType": "UPDATE",
      "title": "Update country: United States",
      "submittedBy": "user123",
      "submittedAt": "2025-01-15T10:30:00Z"
    }
  ],
  "totalElements": 15,
  "totalPages": 1
}
```

### Get Change Request by ID
Retrieves a specific change request by ID.

```http
GET /v1/countries/change-requests/{id}
```

**Parameters:**
- `id` (path, required): UUID of the change request

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "crNumber": "CR-2025-123456",
  "status": "PENDING",
  "operationType": "CREATE",
  "entityType": "COUNTRY",
  "entityId": null,
  "title": "Create new country: Test Country",
  "description": "Adding test country for demo",
  "currentValues": null,
  "proposedChanges": {
    "countryName": "Test Country",
    "iso2Code": "TC"
  },
  "submittedBy": "user123",
  "submittedAt": "2025-01-15T10:30:00Z",
  "businessJustification": "Required for new trade agreement"
}
```

### Get Change Requests by Status
Retrieves change requests filtered by status.

```http
GET /v1/countries/change-requests/by-status?status={status}&page={page}&size={size}
```

**Parameters:**
- `status` (query, required): Status filter (PENDING, APPROVED, REJECTED, APPLIED)
- `page` (query, optional): Page number
- `size` (query, optional): Page size

**Response:** `200 OK`

### Get Change Request History for Entity
Retrieves all change requests for a specific entity.

```http
GET /v1/countries/{id}/change-requests?page={page}&size={size}
```

**Parameters:**
- `id` (path, required): UUID of the country
- `page` (query, optional): Page number
- `size` (query, optional): Page size

**Response:** `200 OK`

### Approve Change Request
Approves a pending change request.

```http
POST /v1/countries/change-requests/{id}/approve
Content-Type: application/json
```

**Parameters:**
- `id` (path, required): UUID of the change request

**Request Body:**
```json
{
  "userId": "approver123",
  "comments": "Verified against official documentation"
}
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "crNumber": "CR-2025-123456",
  "status": "APPROVED",
  "approvedBy": "approver123",
  "approvedAt": "2025-01-15T11:00:00Z"
}
```

### Reject Change Request
Rejects a pending change request.

```http
POST /v1/countries/change-requests/{id}/reject
Content-Type: application/json
```

**Parameters:**
- `id` (path, required): UUID of the change request

**Request Body:**
```json
{
  "userId": "approver123",
  "reason": "ISO codes do not match international standard"
}
```

**Response:** `200 OK`

### Apply Change Request
Applies an approved change request to the database.

```http
POST /v1/countries/change-requests/{id}/apply
```

**Parameters:**
- `id` (path, required): UUID of the change request

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "crNumber": "CR-2025-123456",
  "status": "APPLIED",
  "implementedAt": "2025-01-15T11:30:00Z"
}
```

---

## Bulk Import API

### Initiate Bulk Import
Uploads a file and initiates the bulk import process.

```http
POST /api/v1/bulk-import/initiate
Content-Type: multipart/form-data
```

**Form Parameters:**
- `file` (file, required): CSV or JSON file to import
- `userId` (string, required): User initiating the import
- `dataType` (string, required): Type of data (COUNTRY, PORT, AIRPORT)
- `sourceSystem` (string, required): Source system name
- `description` (string, optional): Description of the import

**Response:** `201 Created`
```json
{
  "batchId": "uuid",
  "message": "Bulk import initiated successfully",
  "success": true
}
```

### Validate Bulk Import
Validates all staged records in a batch.

```http
POST /api/v1/bulk-import/validate/{batchId}
```

**Parameters:**
- `batchId` (path, required): UUID of the batch to validate

**Response:** `200 OK`
```json
{
  "batchId": "uuid",
  "validCount": 95,
  "invalidCount": 5,
  "warningCount": 2,
  "message": "Validation completed successfully",
  "success": true
}
```

### Process Bulk Import
Processes validated records and creates change requests.

```http
POST /api/v1/bulk-import/process/{batchId}
```

**Parameters:**
- `batchId` (path, required): UUID of the batch to process

**Response:** `200 OK`
```json
{
  "batchId": "uuid",
  "status": "COMPLETED",
  "processedRecords": 95,
  "changeRequestsCreated": 95
}
```

### Get Bulk Import Status
Retrieves the current status of a bulk import batch.

```http
GET /api/v1/bulk-import/status/{batchId}
```

**Parameters:**
- `batchId` (path, required): UUID of the batch

**Response:** `200 OK`
```json
{
  "batchId": "uuid",
  "status": "PROCESSING",
  "totalRecords": 100,
  "processedRecords": 45,
  "validRecords": 40,
  "invalidRecords": 5,
  "progress": 45.0
}
```

### Rollback Bulk Import
Rolls back a bulk import batch.

```http
POST /api/v1/bulk-import/rollback/{batchId}
```

**Parameters:**
- `batchId` (path, required): UUID of the batch to rollback

**Response:** `200 OK`
```json
{
  "batchId": "uuid",
  "status": "ROLLED_BACK",
  "message": "Bulk import rolled back successfully"
}
```

---

## Events API

### Event Structure
All events follow this structure:

```json
{
  "eventId": "uuid",
  "eventType": "CHANGE_REQUEST_CREATED",
  "aggregateId": "uuid",
  "aggregateType": "CHANGE_REQUEST",
  "payload": {
    "changeRequestId": "uuid",
    "operationType": "CREATE",
    "entityType": "COUNTRY",
    "userId": "user123"
  },
  "metadata": {
    "correlationId": "uuid",
    "causationId": "uuid",
    "timestamp": "2025-01-15T10:30:00Z",
    "version": "1.0.0"
  }
}
```

### Event Types

| Event Type | Description | Payload |
|------------|-------------|---------|
| `CHANGE_REQUEST_CREATED` | New change request created | changeRequestId, operationType, entityType |
| `CHANGE_REQUEST_APPROVED` | Change request approved | changeRequestId, approvedBy, comments |
| `CHANGE_REQUEST_REJECTED` | Change request rejected | changeRequestId, rejectedBy, reason |
| `CHANGE_REQUEST_APPLIED` | Changes applied to database | changeRequestId, entityId |
| `CHANGE_REQUEST_CANCELLED` | Change request cancelled | changeRequestId, cancelledBy, reason |
| `BULK_IMPORT_INITIATED` | Bulk import started | batchId, fileName, recordCount |
| `BULK_IMPORT_VALIDATED` | Validation completed | batchId, validCount, invalidCount |
| `BULK_IMPORT_COMPLETED` | Bulk import finished | batchId, processedCount, status |
| `BULK_IMPORT_FAILED` | Bulk import failed | batchId, error, failedAt |

### Subscribing to Events
Events are published to Kafka topics:

```yaml
Topics:
- reference-data.change-requests
- reference-data.bulk-imports
- reference-data.countries
- reference-data.audit
```

---

## Error Handling

### Error Response Format
All errors follow RFC 7807 Problem Details format:

```json
{
  "type": "/errors/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "detail": "The country code 'XYZ' is not a valid ISO 3166-1 alpha-3 code",
  "instance": "/v1/countries",
  "traceId": "uuid",
  "timestamp": "2025-01-15T10:30:00Z",
  "violations": [
    {
      "field": "iso3Code",
      "message": "must match pattern ^[A-Z]{3}$"
    }
  ]
}
```

### HTTP Status Codes

| Status Code | Meaning | Usage |
|-------------|---------|--------|
| `200 OK` | Success | Successful GET, PUT |
| `201 Created` | Created | Successful POST creating new resource |
| `202 Accepted` | Accepted | Change request created, processing async |
| `204 No Content` | No Content | Successful DELETE |
| `400 Bad Request` | Invalid Request | Validation errors, malformed request |
| `401 Unauthorized` | Authentication Required | Missing or invalid token |
| `403 Forbidden` | Access Denied | Insufficient permissions |
| `404 Not Found` | Resource Not Found | Entity doesn't exist |
| `409 Conflict` | Conflict | Duplicate resource, state conflict |
| `422 Unprocessable Entity` | Business Rule Violation | Valid request but business rules prevent processing |
| `429 Too Many Requests` | Rate Limited | Too many requests from client |
| `500 Internal Server Error` | Server Error | Unexpected server error |
| `503 Service Unavailable` | Service Down | Service temporarily unavailable |

### Common Error Types

#### Validation Errors
```json
{
  "type": "/errors/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "violations": [...]
}
```

#### Business Rule Violations
```json
{
  "type": "/errors/business-rule-violation",
  "title": "Business Rule Violation",
  "status": 422,
  "detail": "Cannot approve your own change request"
}
```

#### Resource Not Found
```json
{
  "type": "/errors/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Country with ID 'uuid' not found"
}
```

#### Conflict Errors
```json
{
  "type": "/errors/conflict",
  "title": "Resource Conflict",
  "status": 409,
  "detail": "Country with ISO code 'USA' already exists"
}
```

---

## Rate Limiting

API requests are rate limited per client:

- **Default**: 1000 requests per hour
- **Bulk Operations**: 100 requests per hour
- **File Uploads**: 10 requests per hour

Rate limit information is included in response headers:

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1642248000
```

---

## Pagination

All list endpoints support pagination:

**Request Parameters:**
- `page`: Page number (0-based, default: 0)
- `size`: Page size (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., `name,asc`)

**Response Structure:**
```json
{
  "content": [...],
  "totalElements": 1000,
  "totalPages": 50,
  "pageNumber": 0,
  "pageSize": 20,
  "first": true,
  "last": false
}
```

---

## Versioning

The API uses URL versioning:
- Current version: `v1`
- Base URL: `/v1/{resource}`

Version information is also included in response headers:
```http
X-API-Version: 1.0.0
```

---

## OpenAPI Specification

The complete OpenAPI 3.0 specification is available at:
```http
GET /v1/openapi.json
GET /v1/openapi.yaml
```

Interactive API documentation (Swagger UI):
```http
GET /swagger-ui
```

---

## Support

For API support and questions:
- GitHub Issues: https://github.com/balemneh/reference-data/issues
- API Status: https://status.reference.cbp.dhs.gov
- Email: api-support@cbp.dhs.gov