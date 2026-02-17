# Administrator Guide: Reference Data Service

## 1. Introduction

This guide is for **Administrators** of the Reference Data Service. As an administrator, you have full control over the system, including configuration, monitoring, and user management. This document provides the information you need to effectively manage the application.

## 2. Core Responsibilities

- **System Configuration**: Manage application settings and behavior.
- **Monitoring**: Track the health, performance, and usage of the service.
- **Troubleshooting**: Diagnose and resolve technical issues.
- **User and Access Management**: Manage user roles and permissions within Keycloak.

## 3. System Configuration

The application's behavior is primarily configured through the `application.yml` file. Below are key configuration sections relevant to administrators.

### 3.1. Change Request Workflow

You can control the change request process with the following settings:

```yaml
# in application.yml
change-request:
  # Automatically apply approved change requests. Set to false to require manual application.
  auto-apply: false
  # Require two separate approvals for a change request to be approved.
  require-two-approvals: false
  # Number of hours before a pending approval request times out.
  approval-timeout-hours: 72
```

### 3.2. Bulk Import

Configure the limits and behavior of the bulk import feature:

```yaml
# in application.yml
bulk-import:
  # Maximum file size in megabytes for bulk import files.
  max-file-size-mb: 50
  # Maximum number of records allowed in a single bulk import file.
  max-records: 10000
  # Number of records to process in each validation batch.
  validation-batch-size: 100
```

### 3.3. Event Publishing (Kafka)

Configure the connection to the Kafka message broker for event streaming:

```yaml
# in application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

## 4. Monitoring

The application exposes several actuator endpoints for monitoring. These are essential for understanding the state of the service.

### 4.1. Health Checks

Check the health of the application and its connections to downstream services (like the database and Kafka).

**Endpoint**: `GET /actuator/health`

**Example Response:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "kafka": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### 4.2. Key Metrics

Monitor key performance and usage metrics via the `/actuator/metrics` endpoint.

**Endpoint**: `GET /actuator/metrics`

To view a specific metric, use `GET /actuator/metrics/{metricName}`.

**Important Metrics to Watch:**

- `jvm.memory.used`: JVM memory usage.
- `http.server.requests`: Request latency and volume.
- `spring.kafka.producer.record.send.total`: Total number of records sent to Kafka.
- `change.requests.created`: Custom metric for created change requests.
- `change.requests.approved`: Custom metric for approved change requests.
- `bulk.import.completed`: Custom metric for completed bulk imports.

## 5. Troubleshooting

This section covers common issues and how to resolve them.

### 5.1. Change Request Stuck in "PENDING"

1.  **Check Approver Permissions**: Ensure the designated approvers have the correct roles (`data_curator` or `admin`) assigned in Keycloak.
2.  **Review Logs for Errors**: Check the application logs for any validation or processing errors related to the change request.
3.  **Check OPA Policy**: Verify that the `change_request_approval.rego` policy in OPA allows the change.
4.  **Inspect Event Stream**: Check the `reference-data.change-requests` Kafka topic to ensure events are being published and consumed correctly.

### 5.2. Bulk Import Fails Validation

1.  **Download Validation Report**: The UI provides a validation report for failed bulk imports. Instruct the user to download it to see row-by-row error details.
2.  **Check File Format**: Ensure the uploaded file is a valid CSV or JSON and matches the expected schema for the data type.
3.  **Check for Duplicates**: The system will reject records with duplicate business keys.
4.  **Review `data_validation.rego`**: The OPA policy for data validation contains the specific rules being applied.

### 5.3. Events Not Publishing to Kafka

1.  **Check Kafka Connectivity**: Use the `/actuator/health` endpoint to verify the connection to Kafka.
2.  **Inspect the Outbox Table**: The application uses the outbox pattern. Check the `outbox` table in the database for any unprocessed events.
3.  **Review Application Logs**: Look for any serialization errors or connection issues with the Kafka broker.

## 6. User and Access Management

User roles and permissions are managed in **Keycloak**.

### 6.1. Available Roles

-   **`admin`**: (Administrator) Full system access.
-   **`data_curator`**: (Data Steward) Can create, update, and approve change requests.
-   **`data_editor`**: (Data Steward with limited access) Can only propose updates.
-   **`consumer`**: (Consumer) Read-only access to data.

### 6.2. Assigning Roles

1.  Log in to the Keycloak Administration Console.
2.  Navigate to the appropriate realm (`reference-data`).
3.  Go to the **Users** section and select the user.
4.  Go to the **Role Mappings** tab and assign the desired role from the "Realm Roles" section.

---
*For API details, see the [API Documentation](API_DOCUMENTATION.md).*
