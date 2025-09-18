package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.dto.ChangeRequestDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.service.ChangeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/change-requests")
@Tag(name = "Change Requests", description = "Change request management operations")
public class ChangeRequestsController {
    
    private final ChangeRequestService changeRequestService;
    
    public ChangeRequestsController(ChangeRequestService changeRequestService) {
        this.changeRequestService = changeRequestService;
    }
    
    @GetMapping
    @Operation(summary = "Get change requests with pagination",
               description = "Retrieve paginated list of change requests with optional filtering")
    @ApiResponse(responseCode = "200", description = "Change requests retrieved successfully")
    public ResponseEntity<PagedResponse<ChangeRequestDto>> getChangeRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String requestor,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            HttpServletRequest request) {
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        if (fromDate == null) {
            fromDate = LocalDateTime.now().minusDays(30); // Default to last 30 days
        }
        
        PagedResponse<ChangeRequestDto> response = changeRequestService.findByFilters(
                status, requestor, entityType, changeType, fromDate, pageable);
        
        // Add pagination links
        String baseUrl = getBaseUrl(request);
        response.setSelfLink(buildPageLink(baseUrl, status, requestor, entityType, changeType, fromDate, page, size, sortBy, sortDirection));
        
        if (!response.isLast()) {
            response.setNextLink(buildPageLink(baseUrl, status, requestor, entityType, changeType, fromDate, page + 1, size, sortBy, sortDirection));
        }
        if (!response.isFirst()) {
            response.setPrevLink(buildPageLink(baseUrl, status, requestor, entityType, changeType, fromDate, page - 1, size, sortBy, sortDirection));
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get change request by ID",
               description = "Retrieve a specific change request by its unique identifier")
    @ApiResponse(responseCode = "200", description = "Change request found")
    @ApiResponse(responseCode = "404", description = "Change request not found")
    public ResponseEntity<ChangeRequestDto> getChangeRequestById(@PathVariable UUID id) {
        return changeRequestService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @Operation(summary = "Submit a new change request",
               description = "Create a new change request for review")
    @ApiResponse(responseCode = "201", description = "Change request created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid change request data")
    public ResponseEntity<ChangeRequestDto> createChangeRequest(@Valid @RequestBody ChangeRequestDto changeRequestDto) {
        ChangeRequestDto created = changeRequestService.create(changeRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update a change request",
               description = "Update an existing change request (only if pending)")
    @ApiResponse(responseCode = "200", description = "Change request updated successfully")
    @ApiResponse(responseCode = "404", description = "Change request not found")
    @ApiResponse(responseCode = "409", description = "Change request cannot be updated in current status")
    public ResponseEntity<ChangeRequestDto> updateChangeRequest(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRequestDto changeRequestDto) {
        
        return changeRequestService.update(id, changeRequestDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a change request",
               description = "Approve a pending change request (requires APPROVER role)")
    @ApiResponse(responseCode = "200", description = "Change request approved successfully")
    @ApiResponse(responseCode = "404", description = "Change request not found")
    @ApiResponse(responseCode = "409", description = "Change request cannot be approved in current status")
    public ResponseEntity<ChangeRequestDto> approveChangeRequest(
            @PathVariable UUID id,
            @RequestParam(required = false) String comments) {
        
        return changeRequestService.approve(id, comments)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a change request",
               description = "Reject a pending change request (requires APPROVER role)")
    @ApiResponse(responseCode = "200", description = "Change request rejected successfully")
    @ApiResponse(responseCode = "404", description = "Change request not found")
    @ApiResponse(responseCode = "409", description = "Change request cannot be rejected in current status")
    public ResponseEntity<ChangeRequestDto> rejectChangeRequest(
            @PathVariable UUID id,
            @RequestParam String reason) {
        
        return changeRequestService.reject(id, reason)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a change request",
               description = "Cancel a change request (requestor or admin only)")
    @ApiResponse(responseCode = "200", description = "Change request cancelled successfully")
    @ApiResponse(responseCode = "404", description = "Change request not found")
    @ApiResponse(responseCode = "409", description = "Change request cannot be cancelled in current status")
    public ResponseEntity<ChangeRequestDto> cancelChangeRequest(@PathVariable UUID id) {
        return changeRequestService.cancel(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/pending")
    @Operation(summary = "Get pending change requests",
               description = "Retrieve all pending change requests for review")
    @ApiResponse(responseCode = "200", description = "Pending change requests retrieved successfully")
    public ResponseEntity<List<ChangeRequestDto>> getPendingChangeRequests() {
        List<ChangeRequestDto> pending = changeRequestService.findPending();
        return ResponseEntity.ok(pending);
    }
    
    @GetMapping("/high-priority")
    @Operation(summary = "Get high priority pending requests",
               description = "Retrieve high priority change requests that need immediate attention")
    @ApiResponse(responseCode = "200", description = "High priority requests retrieved successfully")
    public ResponseEntity<List<ChangeRequestDto>> getHighPriorityRequests() {
        List<ChangeRequestDto> highPriority = changeRequestService.findHighPriorityPending();
        return ResponseEntity.ok(highPriority);
    }
    
    @GetMapping("/my-requests")
    @Operation(summary = "Get current user's change requests",
               description = "Retrieve change requests submitted by the current user")
    @ApiResponse(responseCode = "200", description = "User's change requests retrieved successfully")
    public ResponseEntity<PagedResponse<ChangeRequestDto>> getMyChangeRequests(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            HttpServletRequest request) {
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        // TODO: Get current user from security context
        String currentUser = request.getRemoteUser(); // Placeholder
        
        PagedResponse<ChangeRequestDto> response = changeRequestService.findByRequestor(currentUser, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Get change request statistics",
               description = "Retrieve statistics about change requests")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = changeRequestService.getStatistics();
        return ResponseEntity.ok(stats);
    }
    
    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        
        if ((scheme.equals("http") && serverPort != 80) || 
            (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        
        url.append(contextPath).append("/v1/change-requests");
        return url.toString();
    }
    
    private String buildPageLink(String baseUrl, String status, String requestor, String entityType,
                                String changeType, LocalDateTime fromDate, int page, int size,
                                String sortBy, String sortDirection) {
        StringBuilder link = new StringBuilder(baseUrl);
        link.append("?page=").append(page)
            .append("&size=").append(size)
            .append("&sortBy=").append(sortBy)
            .append("&sortDirection=").append(sortDirection);
        
        if (status != null) link.append("&status=").append(status);
        if (requestor != null) link.append("&requestor=").append(requestor);
        if (entityType != null) link.append("&entityType=").append(entityType);
        if (changeType != null) link.append("&changeType=").append(changeType);
        if (fromDate != null) link.append("&fromDate=").append(fromDate);
        
        return link.toString();
    }
}