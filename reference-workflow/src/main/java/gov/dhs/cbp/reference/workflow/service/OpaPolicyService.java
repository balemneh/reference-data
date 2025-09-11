package gov.dhs.cbp.reference.workflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpaPolicyService {
    
    private static final Logger log = LoggerFactory.getLogger(OpaPolicyService.class);
    
    @Value("${opa.url:http://localhost:8181}")
    private String opaUrl;
    
    private final RestTemplate restTemplate;
    
    public OpaPolicyService() {
        this.restTemplate = new RestTemplate();
    }
    
    public PolicyDecision evaluatePolicy(String policyName, Map<String, Object> input) {
        String url = String.format("%s/v1/data/%s", opaUrl, policyName);
        
        Map<String, Object> request = new HashMap<>();
        request.put("input", input);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        
        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            
            if (response != null && response.containsKey("result")) {
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                
                boolean allowed = (boolean) result.getOrDefault("allow", false);
                String reason = (String) result.getOrDefault("reason", "No reason provided");
                boolean requiresAdditionalApproval = (boolean) result.getOrDefault("requiresAdditionalApproval", false);
                
                return new PolicyDecision(allowed, reason, requiresAdditionalApproval);
            }
        } catch (Exception e) {
            log.error("Error evaluating OPA policy: {}", e.getMessage(), e);
            // In case of error, default to requiring manual approval
            return new PolicyDecision(false, "Policy evaluation error: " + e.getMessage(), true);
        }
        
        return new PolicyDecision(false, "Policy evaluation failed", true);
    }
    
    public static class PolicyDecision {
        private final boolean allowed;
        private final String reason;
        private final boolean requiresAdditionalApproval;
        
        public PolicyDecision(boolean allowed, String reason, boolean requiresAdditionalApproval) {
            this.allowed = allowed;
            this.reason = reason;
            this.requiresAdditionalApproval = requiresAdditionalApproval;
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public String getReason() {
            return reason;
        }
        
        public boolean isRequiresAdditionalApproval() {
            return requiresAdditionalApproval;
        }
    }
}