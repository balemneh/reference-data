# Consumer Guide: Reference Data Service

## 1. Introduction

This guide is for **Consumers** of the Reference Data Service. As a consumer, you have read-only access to the rich repository of reference data managed by this service. This document will help you understand how to query the data via the REST API and how to subscribe to real-time data streams.

## 2. Core Concepts

- **Read-Only Access**: As a consumer, you can view data but not modify it. All data creation and updates are handled by Data Stewards through a governed workflow.
- **REST API**: The primary way to access the data is through a secure, stable, and well-documented REST API.
- **Event Streaming**: For real-time data needs, you can subscribe to Kafka topics to receive notifications as soon as data changes.

## 3. Accessing Data via the REST API

All API endpoints require authentication. You must include an `Authorization` header with a valid Bearer token. The base URL for the API is `https://api.reference.cbp.dhs.gov/v1`.

### 3.1. Fetching a Specific Record

If you know the unique ID of a record, you can fetch it directly.

**Example**: Get a country by its ID.
```http
GET /v1/countries/123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer {your_access_token}
```

**Response (`200 OK`):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "countryName": "United States",
  "iso2Code": "US",
  "iso3Code": "USA",
  "validFrom": "2020-01-01",
  "validTo": null
}
```

### 3.2. Searching and Filtering Data

You can search for records based on specific criteria. Most search endpoints support pagination.

**Example**: Search for countries with "United" in the name.
```http
GET /v1/countries/search?name=United
Authorization: Bearer {your_access_token}
```

**Response (`200 OK`):**
```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "countryName": "United States"
    },
    {
      "id": "some-other-uuid",
      "countryName": "United Kingdom"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "pageNumber": 0,
  "pageSize": 20
}
```

### 3.3. API Features

- **Pagination**: All list endpoints support `page`, `size`, and `sort` query parameters to help you navigate large datasets.
- **Rate Limiting**: Be aware of rate limits, which are communicated in the `X-RateLimit-*` response headers. Exceeding the limit will result in a `429 Too Many Requests` error.
- **Error Handling**: The API uses standard HTTP status codes and provides detailed error information in the RFC 7807 Problem Details format.

*For a full list of available endpoints and their parameters, please refer to the complete [API Documentation](API_DOCUMENTATION.md) or the interactive Swagger UI at `/swagger-ui`.*

## 4. Subscribing to Real-Time Data Events (Advanced)

For applications that require immediate updates, you can consume events directly from Kafka. This allows you to build reactive systems that respond to data changes as they happen.

### 4.1. Kafka Topics

The service publishes events to the following Kafka topics:

- `reference-data.change-requests`: Events related to the change request lifecycle (created, approved, rejected).
- `reference-data.bulk-imports`: Events for bulk import status changes.
- `reference-data.countries`: Events for changes to country data.
- `reference-data.audit`: A comprehensive audit trail of all activities.

### 4.2. Event Structure

All events share a common structure. The `eventType` tells you what happened, and the `payload` contains the relevant data.

**Example Event (`CHANGE_REQUEST_APPLIED`):**
```json
{
  "eventId": "some-event-uuid",
  "eventType": "CHANGE_REQUEST_APPLIED",
  "aggregateId": "cr-uuid",
  "aggregateType": "CHANGE_REQUEST",
  "payload": {
    "changeRequestId": "cr-uuid",
    "entityId": "entity-uuid",
    "entityType": "COUNTRY"
  },
  "metadata": {
    "correlationId": "correlation-uuid",
    "timestamp": "2025-01-15T11:30:00Z",
    "version": "1.0.0"
  }
}
```
When you receive this event, you can then make a `GET` request to `/v1/countries/{entityId}` to get the newly updated data.

### 4.3. Setting up a Kafka Consumer

To consume these events, you will need to:
1.  Get access to the Kafka broker.
2.  Configure your Kafka client (consumer) to connect to the broker and subscribe to the desired topic(s).
3.  Implement the logic to process the incoming event messages.

---
*For questions or support regarding API access or event consumption, please contact the support team.*
