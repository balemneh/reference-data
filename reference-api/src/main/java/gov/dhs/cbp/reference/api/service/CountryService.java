package gov.dhs.cbp.reference.api.service;

import gov.dhs.cbp.reference.api.dto.CountryDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.mapper.CountryMapper;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
}