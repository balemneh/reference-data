package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.dto.PortDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.service.PortService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/ports")
@Tag(name = "Ports", description = "Port reference data API")
@Validated
public class PortsController {
    
    private final PortService portService;
    
    public PortsController(PortService portService) {
        this.portService = portService;
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get port by ID", 
               description = "Retrieve a specific port by its unique identifier")
    @ApiResponse(responseCode = "200", description = "Port found", 
                 content = @Content(schema = @Schema(implementation = PortDto.class)))
    @ApiResponse(responseCode = "404", description = "Port not found")
    public ResponseEntity<PortDto> getPortById(
            @Parameter(description = "Port unique identifier") @PathVariable UUID id) {
        return portService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    @Operation(summary = "List ports by system code", 
               description = "Retrieve a paginated list of ports for a specific code system")
    @ApiResponse(responseCode = "200", description = "Ports retrieved successfully",
                 content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    public ResponseEntity<PagedResponse<PortDto>> getPortsBySystemCode(
            @Parameter(description = "Code system identifier", example = "UN-LOCODE") 
            @RequestParam String codeSystem,
            @Parameter(description = "Page number (0-based)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") 
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<PortDto> response = portService.findBySystemCode(codeSystem, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{code}")
    @Operation(summary = "Get port by code", 
               description = "Retrieve port by port code, UN/LOCODE, or CBP port code within a specific code system")
    @ApiResponse(responseCode = "200", description = "Port found",
                 content = @Content(schema = @Schema(implementation = PortDto.class)))
    @ApiResponse(responseCode = "404", description = "Port not found")
    public ResponseEntity<PortDto> getPortByCode(
            @Parameter(description = "Port code (port code, UN/LOCODE, or CBP port code)", example = "USNYC") 
            @PathVariable @Size(min = 2, max = 10) String code,
            @Parameter(description = "Code system identifier", example = "UN-LOCODE") 
            @RequestParam String codeSystem,
            @Parameter(description = "Get data as of specific date (optional)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        
        if (asOf != null) {
            return portService.findByCodeAndSystemAsOf(code, codeSystem, asOf)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } else {
            return portService.findByCodeAndSystem(code, codeSystem)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search ports", 
               description = "Search ports by name, city, or country")
    @ApiResponse(responseCode = "200", description = "Search results",
                 content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    public ResponseEntity<PagedResponse<PortDto>> searchPorts(
            @Parameter(description = "Search term for name, city, or country", example = "New York") 
            @RequestParam @Size(min = 2, max = 255) String query,
            @Parameter(description = "Page number (0-based)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") 
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<PortDto> response = portService.searchByNameCityCountry(query, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-country")
    @Operation(summary = "Get ports by country", 
               description = "Retrieve all ports in a specific country")
    @ApiResponse(responseCode = "200", description = "Ports found",
                 content = @Content(schema = @Schema(implementation = List.class)))
    public ResponseEntity<List<PortDto>> getPortsByCountry(
            @Parameter(description = "Country code (ISO 3166-1 alpha-3)", example = "USA") 
            @RequestParam @Size(min = 3, max = 3) String countryCode) {
        List<PortDto> ports = portService.findByCountryCode(countryCode);
        return ResponseEntity.ok(ports);
    }
    
    @GetMapping("/by-city")
    @Operation(summary = "Get ports by city", 
               description = "Retrieve all ports in a specific city")
    @ApiResponse(responseCode = "200", description = "Ports found",
                 content = @Content(schema = @Schema(implementation = List.class)))
    public ResponseEntity<List<PortDto>> getPortsByCity(
            @Parameter(description = "City name", example = "New York") 
            @RequestParam @Size(min = 2, max = 100) String city) {
        List<PortDto> ports = portService.findByCity(city);
        return ResponseEntity.ok(ports);
    }
    
    @GetMapping("/by-type")
    @Operation(summary = "Get ports by type", 
               description = "Retrieve all ports of a specific type")
    @ApiResponse(responseCode = "200", description = "Ports found",
                 content = @Content(schema = @Schema(implementation = List.class)))
    public ResponseEntity<List<PortDto>> getPortsByType(
            @Parameter(description = "Port type", example = "Container") 
            @RequestParam @Size(min = 2, max = 50) String portType) {
        List<PortDto> ports = portService.findByPortType(portType);
        return ResponseEntity.ok(ports);
    }
    
    @GetMapping("/current")
    @Operation(summary = "Get all current ports", 
               description = "Retrieve all currently active ports")
    @ApiResponse(responseCode = "200", description = "Active ports",
                 content = @Content(schema = @Schema(implementation = List.class)))
    public ResponseEntity<List<PortDto>> getAllCurrentPorts() {
        List<PortDto> ports = portService.findAllCurrent();
        return ResponseEntity.ok(ports);
    }
    
    // Protected endpoints for data management (future implementation)
    
    @PostMapping
    @Operation(summary = "Create new port", 
               description = "Create a new port record (protected endpoint)")
    @ApiResponse(responseCode = "201", description = "Port created",
                 content = @Content(schema = @Schema(implementation = PortDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid port data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<PortDto> createPort(@Valid @RequestBody PortDto portDto) {
        // TODO: Add authentication and authorization
        try {
            PortDto created = portService.save(portDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update port", 
               description = "Update an existing port record (protected endpoint)")
    @ApiResponse(responseCode = "200", description = "Port updated",
                 content = @Content(schema = @Schema(implementation = PortDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid port data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Port not found")
    public ResponseEntity<PortDto> updatePort(
            @Parameter(description = "Port unique identifier") @PathVariable UUID id,
            @Valid @RequestBody PortDto portDto) {
        // TODO: Add authentication and authorization
        try {
            PortDto updated = portService.update(id, portDto);
            return ResponseEntity.ok(updated);
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate port", 
               description = "Deactivate a port record (protected endpoint)")
    @ApiResponse(responseCode = "204", description = "Port deactivated")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Port not found")
    public ResponseEntity<Void> deactivatePort(
            @Parameter(description = "Port unique identifier") @PathVariable UUID id) {
        // TODO: Add authentication and authorization
        try {
            portService.deactivate(id);
            return ResponseEntity.noContent().build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
}