package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.dto.*;
import gov.dhs.cbp.reference.api.exception.ResourceNotFoundException;
import gov.dhs.cbp.reference.api.mapper.ChangeRequestMapper;
import gov.dhs.cbp.reference.api.service.CountryChangeRequestService;
import gov.dhs.cbp.reference.api.service.CountryService;
import gov.dhs.cbp.reference.core.entity.ChangeRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/countries")
public class CountriesController {

    private final CountryService countryService;
    private final CountryChangeRequestService countryChangeRequestService;
    private final ChangeRequestMapper changeRequestMapper;

    public CountriesController(CountryService countryService,
                              CountryChangeRequestService countryChangeRequestService,
                              ChangeRequestMapper changeRequestMapper) {
        this.countryService = countryService;
        this.countryChangeRequestService = countryChangeRequestService;
        this.changeRequestMapper = changeRequestMapper;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CountryDto> getCountryById(@PathVariable UUID id) {
        return countryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<PagedResponse<CountryDto>> getCountriesBySystemCode(
            @RequestParam String systemCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CountryDto> response = countryService.findBySystemCode(systemCode, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-code")
    public ResponseEntity<CountryDto> getCountryByCodeAndSystem(
            @RequestParam String code,
            @RequestParam String systemCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        
        if (asOf != null) {
            return countryService.findByCodeAndSystemAsOf(code, systemCode, asOf)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } else {
            return countryService.findByCodeAndSystem(code, systemCode)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<CountryDto>> searchCountries(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CountryDto> response = countryService.searchByName(name, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/current")
    public ResponseEntity<List<CountryDto>> getAllCurrentCountries() {
        List<CountryDto> countries = countryService.findAllCurrent();
        return ResponseEntity.ok(countries);
    }

    // New endpoints for change request workflow

    /**
     * Create a new country via change request
     */
    @PostMapping
    public ResponseEntity<ChangeRequestDto> createCountry(
            @Valid @RequestBody ChangeRequestCreateDto createDto) {

        // Mock user ID - in production this would come from authentication context
        String userId = "mock-user";

        ChangeRequest changeRequest = countryChangeRequestService.createChangeRequest(
                createDto.getCountryData(),
                "CREATE",
                userId,
                createDto.getReason()
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(changeRequestMapper.toDto(changeRequest));
    }

    /**
     * Update an existing country via change request
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChangeRequestDto> updateCountry(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRequestCreateDto updateDto) {

        // Ensure the ID in the path matches the ID in the body
        updateDto.getCountryData().setId(id);

        // Mock user ID - in production this would come from authentication context
        String userId = "mock-user";

        ChangeRequest changeRequest = countryChangeRequestService.createChangeRequest(
                updateDto.getCountryData(),
                "UPDATE",
                userId,
                updateDto.getReason()
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(changeRequestMapper.toDto(changeRequest));
    }

    /**
     * Delete a country via change request
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ChangeRequestDto> deleteCountry(
            @PathVariable UUID id,
            @RequestParam String reason) {

        // Get the existing country data
        CountryDto existingCountry = countryService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + id));

        // Mock user ID - in production this would come from authentication context
        String userId = "mock-user";

        ChangeRequest changeRequest = countryChangeRequestService.createChangeRequest(
                existingCountry,
                "DELETE",
                userId,
                reason
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(changeRequestMapper.toDto(changeRequest));
    }

    /**
     * Get pending change requests
     */
    @GetMapping("/change-requests")
    public ResponseEntity<PagedResponse<ChangeRequestDto>> getChangeRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<ChangeRequest> changeRequests = countryChangeRequestService
                .getPendingChangeRequests(PageRequest.of(page, size));

        // Convert to DTOs
        List<ChangeRequestDto> dtos = changeRequests.getContent().stream()
                .map(changeRequestMapper::toDto)
                .toList();

        PagedResponse<ChangeRequestDto> response = new PagedResponse<>();
        response.setContent(dtos);
        response.setPageNumber(changeRequests.getPageNumber());
        response.setPageSize(changeRequests.getPageSize());
        response.setTotalElements(changeRequests.getTotalElements());
        response.setTotalPages(changeRequests.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * Get change request by ID
     */
    @GetMapping("/change-requests/{id}")
    public ResponseEntity<ChangeRequestDto> getChangeRequestById(@PathVariable UUID id) {
        return countryChangeRequestService.getChangeRequestById(id)
                .map(changeRequestMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get change requests by status
     */
    @GetMapping("/change-requests/by-status")
    public ResponseEntity<PagedResponse<ChangeRequestDto>> getChangeRequestsByStatus(
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<ChangeRequest> changeRequests = countryChangeRequestService
                .getChangeRequestsByStatus(status, PageRequest.of(page, size));

        // Convert to DTOs
        List<ChangeRequestDto> dtos = changeRequests.getContent().stream()
                .map(changeRequestMapper::toDto)
                .toList();

        PagedResponse<ChangeRequestDto> response = new PagedResponse<>();
        response.setContent(dtos);
        response.setPageNumber(changeRequests.getPageNumber());
        response.setPageSize(changeRequests.getPageSize());
        response.setTotalElements(changeRequests.getTotalElements());
        response.setTotalPages(changeRequests.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * Get change request history for a country
     */
    @GetMapping("/{id}/change-requests")
    public ResponseEntity<PagedResponse<ChangeRequestDto>> getChangeRequestHistory(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<ChangeRequest> changeRequests = countryChangeRequestService
                .getChangeRequestHistory(id, PageRequest.of(page, size));

        // Convert to DTOs
        List<ChangeRequestDto> dtos = changeRequests.getContent().stream()
                .map(changeRequestMapper::toDto)
                .toList();

        PagedResponse<ChangeRequestDto> response = new PagedResponse<>();
        response.setContent(dtos);
        response.setPageNumber(changeRequests.getPageNumber());
        response.setPageSize(changeRequests.getPageSize());
        response.setTotalElements(changeRequests.getTotalElements());
        response.setTotalPages(changeRequests.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * Approve a change request
     */
    @PostMapping("/change-requests/{id}/approve")
    public ResponseEntity<ChangeRequestDto> approveChangeRequest(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalRequestDto approvalRequest) {

        ChangeRequest approved = countryChangeRequestService.approveChangeRequest(
                id,
                approvalRequest.getUserId(),
                approvalRequest.getComments()
        );

        return ResponseEntity.ok(changeRequestMapper.toDto(approved));
    }

    /**
     * Reject a change request
     */
    @PostMapping("/change-requests/{id}/reject")
    public ResponseEntity<ChangeRequestDto> rejectChangeRequest(
            @PathVariable UUID id,
            @Valid @RequestBody RejectionRequestDto rejectionRequest) {

        ChangeRequest rejected = countryChangeRequestService.rejectChangeRequest(
                id,
                rejectionRequest.getUserId(),
                rejectionRequest.getReason()
        );

        return ResponseEntity.ok(changeRequestMapper.toDto(rejected));
    }

    /**
     * Apply a change request
     */
    @PostMapping("/change-requests/{id}/apply")
    public ResponseEntity<ChangeRequestDto> applyChangeRequest(@PathVariable UUID id) {

        ChangeRequest applied = countryChangeRequestService.applyChangeRequest(id);

        return ResponseEntity.ok(changeRequestMapper.toDto(applied));
    }
}