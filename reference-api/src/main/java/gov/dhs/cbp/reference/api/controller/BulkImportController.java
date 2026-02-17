package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.service.BulkImportService;
import gov.dhs.cbp.reference.api.service.AsyncBulkImportService;
import gov.dhs.cbp.reference.core.entity.BulkImportBatch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bulk-import")
@Tag(name = "Bulk Import", description = "Bulk data import operations")
@CrossOrigin(origins = "*")
public class BulkImportController {

    private static final Logger logger = LoggerFactory.getLogger(BulkImportController.class);

    private final BulkImportService bulkImportService;
    private final AsyncBulkImportService asyncBulkImportService;

    public BulkImportController(BulkImportService bulkImportService, AsyncBulkImportService asyncBulkImportService) {
        this.bulkImportService = bulkImportService;
        this.asyncBulkImportService = asyncBulkImportService;
    }

    @PostMapping(value = "/initiate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Initiate bulk import",
        description = "Uploads a file and initiates the bulk import process. Supports CSV and JSON formats."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Bulk import initiated successfully",
                    content = @Content(schema = @Schema(implementation = BulkImportInitiateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file format or duplicate file"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BulkImportInitiateResponse> initiateBulkImport(
            @Parameter(description = "File to import (CSV or JSON)", required = true)
            @RequestParam("file") @NotNull MultipartFile file,

            @Parameter(description = "User ID initiating the import", required = true)
            @RequestParam("userId") @NotNull String userId,

            @Parameter(description = "Type of data being imported", required = true)
            @RequestParam("dataType") @NotNull BulkImportBatch.DataType dataType,

            @Parameter(description = "Source system name", required = true)
            @RequestParam("sourceSystem") @NotNull String sourceSystem,

            @Parameter(description = "Optional description of the import")
            @RequestParam(value = "description", required = false) String description) {

        try {
            logger.info("Initiating bulk import for user {} with file {} for data type {}",
                       userId, file.getOriginalFilename(), dataType);

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new BulkImportInitiateResponse(null, "File is empty", false));
            }

            // Validate file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(new BulkImportInitiateResponse(null, "File size exceeds 10MB limit", false));
            }

            // Validate file type
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.toLowerCase().endsWith(".csv") && !filename.toLowerCase().endsWith(".json"))) {
                return ResponseEntity.badRequest()
                        .body(new BulkImportInitiateResponse(null, "Only CSV and JSON files are supported", false));
            }

            java.util.Map<String, UUID> ids = bulkImportService.initiateBulkImport(file, userId, dataType, sourceSystem, description);
            UUID batchId = ids.get("batchId");
            UUID changeRequestId = ids.get("changeRequestId");

            BulkImportInitiateResponse response = new BulkImportInitiateResponse(
                batchId,
                "Bulk import initiated successfully",
                true
            );
            response.setChangeRequestId(changeRequestId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for bulk import: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new BulkImportInitiateResponse(null, e.getMessage(), false));
        } catch (Exception e) {
            logger.error("Failed to initiate bulk import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BulkImportInitiateResponse(null, "Internal server error: " + e.getMessage(), false));
        }
    }

    @GetMapping("/status/{batchId}")
    @Operation(
        summary = "Get bulk import status",
        description = "Returns the current status and progress of the specified bulk import batch"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Batch not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BulkImportService.BulkImportStatus> getBulkImportStatus(
            @Parameter(description = "Batch ID to check status", required = true)
            @PathVariable UUID batchId) {

        try {
            BulkImportService.BulkImportStatus status = bulkImportService.getBulkImportStatus(batchId);
            return ResponseEntity.ok(status);

        } catch (IllegalArgumentException e) {
            logger.warn("Batch not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to get status for batch {}", batchId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/rollback/{batchId}")
    @Operation(
        summary = "Rollback bulk import",
        description = "Rolls back a failed or in-progress bulk import batch"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rollback completed successfully"),
        @ApiResponse(responseCode = "404", description = "Batch not found"),
        @ApiResponse(responseCode = "400", description = "Cannot rollback completed batch"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BulkImportRollbackResponse> rollbackBulkImport(
            @Parameter(description = "Batch ID to rollback", required = true)
            @PathVariable UUID batchId) {

        try {
            logger.info("Starting rollback for batch {}", batchId);

            boolean success = bulkImportService.rollbackBulkImport(batchId);

            BulkImportRollbackResponse response = new BulkImportRollbackResponse(
                batchId,
                success ? "Rollback completed successfully" : "Rollback failed",
                success
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid batch ID for rollback: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Invalid batch status for rollback: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new BulkImportRollbackResponse(batchId, e.getMessage(), false));
        } catch (Exception e) {
            logger.error("Failed to rollback batch {}", batchId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BulkImportRollbackResponse(batchId, "Internal server error: " + e.getMessage(), false));
        }
    }

    @GetMapping("/download/{batchId}")
    @Operation(
        summary = "Download imported file",
        description = "Downloads the original file for the specified bulk import batch"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "404", description = "Batch or file not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Resource> downloadImportedFile(
            @Parameter(description = "Batch ID to download file for", required = true)
            @PathVariable UUID batchId) {

        try {
            Resource resource = bulkImportService.getImportedFile(batchId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (IllegalArgumentException e) {
            logger.warn("File not found for batch {}: {}", batchId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to download file for batch {}", batchId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    // Response DTOs
    public static class BulkImportInitiateResponse {
        private UUID batchId;
        private UUID changeRequestId;
        private String message;
        private boolean success;

        public BulkImportInitiateResponse(UUID batchId, String message, boolean success) {
            this.batchId = batchId;
            this.message = message;
            this.success = success;
        }

        public UUID getBatchId() { return batchId; }
        public void setBatchId(UUID batchId) { this.batchId = batchId; }
        public UUID getChangeRequestId() { return changeRequestId; }
        public void setChangeRequestId(UUID changeRequestId) { this.changeRequestId = changeRequestId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }

    public static class BulkImportRollbackResponse {
        private UUID batchId;
        private String message;
        private boolean success;

        public BulkImportRollbackResponse(UUID batchId, String message, boolean success) {
            this.batchId = batchId;
            this.message = message;
            this.success = success;
        }

        public UUID getBatchId() { return batchId; }
        public void setBatchId(UUID batchId) { this.batchId = batchId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
}