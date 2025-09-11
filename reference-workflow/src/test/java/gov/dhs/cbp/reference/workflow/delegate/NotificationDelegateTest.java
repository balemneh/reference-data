package gov.dhs.cbp.reference.workflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDelegateTest {

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private NotificationDelegate notificationDelegate;

    private UUID requestId;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        when(execution.getVariable("requestId")).thenReturn(requestId);
        when(execution.getVariable("requestor")).thenReturn("test-user");
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
    }

    @Test
    void testExecute_ValidationFailed() throws Exception {
        // Given
        when(execution.getVariable("dataValid")).thenReturn(false);
        when(execution.getVariable("validationErrors")).thenReturn("Invalid country code");
        when(execution.getVariable("changeApplied")).thenReturn(null);
        when(execution.getVariable("policyApproved")).thenReturn(null);
        when(execution.getVariable("approved")).thenReturn(null);

        // When
        notificationDelegate.execute(execution);

        // Then
        verify(execution).setVariable("notificationSent", true);
        verify(execution).setVariable("finalStatus", "VALIDATION_FAILED");
    }

    @Test
    void testExecute_PolicyRejected() throws Exception {
        // Given
        when(execution.getVariable("dataValid")).thenReturn(true);
        when(execution.getVariable("policyApproved")).thenReturn(false);
        when(execution.getVariable("approved")).thenReturn(false);
        when(execution.getVariable("policyReason")).thenReturn("Insufficient permissions");
        when(execution.getVariable("changeApplied")).thenReturn(null);

        // When
        notificationDelegate.execute(execution);

        // Then
        verify(execution).setVariable("notificationSent", true);
        verify(execution).setVariable("finalStatus", "POLICY_REJECTED");
    }

    @Test
    void testExecute_ManuallyRejected() throws Exception {
        // Given
        when(execution.getVariable("dataValid")).thenReturn(true);
        when(execution.getVariable("policyApproved")).thenReturn(true);
        when(execution.getVariable("approved")).thenReturn(false);
        when(execution.getVariable("comments")).thenReturn("Missing documentation");
        when(execution.getVariable("changeApplied")).thenReturn(null);

        // When
        notificationDelegate.execute(execution);

        // Then
        verify(execution).setVariable("notificationSent", true);
        verify(execution).setVariable("finalStatus", "MANUALLY_REJECTED");
    }

    @Test
    void testExecute_SuccessfullyApplied() throws Exception {
        // Given
        when(execution.getVariable("dataValid")).thenReturn(true);
        when(execution.getVariable("policyApproved")).thenReturn(true);
        when(execution.getVariable("approved")).thenReturn(true);
        when(execution.getVariable("changeApplied")).thenReturn(true);

        // When
        notificationDelegate.execute(execution);

        // Then
        verify(execution).setVariable("notificationSent", true);
        verify(execution).setVariable("finalStatus", "APPLIED");
    }

    @Test
    void testExecute_FailedToApply() throws Exception {
        // Given
        when(execution.getVariable("dataValid")).thenReturn(true);
        when(execution.getVariable("policyApproved")).thenReturn(true);
        when(execution.getVariable("approved")).thenReturn(true);
        when(execution.getVariable("changeApplied")).thenReturn(false);
        when(execution.getVariable("changeError")).thenReturn("Database connection error");

        // When
        notificationDelegate.execute(execution);

        // Then
        verify(execution).setVariable("notificationSent", true);
        verify(execution).setVariable("finalStatus", "FAILED");
    }

    @Test
    void testExecute_NullValues() throws Exception {
        // Given
        when(execution.getVariable("dataValid")).thenReturn(null);
        when(execution.getVariable("policyApproved")).thenReturn(null);
        when(execution.getVariable("approved")).thenReturn(null);
        when(execution.getVariable("changeApplied")).thenReturn(null);

        // When
        notificationDelegate.execute(execution);

        // Then
        verify(execution).setVariable("notificationSent", true);
        verify(execution).setVariable("finalStatus", "FAILED");
    }

    @Test
    void testExecute_PolicyApprovedButManuallyRejected() throws Exception {
        // Given
        when(execution.getVariable("dataValid")).thenReturn(true);
        when(execution.getVariable("policyApproved")).thenReturn(true);
        when(execution.getVariable("approved")).thenReturn(false);
        when(execution.getVariable("comments")).thenReturn("Needs further review");
        when(execution.getVariable("changeApplied")).thenReturn(null);

        // When
        notificationDelegate.execute(execution);

        // Then
        verify(execution).setVariable("notificationSent", true);
        verify(execution).setVariable("finalStatus", "MANUALLY_REJECTED");
    }

    @Test
    void testExecute_PolicyRejectedButManuallyApproved() throws Exception {
        // Given - this tests the edge case where policy rejected but manual approval overrides
        when(execution.getVariable("dataValid")).thenReturn(true);
        when(execution.getVariable("policyApproved")).thenReturn(false);
        when(execution.getVariable("approved")).thenReturn(true);
        when(execution.getVariable("changeApplied")).thenReturn(true);

        // When
        notificationDelegate.execute(execution);

        // Then
        verify(execution).setVariable("notificationSent", true);
        verify(execution).setVariable("finalStatus", "APPLIED");
    }

    @Test
    void testExecute_AllDatasetTypes() throws Exception {
        String[] datasetTypes = {"COUNTRY", "PORT", "AIRPORT", "CARRIER"};
        
        for (String datasetType : datasetTypes) {
            // Given
            when(execution.getVariable("datasetType")).thenReturn(datasetType);
            when(execution.getVariable("dataValid")).thenReturn(true);
            when(execution.getVariable("policyApproved")).thenReturn(true);
            when(execution.getVariable("approved")).thenReturn(true);
            when(execution.getVariable("changeApplied")).thenReturn(true);
            
            // When
            notificationDelegate.execute(execution);
            
            // Then
            verify(execution, atLeastOnce()).setVariable("notificationSent", true);
            verify(execution, atLeastOnce()).setVariable("finalStatus", "APPLIED");
        }
    }

    @Test
    void testExecute_ComplexFailureScenario() throws Exception {
        // Given - data valid but both policy and manual approval failed
        when(execution.getVariable("dataValid")).thenReturn(true);
        when(execution.getVariable("policyApproved")).thenReturn(false);
        when(execution.getVariable("approved")).thenReturn(null);
        when(execution.getVariable("policyReason")).thenReturn("Policy check failed");
        when(execution.getVariable("changeApplied")).thenReturn(null);

        // When
        notificationDelegate.execute(execution);

        // Then
        verify(execution).setVariable("notificationSent", true);
        verify(execution).setVariable("finalStatus", "POLICY_REJECTED");
    }
}