package gov.dhs.cbp.reference.workflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("notificationDelegate")
public class NotificationDelegate implements JavaDelegate {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationDelegate.class);
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        UUID requestId = (UUID) execution.getVariable("requestId");
        String requestor = (String) execution.getVariable("requestor");
        String datasetType = (String) execution.getVariable("datasetType");
        Boolean changeApplied = (Boolean) execution.getVariable("changeApplied");
        Boolean dataValid = (Boolean) execution.getVariable("dataValid");
        Boolean policyApproved = (Boolean) execution.getVariable("policyApproved");
        Boolean approved = (Boolean) execution.getVariable("approved");
        
        String status;
        String message;
        
        if (Boolean.FALSE.equals(dataValid)) {
            status = "VALIDATION_FAILED";
            message = "Change request failed data validation: " + execution.getVariable("validationErrors");
        } else if (Boolean.FALSE.equals(policyApproved) && !Boolean.TRUE.equals(approved)) {
            status = "POLICY_REJECTED";
            message = "Change request rejected by policy: " + execution.getVariable("policyReason");
        } else if (Boolean.FALSE.equals(approved)) {
            status = "MANUALLY_REJECTED";
            message = "Change request rejected by reviewer: " + execution.getVariable("comments");
        } else if (Boolean.TRUE.equals(changeApplied)) {
            status = "APPLIED";
            message = "Change request successfully applied";
        } else {
            status = "FAILED";
            message = "Change request failed to apply: " + execution.getVariable("changeError");
        }
        
        log.info("Sending notification for request: {} to: {} with status: {}", 
                 requestId, requestor, status);
        
        // Here we would send actual notifications via email, messaging, etc.
        // For now, we'll just log the notification
        
        sendEmail(requestor, requestId, datasetType, status, message);
        publishEvent(requestId, datasetType, status, message);
        
        execution.setVariable("notificationSent", true);
        execution.setVariable("finalStatus", status);
    }
    
    private void sendEmail(String recipient, UUID requestId, String datasetType, 
                          String status, String message) {
        log.info("EMAIL NOTIFICATION:");
        log.info("  To: {}", recipient);
        log.info("  Subject: Change Request {} - {}", requestId, status);
        log.info("  Body: Your change request for {} dataset has status: {}. {}", 
                 datasetType, status, message);
    }
    
    private void publishEvent(UUID requestId, String datasetType, String status, String message) {
        log.info("EVENT NOTIFICATION:");
        log.info("  Topic: change-request-status");
        log.info("  RequestId: {}", requestId);
        log.info("  DatasetType: {}", datasetType);
        log.info("  Status: {}", status);
        log.info("  Message: {}", message);
    }
}