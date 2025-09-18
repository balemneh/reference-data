package gov.dhs.cbp.reference.api.service;

import gov.dhs.cbp.reference.api.dto.CountryDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.exception.BusinessException;
import gov.dhs.cbp.reference.api.exception.ResourceNotFoundException;
import gov.dhs.cbp.reference.api.mapper.CountryMapper;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CountryService {
    
    private final CountryRepository countryRepository;
    private final CountryMapper countryMapper;
    
    public CountryService(CountryRepository countryRepository, CountryMapper countryMapper) {
        this.countryRepository = countryRepository;
        this.countryMapper = countryMapper;
    }
    
    public Optional<CountryDto> findById(UUID id) {
        return countryRepository.findById(id)
                .map(countryMapper::toDto);
    }
    
    public Optional<CountryDto> findByCodeAndSystem(String code, String systemCode) {
        return countryRepository.findCurrentByCodeAndSystemCode(code, systemCode)
                .map(countryMapper::toDto);
    }
    
    public Optional<CountryDto> findByCodeAndSystemAsOf(String code, String systemCode, LocalDate asOf) {
        return countryRepository.findByCodeAndSystemAsOf(code, systemCode, asOf)
                .map(countryMapper::toDto);
    }
    
    public PagedResponse<CountryDto> findBySystemCode(String systemCode, PageRequest pageRequest) {
        Page<Country> page = countryRepository.findCurrentBySystemCode(systemCode, pageRequest);
        List<CountryDto> dtos = page.getContent().stream()
                .map(countryMapper::toDto)
                .collect(Collectors.toList());
        return new PagedResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }
    
    public PagedResponse<CountryDto> searchByName(String name, PageRequest pageRequest) {
        Page<Country> page = countryRepository.searchByName(name, pageRequest);
        List<CountryDto> dtos = page.getContent().stream()
                .map(countryMapper::toDto)
                .collect(Collectors.toList());
        return new PagedResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }
    
    public List<CountryDto> findAllCurrent() {
        return countryRepository.findAllCurrent().stream()
                .map(countryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public long getTotalCount() {
        return countryRepository.count();
    }
    
    public long getActiveCount() {
        return countryRepository.countByIsActiveTrue();
    }
    
    public LocalDateTime getLastUpdated() {
        return countryRepository.findTopByOrderByRecordedAtDesc()
                .map(Country::getRecordedAt)
                .orElse(LocalDateTime.now());
    }
    
    public String getCurrentVersion() {
        return "1.0.0"; // TODO: Implement versioning system
    }

    // Change Request Workflow Integration Methods

    /**
     * Creates a new country through the change request workflow.
     * This method is called by CountryChangeRequestService when applying approved CREATE requests.
     *
     * @param countryDto The country data to create
     * @param recordedBy The user ID who triggered this creation
     * @return The created country DTO
     * @throws BusinessException if the country already exists or validation fails
     */
    @Transactional
    public CountryDto createCountryFromChangeRequest(CountryDto countryDto, String recordedBy) {
        // Check if country already exists with the same code and system
        Optional<CountryDto> existing = findByCodeAndSystem(countryDto.getCountryCode(), countryDto.getCodeSystem());
        if (existing.isPresent()) {
            throw new BusinessException(
                "Country already exists with code: " + countryDto.getCountryCode() +
                " in system: " + countryDto.getCodeSystem(),
                HttpStatus.CONFLICT
            );
        }

        // Set system fields
        countryDto.setRecordedAt(LocalDateTime.now());
        countryDto.setRecordedBy(recordedBy);
        if (countryDto.getValidFrom() == null) {
            countryDto.setValidFrom(LocalDate.now());
        }
        if (countryDto.getIsActive() == null) {
            countryDto.setIsActive(true);
        }

        // Convert to entity and save
        Country country = countryMapper.toEntity(countryDto);
        Country savedCountry = countryRepository.save(country);

        return countryMapper.toDto(savedCountry);
    }

    /**
     * Updates an existing country through the change request workflow.
     * This method is called by CountryChangeRequestService when applying approved UPDATE requests.
     *
     * @param countryDto The updated country data
     * @param recordedBy The user ID who triggered this update
     * @return The updated country DTO
     * @throws ResourceNotFoundException if the country doesn't exist
     * @throws BusinessException if validation fails
     */
    @Transactional
    public CountryDto updateCountryFromChangeRequest(CountryDto countryDto, String recordedBy) {
        // Find existing country
        Country existingCountry = countryRepository.findById(countryDto.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + countryDto.getId()));

        // In a bitemporal system, we would create a new version with current time
        // For now, we'll update the existing record
        existingCountry.setCountryName(countryDto.getCountryName());
        existingCountry.setIso2Code(countryDto.getIso2Code());
        existingCountry.setIso3Code(countryDto.getIso3Code());
        existingCountry.setNumericCode(countryDto.getNumericCode());
        existingCountry.setIsActive(countryDto.getIsActive());
        existingCountry.setValidFrom(countryDto.getValidFrom());
        existingCountry.setValidTo(countryDto.getValidTo());
        existingCountry.setRecordedAt(LocalDateTime.now());
        existingCountry.setRecordedBy(recordedBy);

        Country savedCountry = countryRepository.save(existingCountry);
        return countryMapper.toDto(savedCountry);
    }

    /**
     * Deactivates a country through the change request workflow.
     * This method is called by CountryChangeRequestService when applying approved DELETE requests.
     * Note: In a bitemporal system, we don't hard delete - we deactivate or set valid_to date.
     *
     * @param countryId The ID of the country to deactivate
     * @param recordedBy The user ID who triggered this deletion
     * @return The deactivated country DTO
     * @throws ResourceNotFoundException if the country doesn't exist
     */
    @Transactional
    public CountryDto deactivateCountryFromChangeRequest(UUID countryId, String recordedBy) {
        Country existingCountry = countryRepository.findById(countryId)
            .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + countryId));

        // Deactivate the country and set end date
        existingCountry.setIsActive(false);
        existingCountry.setValidTo(LocalDate.now());
        existingCountry.setRecordedAt(LocalDateTime.now());
        existingCountry.setRecordedBy(recordedBy);

        Country savedCountry = countryRepository.save(existingCountry);
        return countryMapper.toDto(savedCountry);
    }

    /**
     * Validates that a country DTO has all required fields for creation/update.
     *
     * @param countryDto The country DTO to validate
     * @throws BusinessException if validation fails
     */
    public void validateCountryData(CountryDto countryDto) {
        if (countryDto.getCountryCode() == null || countryDto.getCountryCode().trim().isEmpty()) {
            throw new BusinessException("Country code is required", HttpStatus.BAD_REQUEST);
        }
        if (countryDto.getCountryName() == null || countryDto.getCountryName().trim().isEmpty()) {
            throw new BusinessException("Country name is required", HttpStatus.BAD_REQUEST);
        }
        if (countryDto.getCodeSystem() == null || countryDto.getCodeSystem().trim().isEmpty()) {
            throw new BusinessException("Code system is required", HttpStatus.BAD_REQUEST);
        }
    }
}