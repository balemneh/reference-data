package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.dto.AirportDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.service.AirportService;
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
@RequestMapping("/v1/airports")
@Tag(name = "Airports", description = "Airport reference data API")
@Validated
public class AirportsController {
    
    private final AirportService airportService;
    
    public AirportsController(AirportService airportService) {
        this.airportService = airportService;
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get airport by ID", 
               description = "Retrieve a specific airport by its unique identifier")
    @ApiResponse(responseCode = "200", description = "Airport found", 
                 content = @Content(schema = @Schema(implementation = AirportDto.class)))
    @ApiResponse(responseCode = "404", description = "Airport not found")
    public ResponseEntity<AirportDto> getAirportById(
            @Parameter(description = "Airport unique identifier") @PathVariable UUID id) {
        return airportService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    @Operation(summary = "List airports by system code", 
               description = "Retrieve a paginated list of airports for a specific code system")
    @ApiResponse(responseCode = "200", description = "Airports retrieved successfully",
                 content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    public ResponseEntity<PagedResponse<AirportDto>> getAirportsBySystemCode(
            @Parameter(description = "Code system identifier", example = "IATA") 
            @RequestParam String codeSystem,
            @Parameter(description = "Page number (0-based)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") 
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<AirportDto> response = airportService.findBySystemCode(codeSystem, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-code")
    @Operation(summary = "Get airport by code",
               description = "Retrieve airport by IATA or ICAO code within a specific code system")
    @ApiResponse(responseCode = "200", description = "Airport found",
                 content = @Content(schema = @Schema(implementation = AirportDto.class)))
    @ApiResponse(responseCode = "404", description = "Airport not found")
    public ResponseEntity<AirportDto> getAirportByCode(
            @Parameter(description = "Airport code (IATA or ICAO)", example = "LAX")
            @RequestParam @Size(min = 3, max = 4) String code,
            @Parameter(description = "Code system identifier", example = "IATA")
            @RequestParam String codeSystem,
            @Parameter(description = "Get data as of specific date (optional)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {

        if (asOf != null) {
            return airportService.findByCodeAndSystemAsOf(code, codeSystem, asOf)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } else {
            return airportService.findByCodeAndSystem(code, codeSystem)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search airports", 
               description = "Search airports by name, city, or country")
    @ApiResponse(responseCode = "200", description = "Search results",
                 content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    public ResponseEntity<PagedResponse<AirportDto>> searchAirports(
            @Parameter(description = "Search term for name, city, or country", example = "Los Angeles") 
            @RequestParam @Size(min = 2, max = 255) String query,
            @Parameter(description = "Page number (0-based)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") 
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<AirportDto> response = airportService.searchByNameCityCountry(query, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-country")
    @Operation(summary = "Get airports by country", 
               description = "Retrieve all airports in a specific country")
    @ApiResponse(responseCode = "200", description = "Airports found",
                 content = @Content(schema = @Schema(implementation = List.class)))
    public ResponseEntity<List<AirportDto>> getAirportsByCountry(
            @Parameter(description = "Country code (ISO 3166-1 alpha-3)", example = "USA") 
            @RequestParam @Size(min = 3, max = 3) String countryCode) {
        List<AirportDto> airports = airportService.findByCountryCode(countryCode);
        return ResponseEntity.ok(airports);
    }
    
    @GetMapping("/by-city")
    @Operation(summary = "Get airports by city", 
               description = "Retrieve all airports in a specific city")
    @ApiResponse(responseCode = "200", description = "Airports found",
                 content = @Content(schema = @Schema(implementation = List.class)))
    public ResponseEntity<List<AirportDto>> getAirportsByCity(
            @Parameter(description = "City name", example = "Los Angeles") 
            @RequestParam @Size(min = 2, max = 100) String city) {
        List<AirportDto> airports = airportService.findByCity(city);
        return ResponseEntity.ok(airports);
    }
    
    @GetMapping("/by-type")
    @Operation(summary = "Get airports by type", 
               description = "Retrieve all airports of a specific type")
    @ApiResponse(responseCode = "200", description = "Airports found",
                 content = @Content(schema = @Schema(implementation = List.class)))
    public ResponseEntity<List<AirportDto>> getAirportsByType(
            @Parameter(description = "Airport type", example = "International") 
            @RequestParam @Size(min = 2, max = 50) String airportType) {
        List<AirportDto> airports = airportService.findByAirportType(airportType);
        return ResponseEntity.ok(airports);
    }
    
    @GetMapping("/current")
    @Operation(summary = "Get all current airports", 
               description = "Retrieve all currently active airports")
    @ApiResponse(responseCode = "200", description = "Active airports",
                 content = @Content(schema = @Schema(implementation = List.class)))
    public ResponseEntity<List<AirportDto>> getAllCurrentAirports() {
        List<AirportDto> airports = airportService.findAllCurrent();
        return ResponseEntity.ok(airports);
    }
    
    // Protected endpoints for data management (future implementation)
    
    @PostMapping
    @Operation(summary = "Create new airport", 
               description = "Create a new airport record (protected endpoint)")
    @ApiResponse(responseCode = "201", description = "Airport created",
                 content = @Content(schema = @Schema(implementation = AirportDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid airport data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<AirportDto> createAirport(@Valid @RequestBody AirportDto airportDto) {
        // TODO: Add authentication and authorization
        try {
            AirportDto created = airportService.save(airportDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update airport", 
               description = "Update an existing airport record (protected endpoint)")
    @ApiResponse(responseCode = "200", description = "Airport updated",
                 content = @Content(schema = @Schema(implementation = AirportDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid airport data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Airport not found")
    public ResponseEntity<AirportDto> updateAirport(
            @Parameter(description = "Airport unique identifier") @PathVariable UUID id,
            @Valid @RequestBody AirportDto airportDto) {
        // TODO: Add authentication and authorization
        try {
            AirportDto updated = airportService.update(id, airportDto);
            return ResponseEntity.ok(updated);
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate airport", 
               description = "Deactivate an airport record (protected endpoint)")
    @ApiResponse(responseCode = "204", description = "Airport deactivated")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Airport not found")
    public ResponseEntity<Void> deactivateAirport(
            @Parameter(description = "Airport unique identifier") @PathVariable UUID id) {
        // TODO: Add authentication and authorization
        try {
            airportService.deactivate(id);
            return ResponseEntity.noContent().build();
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }
}