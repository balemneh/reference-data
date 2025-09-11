package gov.dhs.cbp.reference.core.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BitemporalTest {

    private TestBitemporalEntity entity;

    @BeforeEach
    void setUp() {
        entity = new TestBitemporalEntity();
    }

    @Test
    void testIdGenerationAndSetter() {
        UUID id = UUID.randomUUID();
        entity.setId(id);
        assertEquals(id, entity.getId());
    }

    @Test
    void testValidFromAndTo() {
        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().plusYears(1);
        
        entity.setValidFrom(from);
        entity.setValidTo(to);
        
        assertEquals(from, entity.getValidFrom());
        assertEquals(to, entity.getValidTo());
    }

    @Test
    void testRecordedAtAndBy() {
        LocalDateTime recordedAt = LocalDateTime.now();
        String recordedBy = "test-user";
        
        entity.setRecordedAt(recordedAt);
        entity.setRecordedBy(recordedBy);
        
        assertEquals(recordedAt, entity.getRecordedAt());
        assertEquals(recordedBy, entity.getRecordedBy());
    }

    @Test
    void testVersioning() {
        entity.setVersion(1L);
        assertEquals(1L, entity.getVersion());
        
        entity.setVersion(2L);
        assertEquals(2L, entity.getVersion());
    }

    @Test
    void testIsCorrection() {
        entity.setIsCorrection(false);
        assertFalse(entity.getIsCorrection());
        
        entity.setIsCorrection(true);
        assertTrue(entity.getIsCorrection());
    }

    @Test
    void testChangeRequestId() {
        String changeRequestId = UUID.randomUUID().toString();
        entity.setChangeRequestId(changeRequestId);
        assertEquals(changeRequestId, entity.getChangeRequestId());
    }


    @Test
    void testMetadata() {
        String metadata = "{\"source\": \"ISO\", \"updateReason\": \"Annual review\"}";
        entity.setMetadata(metadata);
        assertEquals(metadata, entity.getMetadata());
    }

    @Test
    void testIsCurrentlyValid() {
        // Test when valid_to is null (currently valid)
        entity.setValidFrom(LocalDate.now().minusDays(1));
        entity.setValidTo(null);
        assertTrue(entity.isCurrentlyValid());
        
        // Test when valid_to is in the future (currently valid)
        entity.setValidTo(LocalDate.now().plusDays(1));
        assertTrue(entity.isCurrentlyValid());
        
        // Test when valid_to is in the past (not valid)
        entity.setValidTo(LocalDate.now().minusDays(1));
        assertFalse(entity.isCurrentlyValid());
        
        // Test when valid_from is in the future (not valid yet)
        entity.setValidFrom(LocalDate.now().plusDays(1));
        entity.setValidTo(LocalDate.now().plusDays(10));
        assertFalse(entity.isCurrentlyValid());
    }

    @Test
    void testWasValidOn() {
        LocalDate checkDate = LocalDate.of(2024, 6, 15);
        
        // Valid range includes check date
        entity.setValidFrom(LocalDate.of(2024, 1, 1));
        entity.setValidTo(LocalDate.of(2024, 12, 31));
        assertTrue(entity.wasValidOn(checkDate));
        
        // Valid range before check date
        entity.setValidFrom(LocalDate.of(2023, 1, 1));
        entity.setValidTo(LocalDate.of(2023, 12, 31));
        assertFalse(entity.wasValidOn(checkDate));
        
        // Valid range after check date
        entity.setValidFrom(LocalDate.of(2025, 1, 1));
        entity.setValidTo(LocalDate.of(2025, 12, 31));
        assertFalse(entity.wasValidOn(checkDate));
        
        // Open-ended validity (null valid_to)
        entity.setValidFrom(LocalDate.of(2024, 1, 1));
        entity.setValidTo(null);
        assertTrue(entity.wasValidOn(checkDate));
    }

    @Test
    void testDefaultValues() {
        TestBitemporalEntity newEntity = new TestBitemporalEntity();
        assertNull(newEntity.getId());
        assertNull(newEntity.getValidFrom());
        assertNull(newEntity.getValidTo());
        assertNull(newEntity.getRecordedAt());
        assertNull(newEntity.getRecordedBy());
        assertNull(newEntity.getVersion());
        assertFalse(newEntity.getIsCorrection()); // Default is false
        assertNull(newEntity.getChangeRequestId());
        assertNull(newEntity.getMetadata());
    }

    @Test
    void testEqualsAndHashCode() {
        UUID id = UUID.randomUUID();
        entity.setId(id);
        
        TestBitemporalEntity other = new TestBitemporalEntity();
        other.setId(id);
        
        // Bitemporal doesn't override equals/hashCode, so they use object identity
        assertNotEquals(entity, other); // Different objects
        assertNotEquals(entity.hashCode(), other.hashCode());
        
        // Same object should equal itself
        assertEquals(entity, entity);
        assertEquals(entity.hashCode(), entity.hashCode());
    }

    // Test implementation class
    private static class TestBitemporalEntity extends Bitemporal {
        // Inherits all Bitemporal fields and methods
    }
}