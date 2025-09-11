package gov.dhs.cbp.reference.workflow.service;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ChangeRequestService {
    
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    
    public ChangeRequestService(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }
    
    public String startChangeRequest(ChangeRequestDto request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("requestId", request.getRequestId());
        variables.put("datasetType", request.getDatasetType());
        variables.put("changeType", request.getChangeType());
        variables.put("requestor", request.getRequestor());
        variables.put("description", request.getDescription());
        variables.put("payload", request.getPayload());
        variables.put("urgency", request.getUrgency());
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
            "changeRequestApproval", 
            request.getRequestId().toString(),
            variables
        );
        
        return processInstance.getId();
    }
    
    public List<Task> getTasksForUser(String userId) {
        return taskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime().desc()
                .list();
    }
    
    public List<Task> getTasksForGroup(String groupId) {
        return taskService.createTaskQuery()
                .taskCandidateGroup(groupId)
                .orderByTaskCreateTime().desc()
                .list();
    }
    
    public void completeTask(String taskId, Map<String, Object> variables) {
        taskService.complete(taskId, variables);
    }
    
    public void claimTask(String taskId, String userId) {
        taskService.claim(taskId, userId);
    }
    
    public void delegateTask(String taskId, String userId) {
        taskService.delegateTask(taskId, userId);
    }
    
    public long getPendingRequestCount() {
        // For now, return count of pending tasks - in production this would query a proper change request table
        return taskService.createTaskQuery().count();
    }
    
    public static class ChangeRequestDto {
        private UUID requestId;
        private String datasetType;
        private String changeType;
        private String requestor;
        private String description;
        private Map<String, Object> payload;
        private String urgency;
        
        // Getters and setters
        public UUID getRequestId() {
            return requestId;
        }
        
        public void setRequestId(UUID requestId) {
            this.requestId = requestId;
        }
        
        public String getDatasetType() {
            return datasetType;
        }
        
        public void setDatasetType(String datasetType) {
            this.datasetType = datasetType;
        }
        
        public String getChangeType() {
            return changeType;
        }
        
        public void setChangeType(String changeType) {
            this.changeType = changeType;
        }
        
        public String getRequestor() {
            return requestor;
        }
        
        public void setRequestor(String requestor) {
            this.requestor = requestor;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public Map<String, Object> getPayload() {
            return payload;
        }
        
        public void setPayload(Map<String, Object> payload) {
            this.payload = payload;
        }
        
        public String getUrgency() {
            return urgency;
        }
        
        public void setUrgency(String urgency) {
            this.urgency = urgency;
        }
    }
}