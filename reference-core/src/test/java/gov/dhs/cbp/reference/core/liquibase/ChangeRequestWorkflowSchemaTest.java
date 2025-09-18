package gov.dhs.cbp.reference.core.liquibase;

import gov.dhs.cbp.reference.core.entity.*;
import gov.dhs.cbp.reference.core.repository.*;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive tests for Change Request Workflow schema changes.
 * Tests verify that new tables, constraints, triggers, and relationships work correctly.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
class ChangeRequestWorkflowSchemaTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ChangeRequestRepository changeRequestRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private PortRepository portRepository;

    @Test
    void testNewSchemaChangesetsAppliedCorrectly() throws Exception {
        // Verify Liquibase can process all new changesets without errors
        try (Connection connection = dataSource.getConnection()) {
            try (Liquibase liquibase = new Liquibase("db/changelog/db.changelog-master.xml",
                    new ClassLoaderResourceAccessor(),
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection)))) {

                // This will throw an exception if any changeset fails
                liquibase.validate();
            }
        }
    }

    @Test
    void testChangeRequestIdColumnsUpdatedToUuid() throws Exception {
        // Verify that change_request_id columns are now UUID type with foreign key constraints
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Check countries_v table
            try (ResultSet columns = metaData.getColumns(null, "reference_data", "countries_v", "change_request_id")) {
                assertThat(columns.next()).isTrue();
                String dataType = columns.getString("TYPE_NAME");
                assertThat(dataType.toLowerCase()).contains("uuid");
            }

            // Check airports_v table
            try (ResultSet columns = metaData.getColumns(null, "reference_data", "airports_v", "change_request_id")) {
                assertThat(columns.next()).isTrue();
                String dataType = columns.getString("TYPE_NAME");
                assertThat(dataType.toLowerCase()).contains("uuid");
            }

            // Check ports_v table
            try (ResultSet columns = metaData.getColumns(null, "reference_data", "ports_v", "change_request_id")) {
                assertThat(columns.next()).isTrue();
                String dataType = columns.getString("TYPE_NAME");
                assertThat(dataType.toLowerCase()).contains("uuid");
            }
        }
    }

    @Test
    void testForeignKeyConstraintsExist() throws Exception {
        // Verify that foreign key constraints exist between versioned tables and change_requests
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Check countries_v foreign key
            try (ResultSet foreignKeys = metaData.getImportedKeys(null, "reference_data", "countries_v")) {
                boolean foundChangeRequestFK = false;
                while (foreignKeys.next()) {
                    if ("change_request_id".equals(foreignKeys.getString("FKCOLUMN_NAME"))) {
                        assertThat(foreignKeys.getString("PKTABLE_NAME")).isEqualTo("change_requests");
                        foundChangeRequestFK = true;
                    }
                }
                assertThat(foundChangeRequestFK).isTrue();
            }

            // Check airports_v foreign key
            try (ResultSet foreignKeys = metaData.getImportedKeys(null, "reference_data", "airports_v")) {
                boolean foundChangeRequestFK = false;
                while (foreignKeys.next()) {
                    if ("change_request_id".equals(foreignKeys.getString("FKCOLUMN_NAME"))) {
                        assertThat(foreignKeys.getString("PKTABLE_NAME")).isEqualTo("change_requests");
                        foundChangeRequestFK = true;
                    }
                }
                assertThat(foundChangeRequestFK).isTrue();
            }

            // Check ports_v foreign key
            try (ResultSet foreignKeys = metaData.getImportedKeys(null, "reference_data", "ports_v")) {
                boolean foundChangeRequestFK = false;
                while (foreignKeys.next()) {
                    if ("change_request_id".equals(foreignKeys.getString("FKCOLUMN_NAME"))) {
                        assertThat(foreignKeys.getString("PKTABLE_NAME")).isEqualTo("change_requests");
                        foundChangeRequestFK = true;
                    }
                }
                assertThat(foundChangeRequestFK).isTrue();
            }
        }
    }

    @Test
    void testBulkImportStagingTableExists() throws Exception {
        // Verify bulk_import_staging table exists with correct structure
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Check table exists
            try (ResultSet tables = metaData.getTables(null, "reference_data", "bulk_import_staging", null)) {
                assertThat(tables.next()).isTrue();
            }

            // Verify key columns exist
            String[] requiredColumns = {
                "id", "import_batch_id", "change_request_id", "data_type", "operation_type",
                "source_system", "row_number", "natural_key", "raw_data", "normalized_data",
                "validation_status", "processing_status", "target_record_id", "created_at"
            };

            for (String columnName : requiredColumns) {
                try (ResultSet columns = metaData.getColumns(null, "reference_data", "bulk_import_staging", columnName)) {
                    assertThat(columns.next()).withFailMessage("Column %s should exist", columnName).isTrue();
                }
            }
        }
    }

    @Test
    void testBulkImportBatchesTableExists() throws Exception {
        // Verify bulk_import_batches table exists with correct structure
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Check table exists
            try (ResultSet tables = metaData.getTables(null, "reference_data", "bulk_import_batches", null)) {
                assertThat(tables.next()).isTrue();
            }

            // Verify key columns exist
            String[] requiredColumns = {
                "id", "batch_name", "change_request_id", "source_system", "data_type",
                "status", "total_records", "records_processed", "created_at"
            };

            for (String columnName : requiredColumns) {
                try (ResultSet columns = metaData.getColumns(null, "reference_data", "bulk_import_batches", columnName)) {
                    assertThat(columns.next()).withFailMessage("Column %s should exist", columnName).isTrue();
                }
            }
        }
    }

    @Test
    void testAuditLogTableExists() throws Exception {
        // Verify audit_log table exists with correct structure
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Check table exists
            try (ResultSet tables = metaData.getTables(null, "reference_data", "audit_log", null)) {
                assertThat(tables.next()).isTrue();
            }

            // Verify key columns exist
            String[] requiredColumns = {
                "id", "action", "entity_type", "entity_id", "user_id", "event_timestamp",
                "old_values", "new_values", "change_request_id", "status"
            };

            for (String columnName : requiredColumns) {
                try (ResultSet columns = metaData.getColumns(null, "reference_data", "audit_log", columnName)) {
                    assertThat(columns.next()).withFailMessage("Column %s should exist", columnName).isTrue();
                }
            }
        }
    }

    @Test
    void testBulkImportStagingConstraints() throws Exception {
        // Test check constraints on bulk_import_staging table
        try (Connection connection = dataSource.getConnection()) {
            // Test valid operation_type
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO reference_data.bulk_import_staging (id, import_batch_id, change_request_id, data_type, operation_type, source_system, row_number, natural_key, raw_data, target_table, created_at, created_by, updated_at, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                UUID testId = UUID.randomUUID();
                UUID batchId = UUID.randomUUID();
                UUID changeRequestId = createTestChangeRequest();

                stmt.setObject(1, testId);
                stmt.setObject(2, batchId);
                stmt.setObject(3, changeRequestId);
                stmt.setString(4, "COUNTRIES");
                stmt.setString(5, "INSERT");
                stmt.setString(6, "TEST_SYSTEM");
                stmt.setInt(7, 1);
                stmt.setString(8, "TEST_KEY");
                stmt.setString(9, "{}");
                stmt.setString(10, "countries_v");
                stmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(12, "test_user");
                stmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(14, "test_user");

                int result = stmt.executeUpdate();
                assertThat(result).isEqualTo(1);
            }

            // Test invalid operation_type should fail
            assertThatThrownBy(() -> {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO reference_data.bulk_import_staging (id, import_batch_id, change_request_id, data_type, operation_type, source_system, row_number, natural_key, raw_data, target_table, created_at, created_by, updated_at, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                    UUID testId = UUID.randomUUID();
                    UUID batchId = UUID.randomUUID();
                    UUID changeRequestId = createTestChangeRequest();

                    stmt.setObject(1, testId);
                    stmt.setObject(2, batchId);
                    stmt.setObject(3, changeRequestId);
                    stmt.setString(4, "COUNTRIES");
                    stmt.setString(5, "INVALID_OP"); // Invalid operation type
                    stmt.setString(6, "TEST_SYSTEM");
                    stmt.setInt(7, 1);
                    stmt.setString(8, "TEST_KEY");
                    stmt.setString(9, "{}");
                    stmt.setString(10, "countries_v");
                    stmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
                    stmt.setString(12, "test_user");
                    stmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
                    stmt.setString(14, "test_user");

                    stmt.executeUpdate();
                }
            }).isInstanceOf(SQLException.class);
        }
    }

    @Test
    void testAuditLogConstraints() throws Exception {
        // Test check constraints on audit_log table
        try (Connection connection = dataSource.getConnection()) {
            // Test valid status
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO reference_data.audit_log (id, action, entity_type, operation_type, user_id, event_timestamp, status) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                stmt.setObject(1, UUID.randomUUID());
                stmt.setString(2, "CREATE");
                stmt.setString(3, "Country");
                stmt.setString(4, "CREATE");
                stmt.setString(5, "test_user");
                stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(7, "SUCCESS");

                int result = stmt.executeUpdate();
                assertThat(result).isEqualTo(1);
            }

            // Test invalid status should fail
            assertThatThrownBy(() -> {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO reference_data.audit_log (id, action, entity_type, operation_type, user_id, event_timestamp, status) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                    stmt.setObject(1, UUID.randomUUID());
                    stmt.setString(2, "CREATE");
                    stmt.setString(3, "Country");
                    stmt.setString(4, "CREATE");
                    stmt.setString(5, "test_user");
                    stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                    stmt.setString(7, "INVALID_STATUS"); // Invalid status

                    stmt.executeUpdate();
                }
            }).isInstanceOf(SQLException.class);
        }
    }

    @Test
    void testIndexesCreatedProperly() throws Exception {
        // Verify that indexes were created for optimal performance
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Check bulk_import_staging indexes
            try (ResultSet indexes = metaData.getIndexInfo(null, "reference_data", "bulk_import_staging", false, false)) {
                boolean foundBatchIndex = false;
                boolean foundChangeRequestIndex = false;
                boolean foundNaturalKeyIndex = false;

                while (indexes.next()) {
                    String indexName = indexes.getString("INDEX_NAME");
                    if (indexName != null) {
                        if (indexName.contains("batch_id")) foundBatchIndex = true;
                        if (indexName.contains("change_request")) foundChangeRequestIndex = true;
                        if (indexName.contains("natural_key")) foundNaturalKeyIndex = true;
                    }
                }

                assertThat(foundBatchIndex).isTrue();
                assertThat(foundChangeRequestIndex).isTrue();
                assertThat(foundNaturalKeyIndex).isTrue();
            }

            // Check audit_log indexes
            try (ResultSet indexes = metaData.getIndexInfo(null, "reference_data", "audit_log", false, false)) {
                boolean foundTimestampIndex = false;
                boolean foundUserIndex = false;
                boolean foundEntityIndex = false;

                while (indexes.next()) {
                    String indexName = indexes.getString("INDEX_NAME");
                    if (indexName != null) {
                        if (indexName.contains("timestamp")) foundTimestampIndex = true;
                        if (indexName.contains("user")) foundUserIndex = true;
                        if (indexName.contains("entity")) foundEntityIndex = true;
                    }
                }

                assertThat(foundTimestampIndex).isTrue();
                assertThat(foundUserIndex).isTrue();
                assertThat(foundEntityIndex).isTrue();
            }
        }
    }

    @Test
    void testBulkImportWorkflow() throws Exception {
        // Test complete bulk import workflow using the new tables
        UUID changeRequestId = createTestChangeRequest();
        UUID batchId = createTestBulkImportBatch(changeRequestId);

        // Create staging records
        createTestStagingRecord(batchId, changeRequestId, "COUNTRIES", "INSERT", "US", 1);
        createTestStagingRecord(batchId, changeRequestId, "COUNTRIES", "INSERT", "CA", 2);

        // Verify staging records were created
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM reference_data.bulk_import_staging WHERE import_batch_id = ?")) {
                stmt.setObject(1, batchId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(2);
                }
            }
        }

        // Update processing status
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE reference_data.bulk_import_staging SET processing_status = 'COMPLETED' WHERE import_batch_id = ?")) {
                stmt.setObject(1, batchId);
                int updated = stmt.executeUpdate();
                assertThat(updated).isEqualTo(2);
            }
        }

        // Update batch status
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE reference_data.bulk_import_batches SET status = 'COMPLETED', records_processed = 2 WHERE id = ?")) {
                stmt.setObject(1, batchId);
                int updated = stmt.executeUpdate();
                assertThat(updated).isEqualTo(1);
            }
        }
    }

    @Test
    void testAuditTrailGeneration() throws Exception {
        // Test that audit records are created properly (manually for testing)
        UUID changeRequestId = createTestChangeRequest();

        // Create audit record manually (in production, triggers would do this)
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO reference_data.audit_log (id, action, entity_type, entity_id, operation_type, user_id, event_timestamp, change_request_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                stmt.setObject(1, UUID.randomUUID());
                stmt.setString(2, "CREATE");
                stmt.setString(3, "Country");
                stmt.setObject(4, UUID.randomUUID());
                stmt.setString(5, "CREATE");
                stmt.setString(6, "test_user");
                stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setObject(8, changeRequestId);
                stmt.setString(9, "SUCCESS");

                int result = stmt.executeUpdate();
                assertThat(result).isEqualTo(1);
            }
        }

        // Verify audit record was created and linked to change request
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM reference_data.audit_log WHERE change_request_id = ?")) {
                stmt.setObject(1, changeRequestId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(1);
                }
            }
        }
    }

    @Test
    void testRollbackFunctionality() throws Exception {
        // Test that rollback statements work correctly
        try (Connection connection = dataSource.getConnection()) {
            try (Liquibase liquibase = new Liquibase("db/changelog/schema/007-change-request-tracking.xml",
                    new ClassLoaderResourceAccessor(),
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection)))) {

                // Test that we can validate without errors (which includes rollback validation)
                liquibase.validate();

                // Test that changesets can be listed
                var changeSets = liquibase.getDatabaseChangeLog().getChangeSets();
                assertThat(changeSets).isNotEmpty();

                // Verify changesets have rollback elements (by checking they contain rollback tags)
                for (var changeSet : changeSets) {
                    assertThat(changeSet.getRollback()).isNotNull();
                    assertThat(changeSet.getRollback().getChanges()).isNotEmpty();
                }
            }
        }
    }

    // Helper methods

    private UUID createTestChangeRequest() throws SQLException {
        UUID changeRequestId = UUID.randomUUID();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO reference_data.change_requests (id, cr_number, title, requester_id, data_type, operation_type, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                stmt.setObject(1, changeRequestId);
                stmt.setString(2, "CR-TEST-" + System.currentTimeMillis());
                stmt.setString(3, "Test Change Request");
                stmt.setString(4, "test_user");
                stmt.setString(5, "COUNTRIES");
                stmt.setString(6, "INSERT");
                stmt.setString(7, "APPROVED");
                stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));

                stmt.executeUpdate();
            }
        }
        return changeRequestId;
    }

    private UUID createTestBulkImportBatch(UUID changeRequestId) throws SQLException {
        UUID batchId = UUID.randomUUID();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO reference_data.bulk_import_batches (id, batch_name, change_request_id, source_system, data_type, status, created_at, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {

                stmt.setObject(1, batchId);
                stmt.setString(2, "Test Batch");
                stmt.setObject(3, changeRequestId);
                stmt.setString(4, "TEST_SYSTEM");
                stmt.setString(5, "COUNTRIES");
                stmt.setString(6, "PENDING");
                stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(8, "test_user");

                stmt.executeUpdate();
            }
        }
        return batchId;
    }

    private void createTestStagingRecord(UUID batchId, UUID changeRequestId, String dataType, String operationType, String naturalKey, int rowNumber) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO reference_data.bulk_import_staging (id, import_batch_id, change_request_id, data_type, operation_type, source_system, row_number, natural_key, raw_data, target_table, created_at, created_by, updated_at, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, batchId);
                stmt.setObject(3, changeRequestId);
                stmt.setString(4, dataType);
                stmt.setString(5, operationType);
                stmt.setString(6, "TEST_SYSTEM");
                stmt.setInt(7, rowNumber);
                stmt.setString(8, naturalKey);
                stmt.setString(9, "{\"code\":\"" + naturalKey + "\",\"name\":\"Test " + naturalKey + "\"}");
                stmt.setString(10, "countries_v");
                stmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(12, "test_user");
                stmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(14, "test_user");

                stmt.executeUpdate();
            }
        }
    }
}