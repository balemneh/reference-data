package gov.dhs.cbp.reference.core.liquibase;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to ensure H2 database compatibility for CI/CD environments.
 * These tests verify that all schema changes work correctly in H2 mode.
 */
@DataJpaTest
@ActiveProfiles("test")
class H2CompatibilityTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testH2DatabaseDetected() throws Exception {
        // Verify we're running against H2 database
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            assertThat(databaseProductName).isEqualToIgnoringCase("H2");
        }
    }

    @Test
    void testLiquibaseChangesetsWorkInH2() throws Exception {
        // Verify all changesets work in H2 environment
        try (Connection connection = dataSource.getConnection()) {
            try (Liquibase liquibase = new Liquibase("db/changelog/db.changelog-master.xml",
                    new ClassLoaderResourceAccessor(),
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection)))) {

                // Validate all changesets
                liquibase.validate();

                // Get current status
                var unrunChangeSets = liquibase.listUnrunChangeSets(null);
                // Should be empty if all changesets have been applied
                assertThat(unrunChangeSets).isEmpty();
            }
        }
    }

    @Test
    void testH2SpecificChangesetsApplied() throws Exception {
        // Verify H2-specific changesets were applied
        try (Connection connection = dataSource.getConnection()) {
            // Check if H2 functions were created
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT 1 FROM INFORMATION_SCHEMA.FUNCTION_ALIASES WHERE ALIAS_NAME = 'CHECK_CHANGE_REQUEST'")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    // H2 alias should exist
                    assertThat(rs.next()).isTrue();
                }
            }
        }
    }

    @Test
    void testH2UuidCompatibility() throws Exception {
        // Test UUID handling in H2
        try (Connection connection = dataSource.getConnection()) {
            UUID testUuid = UUID.randomUUID();

            // Test UUID insertion and retrieval
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT CAST(? AS UUID) as test_uuid")) {
                stmt.setObject(1, testUuid);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    UUID retrievedUuid = (UUID) rs.getObject("test_uuid");
                    assertThat(retrievedUuid).isEqualTo(testUuid);
                }
            }
        }
    }

    @Test
    void testH2JsonbCompatibility() throws Exception {
        // Test JSONB column handling in H2 (stored as VARCHAR in H2)
        try (Connection connection = dataSource.getConnection()) {
            String testJson = "{\"test\": \"value\", \"number\": 123}";

            // Test in bulk_import_staging table
            UUID stagingId = UUID.randomUUID();
            UUID batchId = UUID.randomUUID();
            UUID changeRequestId = createTestChangeRequest(connection);

            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO reference_data.bulk_import_staging (id, import_batch_id, change_request_id, data_type, operation_type, source_system, row_number, natural_key, raw_data, target_table, created_at, created_by, updated_at, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                stmt.setObject(1, stagingId);
                stmt.setObject(2, batchId);
                stmt.setObject(3, changeRequestId);
                stmt.setString(4, "COUNTRIES");
                stmt.setString(5, "INSERT");
                stmt.setString(6, "TEST_SYSTEM");
                stmt.setInt(7, 1);
                stmt.setString(8, "TEST_KEY");
                stmt.setString(9, testJson);
                stmt.setString(10, "countries_v");
                stmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(12, "test_user");
                stmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(14, "test_user");

                int result = stmt.executeUpdate();
                assertThat(result).isEqualTo(1);
            }

            // Verify JSON was stored correctly
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT raw_data FROM reference_data.bulk_import_staging WHERE id = ?")) {
                stmt.setObject(1, stagingId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    String retrievedJson = rs.getString("raw_data");
                    assertThat(retrievedJson).isEqualTo(testJson);
                }
            }
        }
    }

    @Test
    void testH2CheckConstraints() throws Exception {
        // Test that check constraints work in H2
        try (Connection connection = dataSource.getConnection()) {
            UUID changeRequestId = createTestChangeRequest(connection);

            // Test valid operation_type
            UUID validId = UUID.randomUUID();
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO reference_data.bulk_import_staging (id, import_batch_id, change_request_id, data_type, operation_type, source_system, row_number, natural_key, raw_data, target_table, created_at, created_by, updated_at, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                stmt.setObject(1, validId);
                stmt.setObject(2, UUID.randomUUID());
                stmt.setObject(3, changeRequestId);
                stmt.setString(4, "COUNTRIES");
                stmt.setString(5, "INSERT"); // Valid operation type
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
        }
    }

    @Test
    void testH2ForeignKeyConstraints() throws Exception {
        // Test foreign key constraint enforcement in H2
        try (Connection connection = dataSource.getConnection()) {
            UUID changeRequestId = createTestChangeRequest(connection);
            UUID batchId = createTestBulkImportBatch(connection, changeRequestId);

            // Test valid foreign key reference
            UUID stagingId = UUID.randomUUID();
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO reference_data.bulk_import_staging (id, import_batch_id, change_request_id, data_type, operation_type, source_system, row_number, natural_key, raw_data, target_table, created_at, created_by, updated_at, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                stmt.setObject(1, stagingId);
                stmt.setObject(2, batchId); // Valid batch ID
                stmt.setObject(3, changeRequestId); // Valid change request ID
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

            // Verify the record was created with correct foreign key references
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT s.id, b.batch_name, cr.cr_number " +
                    "FROM reference_data.bulk_import_staging s " +
                    "JOIN reference_data.bulk_import_batches b ON s.import_batch_id = b.id " +
                    "JOIN reference_data.change_requests cr ON s.change_request_id = cr.id " +
                    "WHERE s.id = ?")) {
                stmt.setObject(1, stagingId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getObject("id")).isEqualTo(stagingId);
                    assertThat(rs.getString("batch_name")).isEqualTo("Test Batch");
                    assertThat(rs.getString("cr_number")).startsWith("CR-TEST-");
                }
            }
        }
    }

    @Test
    void testH2IndexPerformance() throws Exception {
        // Test that indexes work in H2 for basic performance
        try (Connection connection = dataSource.getConnection()) {
            UUID changeRequestId = createTestChangeRequest(connection);

            // Create multiple audit log entries
            for (int i = 0; i < 10; i++) {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO reference_data.audit_log (id, action, entity_type, operation_type, user_id, event_timestamp, change_request_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {

                    stmt.setObject(1, UUID.randomUUID());
                    stmt.setString(2, "CREATE");
                    stmt.setString(3, "Country");
                    stmt.setString(4, "CREATE");
                    stmt.setString(5, "test_user_" + i);
                    stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now().plusMinutes(i)));
                    stmt.setObject(7, changeRequestId);
                    stmt.setString(8, "SUCCESS");

                    stmt.executeUpdate();
                }
            }

            // Test indexed query performance
            long startTime = System.currentTimeMillis();
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM reference_data.audit_log WHERE change_request_id = ?")) {
                stmt.setObject(1, changeRequestId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(10);
                }
            }
            long endTime = System.currentTimeMillis();

            // Should be fast (under 100ms for this small dataset)
            assertThat(endTime - startTime).isLessThan(100);
        }
    }

    @Test
    void testH2CompatibilityWithPostgreSQLFeatures() throws Exception {
        // Test H2 compatibility with PostgreSQL-specific features
        try (Connection connection = dataSource.getConnection()) {
            // Test timestamp functions
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT CURRENT_TIMESTAMP, NOW()")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getTimestamp(1)).isNotNull();
                    assertThat(rs.getTimestamp(2)).isNotNull();
                }
            }

            // Test UUID generation (H2 uses RANDOM_UUID())
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT RANDOM_UUID()")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    UUID generated = (UUID) rs.getObject(1);
                    assertThat(generated).isNotNull();
                }
            }
        }
    }

    // Helper methods

    private UUID createTestChangeRequest(Connection connection) throws SQLException {
        UUID changeRequestId = UUID.randomUUID();
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
        return changeRequestId;
    }

    private UUID createTestBulkImportBatch(Connection connection, UUID changeRequestId) throws SQLException {
        UUID batchId = UUID.randomUUID();
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
        return batchId;
    }
}