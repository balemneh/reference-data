package gov.dhs.cbp.reference.workflow.controller;

import gov.dhs.cbp.reference.workflow.service.ChangeRequestService;
import gov.dhs.cbp.reference.workflow.service.ChangeRequestService.ChangeRequestDto;
import org.camunda.bpm.engine.task.Task;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/workflow")
public class WorkflowController {
    
    private final ChangeRequestService changeRequestService;
    
    public WorkflowController(ChangeRequestService changeRequestService) {
        this.changeRequestService = changeRequestService;
    }
    
    @PostMapping("/change-requests")
    public ResponseEntity<Map<String, Object>> submitChangeRequest(@RequestBody ChangeRequestDto request) {
        if (request.getRequestId() == null) {
            request.setRequestId(UUID.randomUUID());
        }
        
        String processInstanceId = changeRequestService.startChangeRequest(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", request.getRequestId());
        response.put("processInstanceId", processInstanceId);
        response.put("status", "SUBMITTED");
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/tasks/user/{userId}")
    public ResponseEntity<List<Task>> getUserTasks(@PathVariable String userId) {
        List<Task> tasks = changeRequestService.getTasksForUser(userId);
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/tasks/group/{groupId}")
    public ResponseEntity<List<Task>> getGroupTasks(@PathVariable String groupId) {
        List<Task> tasks = changeRequestService.getTasksForGroup(groupId);
        return ResponseEntity.ok(tasks);
    }
    
    @PostMapping("/tasks/{taskId}/claim")
    public ResponseEntity<Void> claimTask(@PathVariable String taskId, 
                                          @RequestParam String userId) {
        changeRequestService.claimTask(taskId, userId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Void> completeTask(@PathVariable String taskId,
                                            @RequestBody Map<String, Object> variables) {
        changeRequestService.completeTask(taskId, variables);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/tasks/{taskId}/delegate")
    public ResponseEntity<Void> delegateTask(@PathVariable String taskId,
                                            @RequestParam String userId) {
        changeRequestService.delegateTask(taskId, userId);
        return ResponseEntity.noContent().build();
    }
}