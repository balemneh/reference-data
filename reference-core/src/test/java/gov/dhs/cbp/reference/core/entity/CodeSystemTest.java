package gov.dhs.cbp.reference.core.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CodeSystemTest {
    
    private CodeSystem codeSystem;
    
    @BeforeEach
    void setUp() {
        codeSystem = new CodeSystem();
    }
    
    @Test
    void testCodeSystemCreation() {
        codeSystem.setCode("ISO3166-1");
        codeSystem.setName("ISO Country Codes");
        codeSystem.setDescription("ISO standard for country codes");
        codeSystem.setOwner("ISO");
        codeSystem.setIsActive(true);
        
        assertEquals("ISO3166-1", codeSystem.getCode());
        assertEquals("ISO Country Codes", codeSystem.getName());
        assertEquals("ISO standard for country codes", codeSystem.getDescription());
        assertEquals("ISO", codeSystem.getOwner());
        assertTrue(codeSystem.getIsActive());
    }
    
    @Test
    void testIdField() {
        UUID id = UUID.randomUUID();
        codeSystem.setId(id);
        
        assertEquals(id, codeSystem.getId());
    }
    
    @Test
    void testTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        codeSystem.setCreatedAt(now);
        codeSystem.setUpdatedAt(now.plusHours(1));
        
        assertEquals(now, codeSystem.getCreatedAt());
        assertEquals(now.plusHours(1), codeSystem.getUpdatedAt());
    }
    
    @Test
    void testInactiveCodeSystem() {
        codeSystem.setIsActive(false);
        assertFalse(codeSystem.getIsActive());
    }
    
    @Test
    void testOwnerField() {
        codeSystem.setOwner("International Organization for Standardization");
        assertEquals("International Organization for Standardization", codeSystem.getOwner());
    }
    
    @Test
    void testCodeUniqueness() {
        codeSystem.setCode("UNIQUE_CODE_123");
        assertEquals("UNIQUE_CODE_123", codeSystem.getCode());
    }
    
    @Test
    void testNullDescription() {
        codeSystem.setDescription(null);
        assertNull(codeSystem.getDescription());
    }
}