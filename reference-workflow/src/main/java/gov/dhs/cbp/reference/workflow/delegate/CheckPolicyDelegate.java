package gov.dhs.cbp.reference.workflow.delegate;

import gov.dhs.cbp.reference.workflow.service.OpaPolicyService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("checkPolicyDelegate")
public class CheckPolicyDelegate implements JavaDelegate {
    
    private static final Logger log = LoggerFactory.getLogger(CheckPolicyDelegate.class);
    
    private final OpaPolicyService opaPolicyService;
    
    public CheckPolicyDelegate(OpaPolicyService opaPolicyService) {
        this.opaPolicyService = opaPolicyService;
    }
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String datasetType = (String) execution.getVariable("datasetType");
        String changeType = (String) execution.getVariable("changeType");
        String requestor = (String) execution.getVariable("requestor");
        Map<String, Object> payload = (Map<String, Object>) execution.getVariable("payload");
        
        log.info("Checking policy for dataset: {}, changeType: {}, requestor: {}", 
                 datasetType, changeType, requestor);
        
        Map<String, Object> policyInput = new HashMap<>();
        policyInput.put("datasetType", datasetType);
        policyInput.put("changeType", changeType);
        policyInput.put("requestor", requestor);
        policyInput.put("payload", payload);
        
        OpaPolicyService.PolicyDecision decision = opaPolicyService.evaluatePolicy(
            "change_request_approval",
            policyInput
        );
        
        execution.setVariable("policyApproved", decision.isAllowed());
        execution.setVariable("policyReason", decision.getReason());
        execution.setVariable("requiresAdditionalApproval", decision.isRequiresAdditionalApproval());
        
        if (decision.isAllowed()) {
            log.info("Policy approved for change request");
        } else {
            log.warn("Policy rejected for change request. Reason: {}", decision.getReason());
        }
    }
}