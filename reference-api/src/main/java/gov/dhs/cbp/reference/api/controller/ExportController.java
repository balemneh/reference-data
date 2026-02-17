package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/export")
@Tag(name = "Export", description = "Export data operations")
@CrossOrigin(origins = "*")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping
    @Operation(summary = "Export data")
    public ResponseEntity<Resource> exportData(
            @Parameter(description = "Entity type to export", required = true)
            @RequestParam String entityType,
            @Parameter(description = "Format to export", required = true)
            @RequestParam String format) {

        try {
            Resource resource = exportService.exportData(entityType, format);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
