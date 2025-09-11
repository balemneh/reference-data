package gov.dhs.cbp.reference.workflow.controller;

import gov.dhs.cbp.reference.workflow.service.ChangeRequestService;
import gov.dhs.cbp.reference.workflow.service.ChangeRequestService.ChangeRequestDto;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowControllerTest {

    @Mock
    private ChangeRequestService changeRequestService;

    @Mock
    private Task task;

    @InjectMocks
    private WorkflowController workflowController;

    private ChangeRequestDto changeRequest;

    @BeforeEach
    void setUp() {
        changeRequest = new ChangeRequestDto();
        changeRequest.setDatasetType("Country");
        changeRequest.setChangeType("UPDATE");
        changeRequest.setRequestor("test-user");
        changeRequest.setDescription("Test change request");
        changeRequest.setUrgency("NORMAL");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("countryCode", "US");
        payload.put("countryName", "United States");
        changeRequest.setPayload(payload);
    }

    @Test
    void testSubmitChangeRequest_Success() {
        // Given
        String processInstanceId = "process-123";
        when(changeRequestService.startChangeRequest(any(ChangeRequestDto.class)))
                .thenReturn(processInstanceId);

        // When
        ResponseEntity<Map<String, Object>> response = workflowController.submitChangeRequest(changeRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(processInstanceId, response.getBody().get("processInstanceId"));
        assertEquals("SUBMITTED", response.getBody().get("status"));
        assertNotNull(response.getBody().get("requestId"));
        verify(changeRequestService).startChangeRequest(any(ChangeRequestDto.class));
    }

    @Test
    void testSubmitChangeRequest_WithExistingRequestId() {
        // Given
        UUID requestId = UUID.randomUUID();
        changeRequest.setRequestId(requestId);
        String processInstanceId = "process-123";
        when(changeRequestService.startChangeRequest(any(ChangeRequestDto.class)))
                .thenReturn(processInstanceId);

        // When
        ResponseEntity<Map<String, Object>> response = workflowController.submitChangeRequest(changeRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(requestId, response.getBody().get("requestId"));
        assertEquals(processInstanceId, response.getBody().get("processInstanceId"));
    }

    @Test
    void testSubmitChangeRequest_WithException() {
        // Given
        when(changeRequestService.startChangeRequest(any(ChangeRequestDto.class)))
                .thenThrow(new RuntimeException("Process engine error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            workflowController.submitChangeRequest(changeRequest);
        });
    }

    @Test
    void testGetUserTasks_WithTasks() {
        // Given
        String userId = "test-user";
        List<Task> tasks = new ArrayList<>();
        when(task.getId()).thenReturn("task-1");
        when(task.getName()).thenReturn("Review Change Request");
        tasks.add(task);
        
        when(changeRequestService.getTasksForUser(userId)).thenReturn(tasks);

        // When
        ResponseEntity<List<Task>> response = workflowController.getUserTasks(userId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("task-1", response.getBody().get(0).getId());
        assertEquals("Review Change Request", response.getBody().get(0).getName());
    }

    @Test
    void testGetUserTasks_NoTasks() {
        // Given
        String userId = "test-user";
        when(changeRequestService.getTasksForUser(userId)).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<Task>> response = workflowController.getUserTasks(userId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testGetGroupTasks_WithTasks() {
        // Given
        String groupId = "reviewers";
        List<Task> tasks = new ArrayList<>();
        tasks.add(task);
        
        when(changeRequestService.getTasksForGroup(groupId)).thenReturn(tasks);

        // When
        ResponseEntity<List<Task>> response = workflowController.getGroupTasks(groupId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetGroupTasks_NoTasks() {
        // Given
        String groupId = "reviewers";
        when(changeRequestService.getTasksForGroup(groupId)).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<List<Task>> response = workflowController.getGroupTasks(groupId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testClaimTask_Success() {
        // Given
        String taskId = "task-123";
        String userId = "test-user";

        // When
        ResponseEntity<Void> response = workflowController.claimTask(taskId, userId);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(changeRequestService).claimTask(taskId, userId);
    }

    @Test
    void testCompleteTask_Approved() {
        // Given
        String taskId = "task-123";
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("comments", "Looks good");

        // When
        ResponseEntity<Void> response = workflowController.completeTask(taskId, variables);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(changeRequestService).completeTask(taskId, variables);
    }

    @Test
    void testCompleteTask_Rejected() {
        // Given
        String taskId = "task-123";
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", false);
        variables.put("comments", "Missing information");

        // When
        ResponseEntity<Void> response = workflowController.completeTask(taskId, variables);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(changeRequestService).completeTask(taskId, variables);
    }

    @Test
    void testCompleteTask_WithException() {
        // Given
        String taskId = "task-123";
        Map<String, Object> variables = new HashMap<>();
        doThrow(new RuntimeException("Task not found")).when(changeRequestService).completeTask(anyString(), any(Map.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            workflowController.completeTask(taskId, variables);
        });
    }

    @Test
    void testDelegateTask_Success() {
        // Given
        String taskId = "task-123";
        String userId = "delegate-user";

        // When
        ResponseEntity<Void> response = workflowController.delegateTask(taskId, userId);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(changeRequestService).delegateTask(taskId, userId);
    }
}