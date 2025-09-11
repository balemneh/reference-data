package gov.dhs.cbp.reference.workflow.delegate;

import gov.dhs.cbp.reference.workflow.service.OpaPolicyService;
import gov.dhs.cbp.reference.workflow.service.OpaPolicyService.PolicyDecision;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckPolicyDelegateTest {

    @Mock
    private DelegateExecution execution;

    @Mock
    private OpaPolicyService opaPolicyService;

    @InjectMocks
    private CheckPolicyDelegate checkPolicyDelegate;

    private Map<String, Object> payload;

    @BeforeEach
    void setUp() {
        payload = new HashMap<>();
        payload.put("code", "US");
        payload.put("name", "United States");
    }

    @Test
    void testExecute_PolicyApproved() throws Exception {
        // Given
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
        when(execution.getVariable("changeType")).thenReturn("UPDATE");
        when(execution.getVariable("requestor")).thenReturn("test-user");
        when(execution.getVariable("payload")).thenReturn(payload);
        
        PolicyDecision approvedDecision = new PolicyDecision(true, "All checks passed", false);
        when(opaPolicyService.evaluatePolicy(eq("change_request_approval"), any(Map.class)))
                .thenReturn(approvedDecision);

        // When
        checkPolicyDelegate.execute(execution);

        // Then
        verify(execution).setVariable("policyApproved", true);
        verify(execution).setVariable("policyReason", "All checks passed");
        verify(execution).setVariable("requiresAdditionalApproval", false);
    }

    @Test
    void testExecute_PolicyRejected() throws Exception {
        // Given
        when(execution.getVariable("datasetType")).thenReturn("PORT");
        when(execution.getVariable("changeType")).thenReturn("DELETE");
        when(execution.getVariable("requestor")).thenReturn("unauthorized-user");
        when(execution.getVariable("payload")).thenReturn(payload);
        
        PolicyDecision rejectedDecision = new PolicyDecision(false, "User lacks permissions", true);
        when(opaPolicyService.evaluatePolicy(eq("change_request_approval"), any(Map.class)))
                .thenReturn(rejectedDecision);

        // When
        checkPolicyDelegate.execute(execution);

        // Then
        verify(execution).setVariable("policyApproved", false);
        verify(execution).setVariable("policyReason", "User lacks permissions");
        verify(execution).setVariable("requiresAdditionalApproval", true);
    }

    @Test
    void testExecute_RequiresAdditionalApproval() throws Exception {
        // Given
        when(execution.getVariable("datasetType")).thenReturn("AIRPORT");
        when(execution.getVariable("changeType")).thenReturn("CREATE");
        when(execution.getVariable("requestor")).thenReturn("standard-user");
        when(execution.getVariable("payload")).thenReturn(payload);
        
        PolicyDecision decisionWithAdditionalApproval = new PolicyDecision(true, "Approved with conditions", true);
        when(opaPolicyService.evaluatePolicy(eq("change_request_approval"), any(Map.class)))
                .thenReturn(decisionWithAdditionalApproval);

        // When
        checkPolicyDelegate.execute(execution);

        // Then
        verify(execution).setVariable("policyApproved", true);
        verify(execution).setVariable("policyReason", "Approved with conditions");
        verify(execution).setVariable("requiresAdditionalApproval", true);
    }

    @Test
    void testExecute_NullPayload() throws Exception {
        // Given
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
        when(execution.getVariable("changeType")).thenReturn("UPDATE");
        when(execution.getVariable("requestor")).thenReturn("test-user");
        when(execution.getVariable("payload")).thenReturn(null);
        
        PolicyDecision approvedDecision = new PolicyDecision(true, "Approved", false);
        when(opaPolicyService.evaluatePolicy(eq("change_request_approval"), any(Map.class)))
                .thenReturn(approvedDecision);

        // When
        checkPolicyDelegate.execute(execution);

        // Then
        verify(execution).setVariable("policyApproved", true);
        verify(opaPolicyService).evaluatePolicy(eq("change_request_approval"), argThat(input ->
                input.get("payload") == null
        ));
    }

    @Test
    void testExecute_EmptyPayload() throws Exception {
        // Given
        when(execution.getVariable("datasetType")).thenReturn("CARRIER");
        when(execution.getVariable("changeType")).thenReturn("UPDATE");
        when(execution.getVariable("requestor")).thenReturn("admin");
        when(execution.getVariable("payload")).thenReturn(new HashMap<>());
        
        PolicyDecision approvedDecision = new PolicyDecision(true, "Approved", false);
        when(opaPolicyService.evaluatePolicy(eq("change_request_approval"), any(Map.class)))
                .thenReturn(approvedDecision);

        // When
        checkPolicyDelegate.execute(execution);

        // Then
        verify(execution).setVariable("policyApproved", true);
        verify(opaPolicyService).evaluatePolicy(eq("change_request_approval"), argThat(input ->
                ((Map<String, Object>) input.get("payload")).isEmpty()
        ));
    }

    @Test
    void testExecute_PolicyServiceException() throws Exception {
        // Given
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
        when(execution.getVariable("changeType")).thenReturn("UPDATE");
        when(execution.getVariable("requestor")).thenReturn("test-user");
        when(execution.getVariable("payload")).thenReturn(payload);
        
        when(opaPolicyService.evaluatePolicy(anyString(), any(Map.class)))
                .thenThrow(new RuntimeException("Policy service unavailable"));

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            checkPolicyDelegate.execute(execution);
        });
        
        assertEquals("Policy service unavailable", exception.getMessage());
        verify(execution, never()).setVariable(eq("policyApproved"), anyBoolean());
    }

    @Test
    void testExecute_VerifyPolicyInputMapping() throws Exception {
        // Given
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
        when(execution.getVariable("changeType")).thenReturn("CREATE");
        when(execution.getVariable("requestor")).thenReturn("test-user");
        when(execution.getVariable("payload")).thenReturn(payload);
        
        PolicyDecision approvedDecision = new PolicyDecision(true, "Approved", false);
        when(opaPolicyService.evaluatePolicy(eq("change_request_approval"), any(Map.class)))
                .thenReturn(approvedDecision);

        // When
        checkPolicyDelegate.execute(execution);

        // Then
        verify(opaPolicyService).evaluatePolicy(eq("change_request_approval"), argThat(input -> {
            Map<String, Object> policyInput = (Map<String, Object>) input;
            return "COUNTRY".equals(policyInput.get("datasetType")) &&
                   "CREATE".equals(policyInput.get("changeType")) &&
                   "test-user".equals(policyInput.get("requestor")) &&
                   payload.equals(policyInput.get("payload"));
        }));
    }

    @Test
    void testExecute_AllDatasetTypes() throws Exception {
        PolicyDecision approvedDecision = new PolicyDecision(true, "Approved", false);
        when(opaPolicyService.evaluatePolicy(eq("change_request_approval"), any(Map.class)))
                .thenReturn(approvedDecision);
        
        String[] datasetTypes = {"COUNTRY", "PORT", "AIRPORT", "CARRIER"};
        
        for (String datasetType : datasetTypes) {
            // Given
            when(execution.getVariable("datasetType")).thenReturn(datasetType);
            when(execution.getVariable("changeType")).thenReturn("UPDATE");
            when(execution.getVariable("requestor")).thenReturn("test-user");
            when(execution.getVariable("payload")).thenReturn(payload);
            
            // When
            checkPolicyDelegate.execute(execution);
            
            // Then
            verify(execution, atLeastOnce()).setVariable("policyApproved", true);
        }
    }
}