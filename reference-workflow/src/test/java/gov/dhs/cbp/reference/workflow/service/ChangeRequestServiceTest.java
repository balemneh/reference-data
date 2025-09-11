package gov.dhs.cbp.reference.workflow.service;

import gov.dhs.cbp.reference.workflow.service.ChangeRequestService.ChangeRequestDto;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeRequestServiceTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private TaskService taskService;

    @Mock
    private ProcessInstance processInstance;

    @Mock
    private TaskQuery taskQuery;

    @Mock
    private Task task;

    @InjectMocks
    private ChangeRequestService changeRequestService;

    private ChangeRequestDto changeRequest;

    @BeforeEach
    void setUp() {
        changeRequest = new ChangeRequestDto();
        changeRequest.setRequestId(UUID.randomUUID());
        changeRequest.setDatasetType("Country");
        changeRequest.setChangeType("UPDATE");
        changeRequest.setRequestor("test-user");
        changeRequest.setDescription("Test change request");
        changeRequest.setUrgency("NORMAL");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("countryCode", "US");
        payload.put("countryName", "United States of America");
        changeRequest.setPayload(payload);
    }

    @Test
    void testStartChangeRequest_Success() {
        // Given
        String processInstanceId = "process-123";
        when(runtimeService.startProcessInstanceByKey(
                eq("changeRequestApproval"), 
                eq(changeRequest.getRequestId().toString()),
                any(Map.class)))
                .thenReturn(processInstance);
        when(processInstance.getId()).thenReturn(processInstanceId);

        // When
        String result = changeRequestService.startChangeRequest(changeRequest);

        // Then
        assertEquals(processInstanceId, result);
        verify(runtimeService).startProcessInstanceByKey(
                eq("changeRequestApproval"),
                eq(changeRequest.getRequestId().toString()),
                any(Map.class));
    }

    @Test
    void testStartChangeRequest_WithAllFields() {
        // Given
        String processInstanceId = "process-456";
        when(runtimeService.startProcessInstanceByKey(
                anyString(), 
                anyString(),
                any(Map.class)))
                .thenReturn(processInstance);
        when(processInstance.getId()).thenReturn(processInstanceId);

        // When
        String result = changeRequestService.startChangeRequest(changeRequest);

        // Then
        assertEquals(processInstanceId, result);
        verify(runtimeService).startProcessInstanceByKey(
                eq("changeRequestApproval"),
                eq(changeRequest.getRequestId().toString()),
                argThat((Map<String, Object> variables) -> {
                    return variables.get("requestId").equals(changeRequest.getRequestId()) &&
                           variables.get("datasetType").equals("Country") &&
                           variables.get("changeType").equals("UPDATE") &&
                           variables.get("requestor").equals("test-user") &&
                           variables.get("description").equals("Test change request") &&
                           variables.get("urgency").equals("NORMAL") &&
                           variables.get("payload") != null;
                }));
    }

    @Test
    void testStartChangeRequest_WithException() {
        // Given
        when(runtimeService.startProcessInstanceByKey(
                anyString(), 
                anyString(),
                any(Map.class)))
                .thenThrow(new RuntimeException("Process definition not found"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            changeRequestService.startChangeRequest(changeRequest);
        });
    }

    @Test
    void testGetTasksForUser_WithTasks() {
        // Given
        String userId = "test-user";
        List<Task> expectedTasks = new ArrayList<>();
        expectedTasks.add(task);
        
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskAssignee(userId)).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.desc()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(expectedTasks);

        // When
        List<Task> result = changeRequestService.getTasksForUser(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(task, result.get(0));
        verify(taskQuery).taskAssignee(userId);
    }

    @Test
    void testGetTasksForUser_NoTasks() {
        // Given
        String userId = "test-user";
        
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskAssignee(userId)).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.desc()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(new ArrayList<>());

        // When
        List<Task> result = changeRequestService.getTasksForUser(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTasksForGroup_WithTasks() {
        // Given
        String groupId = "reviewers";
        List<Task> expectedTasks = new ArrayList<>();
        expectedTasks.add(task);
        
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskCandidateGroup(groupId)).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.desc()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(expectedTasks);

        // When
        List<Task> result = changeRequestService.getTasksForGroup(groupId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(task, result.get(0));
        verify(taskQuery).taskCandidateGroup(groupId);
    }

    @Test
    void testGetTasksForGroup_NoTasks() {
        // Given
        String groupId = "reviewers";
        
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskCandidateGroup(groupId)).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.desc()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(new ArrayList<>());

        // When
        List<Task> result = changeRequestService.getTasksForGroup(groupId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCompleteTask_Success() {
        // Given
        String taskId = "task-123";
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("comments", "Looks good");

        // When
        changeRequestService.completeTask(taskId, variables);

        // Then
        verify(taskService).complete(taskId, variables);
    }

    @Test
    void testCompleteTask_WithException() {
        // Given
        String taskId = "task-123";
        Map<String, Object> variables = new HashMap<>();
        doThrow(new RuntimeException("Task not found"))
                .when(taskService).complete(anyString(), any(Map.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            changeRequestService.completeTask(taskId, variables);
        });
    }

    @Test
    void testClaimTask_Success() {
        // Given
        String taskId = "task-123";
        String userId = "test-user";

        // When
        changeRequestService.claimTask(taskId, userId);

        // Then
        verify(taskService).claim(taskId, userId);
    }

    @Test
    void testClaimTask_WithException() {
        // Given
        String taskId = "task-123";
        String userId = "test-user";
        doThrow(new RuntimeException("Task already claimed"))
                .when(taskService).claim(anyString(), anyString());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            changeRequestService.claimTask(taskId, userId);
        });
    }

    @Test
    void testDelegateTask_Success() {
        // Given
        String taskId = "task-123";
        String userId = "delegate-user";

        // When
        changeRequestService.delegateTask(taskId, userId);

        // Then
        verify(taskService).delegateTask(taskId, userId);
    }

    @Test
    void testDelegateTask_WithException() {
        // Given
        String taskId = "task-123";
        String userId = "delegate-user";
        doThrow(new RuntimeException("Cannot delegate task"))
                .when(taskService).delegateTask(anyString(), anyString());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            changeRequestService.delegateTask(taskId, userId);
        });
    }

    @Test
    void testChangeRequestDto_GettersAndSetters() {
        // Given
        ChangeRequestDto dto = new ChangeRequestDto();
        UUID requestId = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>();
        payload.put("test", "data");

        // When
        dto.setRequestId(requestId);
        dto.setDatasetType("Port");
        dto.setChangeType("CREATE");
        dto.setRequestor("admin");
        dto.setDescription("New port");
        dto.setPayload(payload);
        dto.setUrgency("HIGH");

        // Then
        assertEquals(requestId, dto.getRequestId());
        assertEquals("Port", dto.getDatasetType());
        assertEquals("CREATE", dto.getChangeType());
        assertEquals("admin", dto.getRequestor());
        assertEquals("New port", dto.getDescription());
        assertEquals(payload, dto.getPayload());
        assertEquals("HIGH", dto.getUrgency());
    }
}