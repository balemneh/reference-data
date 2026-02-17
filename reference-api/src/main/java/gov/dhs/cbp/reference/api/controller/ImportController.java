package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.service.BulkImportService;
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

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/import")
@Tag(name = "Single Import", description = "Single data import operations")
@CrossOrigin(origins = "*")
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);

    private final BulkImportService bulkImportService;

    public ImportController(BulkImportService bulkImportService) {
        this.bulkImportService = bulkImportService;
    }

    @PostMapping
    @Operation(
        summary = "Import records",
        description = "Imports a list of records for a specified entity type"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Records imported successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request payload"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<UUID>> importRecords(@RequestBody ImportRecordsPayload payload) {
        logger.info("Received request to import records for entity type: {}", payload.getEntityType());
        List<UUID> changeRequestIds = bulkImportService.importRecords(payload.getRecords(), payload.getEntityType());
        return ResponseEntity.status(HttpStatus.CREATED).body(changeRequestIds);
    }

    public static class ImportRecordsPayload {
        private String entityType;
        private java.util.List<java.util.Map<String, String>> records;

        public String getEntityType() {
            return entityType;
        }

        public void setEntityType(String entityType) {
            this.entityType = entityType;
        }

        public java.util.List<java.util.Map<String, String>> getRecords() {
            return records;
        }

        public void setRecords(java.util.List<java.util.Map<String, String>> records) {
            this.records = records;
        }
    }
}