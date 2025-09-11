package gov.dhs.cbp.reference.workflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplyChangeDelegateTest {

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private ApplyChangeDelegate applyChangeDelegate;

    private UUID requestId;
    private Map<String, Object> payload;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        payload = new HashMap<>();
        payload.put("code", "US");
        payload.put("name", "United States");
    }

    @Test
    void testExecute_CountryChange_Success() throws Exception {
        // Given
        when(execution.getVariable("requestId")).thenReturn(requestId);
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
        when(execution.getVariable("changeType")).thenReturn("UPDATE");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        applyChangeDelegate.execute(execution);

        // Then
        verify(execution).setVariable("changeApplied", true);
        verify(execution).setVariable(eq("changeAppliedAt"), anyLong());
        verify(execution, never()).setVariable(eq("changeError"), anyString());
    }

    @Test
    void testExecute_PortChange_Success() throws Exception {
        // Given
        when(execution.getVariable("requestId")).thenReturn(requestId);
        when(execution.getVariable("datasetType")).thenReturn("PORT");
        when(execution.getVariable("changeType")).thenReturn("CREATE");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        applyChangeDelegate.execute(execution);

        // Then
        verify(execution).setVariable("changeApplied", true);
        verify(execution).setVariable(eq("changeAppliedAt"), anyLong());
    }

    @Test
    void testExecute_AirportChange_Success() throws Exception {
        // Given
        when(execution.getVariable("requestId")).thenReturn(requestId);
        when(execution.getVariable("datasetType")).thenReturn("AIRPORT");
        when(execution.getVariable("changeType")).thenReturn("UPDATE");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        applyChangeDelegate.execute(execution);

        // Then
        verify(execution).setVariable("changeApplied", true);
        verify(execution).setVariable(eq("changeAppliedAt"), anyLong());
    }

    @Test
    void testExecute_CarrierChange_Success() throws Exception {
        // Given
        when(execution.getVariable("requestId")).thenReturn(requestId);
        when(execution.getVariable("datasetType")).thenReturn("CARRIER");
        when(execution.getVariable("changeType")).thenReturn("DELETE");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        applyChangeDelegate.execute(execution);

        // Then
        verify(execution).setVariable("changeApplied", true);
        verify(execution).setVariable(eq("changeAppliedAt"), anyLong());
    }

    @Test
    void testExecute_UnknownDatasetType() throws Exception {
        // Given
        when(execution.getVariable("requestId")).thenReturn(requestId);
        when(execution.getVariable("datasetType")).thenReturn("UNKNOWN");
        when(execution.getVariable("changeType")).thenReturn("UPDATE");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        applyChangeDelegate.execute(execution);

        // Then
        verify(execution).setVariable("changeApplied", true);
        verify(execution).setVariable(eq("changeAppliedAt"), anyLong());
    }

    @Test
    void testExecute_WithException() throws Exception {
        // Given
        when(execution.getVariable("requestId")).thenReturn(requestId);
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
        when(execution.getVariable("changeType")).thenReturn("UPDATE");
        when(execution.getVariable("payload")).thenReturn(payload);
        
        // Simulate an exception during execution
        doThrow(new RuntimeException("Test exception"))
            .when(execution).setVariable(eq("changeApplied"), anyBoolean());

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            applyChangeDelegate.execute(execution);
        });
        
        assertEquals("Test exception", exception.getMessage());
    }

    @Test
    void testExecute_NullPayload() throws Exception {
        // Given
        when(execution.getVariable("requestId")).thenReturn(requestId);
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
        when(execution.getVariable("changeType")).thenReturn("UPDATE");
        when(execution.getVariable("payload")).thenReturn(null);

        // When
        applyChangeDelegate.execute(execution);

        // Then
        verify(execution).setVariable("changeApplied", true);
        verify(execution).setVariable(eq("changeAppliedAt"), anyLong());
    }

    @Test
    void testExecute_EmptyPayload() throws Exception {
        // Given
        when(execution.getVariable("requestId")).thenReturn(requestId);
        when(execution.getVariable("datasetType")).thenReturn("PORT");
        when(execution.getVariable("changeType")).thenReturn("CREATE");
        when(execution.getVariable("payload")).thenReturn(new HashMap<>());

        // When
        applyChangeDelegate.execute(execution);

        // Then
        verify(execution).setVariable("changeApplied", true);
        verify(execution).setVariable(eq("changeAppliedAt"), anyLong());
    }

    @Test
    void testExecute_AllChangeTypes() throws Exception {
        // Test CREATE
        when(execution.getVariable("requestId")).thenReturn(requestId);
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
        when(execution.getVariable("changeType")).thenReturn("CREATE");
        when(execution.getVariable("payload")).thenReturn(payload);
        
        applyChangeDelegate.execute(execution);
        verify(execution, times(1)).setVariable("changeApplied", true);
        
        // Test UPDATE
        when(execution.getVariable("changeType")).thenReturn("UPDATE");
        applyChangeDelegate.execute(execution);
        verify(execution, times(2)).setVariable("changeApplied", true);
        
        // Test DELETE
        when(execution.getVariable("changeType")).thenReturn("DELETE");
        applyChangeDelegate.execute(execution);
        verify(execution, times(3)).setVariable("changeApplied", true);
    }
}