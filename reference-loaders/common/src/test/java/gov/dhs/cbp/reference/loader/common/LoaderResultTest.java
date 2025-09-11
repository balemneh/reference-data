package gov.dhs.cbp.reference.loader.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LoaderResultTest {
    
    private LoaderResult loaderResult;
    
    @BeforeEach
    void setUp() {
        loaderResult = new LoaderResult("exec-123", "TestLoader");
    }
    
    @Test
    void testInitialState() {
        assertEquals("exec-123", loaderResult.getExecutionId());
        assertEquals("TestLoader", loaderResult.getLoaderName());
        assertEquals(LoaderStatus.IN_PROGRESS, loaderResult.getStatus());
        assertNotNull(loaderResult.getStartTime());
        assertEquals(0, loaderResult.getRecordsRead());
        assertEquals(0, loaderResult.getRecordsStaged());
        assertFalse(loaderResult.hasErrors());
    }
    
    @Test
    void testRecordCounts() {
        loaderResult.setRecordsRead(100);
        loaderResult.setRecordsStaged(95);
        loaderResult.setRecordsAdded(10);
        loaderResult.setRecordsUpdated(20);
        loaderResult.setRecordsDeleted(5);
        loaderResult.setRecordsSkipped(5);
        
        assertEquals(100, loaderResult.getRecordsRead());
        assertEquals(95, loaderResult.getRecordsStaged());
        assertEquals(35, loaderResult.getTotalChanges());
        assertEquals(95.0, loaderResult.getSuccessRate(), 0.01);
    }
    
    @Test
    void testSuccessRateWithZeroRecords() {
        loaderResult.setRecordsRead(0);
        loaderResult.setRecordsStaged(0);
        
        assertEquals(0.0, loaderResult.getSuccessRate(), 0.01);
    }
    
    @Test
    void testValidationErrors() {
        ValidationError error1 = new ValidationError(
            0, "field1", "Error message 1", ValidationError.Severity.ERROR
        );
        ValidationError error2 = new ValidationError(
            1, "field2", "Error message 2", ValidationError.Severity.WARNING
        );
        
        loaderResult.setValidationErrors(Arrays.asList(error1, error2));
        
        assertTrue(loaderResult.hasErrors());
        assertEquals(2, loaderResult.getValidationErrors().size());
    }
    
    @Test
    void testStatusTransitions() {
        loaderResult.setStatus(LoaderStatus.SUCCESS);
        assertEquals(LoaderStatus.SUCCESS, loaderResult.getStatus());
        
        loaderResult.setStatus(LoaderStatus.FAILED);
        loaderResult.setErrorMessage("Connection timeout");
        
        assertEquals(LoaderStatus.FAILED, loaderResult.getStatus());
        assertEquals("Connection timeout", loaderResult.getErrorMessage());
        assertTrue(loaderResult.hasErrors());
    }
    
    @Test
    void testDurationCalculation() {
        LocalDateTime start = LocalDateTime.now();
        loaderResult.setStartTime(start);
        loaderResult.setEndTime(start.plusSeconds(30));
        loaderResult.setDurationMillis(30000L);
        
        assertEquals(30000L, loaderResult.getDurationMillis());
    }
    
    @Test
    void testChangeRequestTracking() {
        loaderResult.setChangeRequestId("CR-001");
        loaderResult.setChangesApplied(false);
        
        assertEquals("CR-001", loaderResult.getChangeRequestId());
        assertFalse(loaderResult.isChangesApplied());
        
        loaderResult.setChangesApplied(true);
        assertTrue(loaderResult.isChangesApplied());
    }
    
    @Test
    void testToString() {
        loaderResult.setStatus(LoaderStatus.SUCCESS);
        loaderResult.setRecordsRead(100);
        loaderResult.setRecordsStaged(95);
        loaderResult.setRecordsAdded(10);
        loaderResult.setRecordsUpdated(20);
        loaderResult.setRecordsDeleted(5);
        loaderResult.setDurationMillis(5000L);
        
        String result = loaderResult.toString();
        
        assertTrue(result.contains("TestLoader"));
        assertTrue(result.contains("SUCCESS"));
        assertTrue(result.contains("read=100"));
        assertTrue(result.contains("staged=95"));
        assertTrue(result.contains("5000ms"));
    }
    
    @Test
    void testPartialSuccess() {
        loaderResult.setStatus(LoaderStatus.PARTIAL_SUCCESS);
        loaderResult.setRecordsRead(100);
        loaderResult.setRecordsStaged(75);
        loaderResult.setRecordsSkipped(25);
        
        assertEquals(LoaderStatus.PARTIAL_SUCCESS, loaderResult.getStatus());
        assertEquals(75.0, loaderResult.getSuccessRate(), 0.01);
    }
    
    @Test
    void testCancelledStatus() {
        loaderResult.setStatus(LoaderStatus.CANCELLED);
        loaderResult.setErrorMessage("User cancelled operation");
        
        assertEquals(LoaderStatus.CANCELLED, loaderResult.getStatus());
        assertTrue(loaderResult.hasErrors());
    }
}