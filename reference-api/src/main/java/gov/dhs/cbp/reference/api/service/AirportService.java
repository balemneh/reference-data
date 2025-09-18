package gov.dhs.cbp.reference.api.service;

import gov.dhs.cbp.reference.api.dto.AirportDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.mapper.AirportMapper;
import gov.dhs.cbp.reference.core.entity.Airport;
import gov.dhs.cbp.reference.core.repository.AirportRepository;
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
public class AirportService {
    
    private final AirportRepository airportRepository;
    private final AirportMapper airportMapper;
    
    public AirportService(AirportRepository airportRepository, AirportMapper airportMapper) {
        this.airportRepository = airportRepository;
        this.airportMapper = airportMapper;
    }
    
    public Optional<AirportDto> findById(UUID id) {
        return airportRepository.findById(id)
                .map(airportMapper::toDto);
    }
    
    public Optional<AirportDto> findByIataCodeAndSystem(String iataCode, String systemCode) {
        return airportRepository.findCurrentByIataCodeAndSystemCode(iataCode, systemCode)
                .map(airportMapper::toDto);
    }
    
    public Optional<AirportDto> findByIcaoCodeAndSystem(String icaoCode, String systemCode) {
        return airportRepository.findCurrentByIcaoCodeAndSystemCode(icaoCode, systemCode)
                .map(airportMapper::toDto);
    }
    
    public Optional<AirportDto> findByCodeAndSystem(String code, String systemCode) {
        // Try IATA first, then ICAO
        Optional<AirportDto> iataResult = findByIataCodeAndSystem(code, systemCode);
        if (iataResult.isPresent()) {
            return iataResult;
        }
        return findByIcaoCodeAndSystem(code, systemCode);
    }
    
    public Optional<AirportDto> findByIataCodeAndSystemAsOf(String iataCode, String systemCode, LocalDate asOf) {
        return airportRepository.findByIataCodeAndSystemAsOf(iataCode, systemCode, asOf)
                .map(airportMapper::toDto);
    }
    
    public Optional<AirportDto> findByIcaoCodeAndSystemAsOf(String icaoCode, String systemCode, LocalDate asOf) {
        return airportRepository.findByIcaoCodeAndSystemAsOf(icaoCode, systemCode, asOf)
                .map(airportMapper::toDto);
    }
    
    public Optional<AirportDto> findByCodeAndSystemAsOf(String code, String systemCode, LocalDate asOf) {
        // Try IATA first, then ICAO
        Optional<AirportDto> iataResult = findByIataCodeAndSystemAsOf(code, systemCode, asOf);
        if (iataResult.isPresent()) {
            return iataResult;
        }
        return findByIcaoCodeAndSystemAsOf(code, systemCode, asOf);
    }
    
    public PagedResponse<AirportDto> findBySystemCode(String systemCode, PageRequest pageRequest) {
        Page<Airport> page = airportRepository.findCurrentBySystemCode(systemCode, pageRequest);
        List<AirportDto> dtos = page.getContent().stream()
                .map(airportMapper::toDto)
                .collect(Collectors.toList());
        return new PagedResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }
    
    public PagedResponse<AirportDto> searchByName(String name, PageRequest pageRequest) {
        Page<Airport> page = airportRepository.searchByName(name, pageRequest);
        List<AirportDto> dtos = page.getContent().stream()
                .map(airportMapper::toDto)
                .collect(Collectors.toList());
        return new PagedResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }
    
    public PagedResponse<AirportDto> searchByNameCityCountry(String searchTerm, PageRequest pageRequest) {
        Page<Airport> page = airportRepository.searchByName(searchTerm, pageRequest);
        List<AirportDto> dtos = page.getContent().stream()
                .map(airportMapper::toDto)
                .collect(Collectors.toList());
        return new PagedResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }
    
    public List<AirportDto> findByCountryCode(String countryCode) {
        return airportRepository.findCurrentByCountryCode(countryCode).stream()
                .map(airportMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public List<AirportDto> findByCity(String city) {
        return airportRepository.findCurrentByCity(city).stream()
                .map(airportMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public List<AirportDto> findByAirportType(String airportType) {
        return airportRepository.findCurrentByAirportType(airportType).stream()
                .map(airportMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public List<AirportDto> findAllCurrent() {
        return airportRepository.findAllActive().stream()
                .map(airportMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public long getTotalCount() {
        return airportRepository.count();
    }
    
    public long getActiveCount() {
        return airportRepository.countByIsActiveTrue();
    }
    
    public LocalDateTime getLastUpdated() {
        return airportRepository.findTopByOrderByRecordedAtDesc()
                .map(Airport::getRecordedAt)
                .orElse(LocalDateTime.now());
    }
    
    public String getCurrentVersion() {
        return "1.0.0"; // TODO: Implement versioning system
    }
    
    // Write operations for future implementation (protected endpoints)
    
    @Transactional
    public AirportDto save(AirportDto airportDto) {
        Airport airport = airportMapper.toEntity(airportDto);
        // TODO: Add bitemporal logic, validation, and code system resolution
        Airport saved = airportRepository.save(airport);
        return airportMapper.toDto(saved);
    }
    
    @Transactional
    public AirportDto update(UUID id, AirportDto airportDto) {
        // TODO: Implement bitemporal update logic
        throw new UnsupportedOperationException("Update operations not yet implemented");
    }
    
    @Transactional
    public void deactivate(UUID id) {
        // TODO: Implement bitemporal deactivation
        throw new UnsupportedOperationException("Deactivate operations not yet implemented");
    }
}