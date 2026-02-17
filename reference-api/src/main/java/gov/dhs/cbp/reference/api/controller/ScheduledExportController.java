package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.dto.ScheduledExportDto;
import gov.dhs.cbp.reference.api.service.ScheduledExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scheduled-exports")
@Tag(name = "Scheduled Exports", description = "Operations for managing scheduled exports")
@CrossOrigin(origins = "*")
public class ScheduledExportController {

    private final ScheduledExportService scheduledExportService;

    public ScheduledExportController(ScheduledExportService scheduledExportService) {
        this.scheduledExportService = scheduledExportService;
    }

    @GetMapping
    @Operation(summary = "Get all scheduled exports")
    public List<ScheduledExportDto> getAllScheduledExports() {
        return scheduledExportService.getAllScheduledExports();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a scheduled export by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Scheduled export found"),
            @ApiResponse(responseCode = "404", description = "Scheduled export not found")
    })
    public ResponseEntity<ScheduledExportDto> getScheduledExportById(
            @Parameter(description = "ID of the scheduled export to retrieve", required = true)
            @PathVariable UUID id) {
        return scheduledExportService.getScheduledExportById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new scheduled export")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Scheduled export created successfully")
    })
    public ResponseEntity<ScheduledExportDto> createScheduledExport(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Scheduled export data", required = true)
            @RequestBody ScheduledExportDto scheduledExportDto) {
        ScheduledExportDto createdScheduledExport = scheduledExportService.createScheduledExport(scheduledExportDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdScheduledExport);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing scheduled export")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Scheduled export updated successfully"),
            @ApiResponse(responseCode = "404", description = "Scheduled export not found")
    })
    public ResponseEntity<ScheduledExportDto> updateScheduledExport(
            @Parameter(description = "ID of the scheduled export to update", required = true)
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated scheduled export data", required = true)
            @RequestBody ScheduledExportDto scheduledExportDto) {
        try {
            ScheduledExportDto updatedScheduledExport = scheduledExportService.updateScheduledExport(id, scheduledExportDto);
            return ResponseEntity.ok(updatedScheduledExport);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a scheduled export")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Scheduled export deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Scheduled export not found")
    })
    public ResponseEntity<Void> deleteScheduledExport(
            @Parameter(description = "ID of the scheduled export to delete", required = true)
            @PathVariable UUID id) {
        try {
            scheduledExportService.deleteScheduledExport(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}