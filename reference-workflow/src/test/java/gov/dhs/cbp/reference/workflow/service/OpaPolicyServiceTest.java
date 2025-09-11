package gov.dhs.cbp.reference.workflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpaPolicyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private OpaPolicyService opaPolicyService;

    private Map<String, Object> changeRequest;

    @BeforeEach
    void setUp() {
        opaPolicyService = new OpaPolicyService();
        ReflectionTestUtils.setField(opaPolicyService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(opaPolicyService, "opaUrl", "http://localhost:8181");
        
        changeRequest = new HashMap<>();
        changeRequest.put("datasetType", "Country");
        changeRequest.put("changeType", "UPDATE");
        changeRequest.put("requestor", "test-user");
        changeRequest.put("urgency", "NORMAL");
        
        Map<String, Object> data = new HashMap<>();
        data.put("countryCode", "US");
        data.put("countryName", "United States");
        changeRequest.put("data", data);
    }

    @Test
    void testEvaluatePolicyApproved() {
        Map<String, Object> opaResponse = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        result.put("allow", true);
        result.put("reason", "All checks passed");
        result.put("requiresAdditionalApproval", false);
        opaResponse.put("result", result);
        
        when(restTemplate.postForObject(
                anyString(),
                any(),
                eq(Map.class)))
                .thenReturn(opaResponse);

        OpaPolicyService.PolicyDecision decision = 
                opaPolicyService.evaluatePolicy("change-request-approval", changeRequest);

        assertTrue(decision.isAllowed());
        assertEquals("All checks passed", decision.getReason());
        assertFalse(decision.isRequiresAdditionalApproval());
    }

    @Test
    void testEvaluatePolicyRejected() {
        Map<String, Object> opaResponse = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        result.put("allow", false);
        result.put("reason", "User lacks required permissions");
        result.put("requiresAdditionalApproval", true);
        opaResponse.put("result", result);
        
        when(restTemplate.postForObject(
                anyString(),
                any(),
                eq(Map.class)))
                .thenReturn(opaResponse);

        OpaPolicyService.PolicyDecision decision = 
                opaPolicyService.evaluatePolicy("change-request-approval", changeRequest);

        assertFalse(decision.isAllowed());
        assertEquals("User lacks required permissions", decision.getReason());
        assertTrue(decision.isRequiresAdditionalApproval());
    }

    @Test
    void testEvaluatePolicyWithException() {
        when(restTemplate.postForObject(
                anyString(),
                any(),
                eq(Map.class)))
                .thenThrow(new RestClientException("Connection timeout"));

        OpaPolicyService.PolicyDecision decision = 
                opaPolicyService.evaluatePolicy("change-request-approval", changeRequest);

        assertFalse(decision.isAllowed());
        assertTrue(decision.getReason().contains("Connection timeout"));
        assertTrue(decision.isRequiresAdditionalApproval());
    }

    @Test
    void testEvaluatePolicyWithNullResponse() {
        when(restTemplate.postForObject(
                anyString(),
                any(),
                eq(Map.class)))
                .thenReturn(null);

        OpaPolicyService.PolicyDecision decision = 
                opaPolicyService.evaluatePolicy("change-request-approval", changeRequest);

        assertFalse(decision.isAllowed());
        assertEquals("Policy evaluation failed", decision.getReason());
        assertTrue(decision.isRequiresAdditionalApproval());
    }

    @Test
    void testEvaluatePolicyWithMissingResult() {
        Map<String, Object> opaResponse = new HashMap<>();
        // No "result" key in response
        
        when(restTemplate.postForObject(
                anyString(),
                any(),
                eq(Map.class)))
                .thenReturn(opaResponse);

        OpaPolicyService.PolicyDecision decision = 
                opaPolicyService.evaluatePolicy("change-request-approval", changeRequest);

        assertFalse(decision.isAllowed());
        assertEquals("Policy evaluation failed", decision.getReason());
        assertTrue(decision.isRequiresAdditionalApproval());
    }

    @Test
    void testEvaluatePolicyWithDefaultValues() {
        Map<String, Object> opaResponse = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        // Only "allow" is set, other fields should use defaults
        result.put("allow", true);
        opaResponse.put("result", result);
        
        when(restTemplate.postForObject(
                anyString(),
                any(),
                eq(Map.class)))
                .thenReturn(opaResponse);

        OpaPolicyService.PolicyDecision decision = 
                opaPolicyService.evaluatePolicy("change-request-approval", changeRequest);

        assertTrue(decision.isAllowed());
        assertEquals("No reason provided", decision.getReason());
        assertFalse(decision.isRequiresAdditionalApproval());
    }

    @Test
    void testPolicyDecisionConstructor() {
        OpaPolicyService.PolicyDecision decision = 
                new OpaPolicyService.PolicyDecision(true, "Approved", false);
        
        assertTrue(decision.isAllowed());
        assertEquals("Approved", decision.getReason());
        assertFalse(decision.isRequiresAdditionalApproval());
    }

    @Test
    void testPolicyDecisionWithAdditionalApproval() {
        OpaPolicyService.PolicyDecision decision = 
                new OpaPolicyService.PolicyDecision(false, "Needs review", true);
        
        assertFalse(decision.isAllowed());
        assertEquals("Needs review", decision.getReason());
        assertTrue(decision.isRequiresAdditionalApproval());
    }

    @Test
    void testEvaluatePolicyUrlConstruction() {
        Map<String, Object> opaResponse = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        result.put("allow", true);
        opaResponse.put("result", result);
        
        when(restTemplate.postForObject(
                eq("http://localhost:8181/v1/data/custom-policy"),
                any(),
                eq(Map.class)))
                .thenReturn(opaResponse);

        OpaPolicyService.PolicyDecision decision = 
                opaPolicyService.evaluatePolicy("custom-policy", changeRequest);

        assertTrue(decision.isAllowed());
        verify(restTemplate).postForObject(
                eq("http://localhost:8181/v1/data/custom-policy"),
                any(),
                eq(Map.class));
    }

    @Test
    void testEvaluatePolicyWithComplexInput() {
        Map<String, Object> complexInput = new HashMap<>();
        complexInput.put("user", "admin");
        complexInput.put("action", "approve");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("timestamp", System.currentTimeMillis());
        metadata.put("source", "API");
        complexInput.put("metadata", metadata);
        
        Map<String, Object> opaResponse = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        result.put("allow", true);
        result.put("reason", "Admin approved");
        opaResponse.put("result", result);
        
        when(restTemplate.postForObject(
                anyString(),
                any(),
                eq(Map.class)))
                .thenReturn(opaResponse);

        OpaPolicyService.PolicyDecision decision = 
                opaPolicyService.evaluatePolicy("admin-policy", complexInput);

        assertTrue(decision.isAllowed());
        assertEquals("Admin approved", decision.getReason());
    }
}