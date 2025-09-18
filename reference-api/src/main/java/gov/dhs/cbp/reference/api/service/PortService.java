package gov.dhs.cbp.reference.api.service;

import gov.dhs.cbp.reference.api.dto.PortDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.mapper.PortMapper;
import gov.dhs.cbp.reference.core.entity.Port;
import gov.dhs.cbp.reference.core.repository.PortRepository;
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
public class PortService {
    
    private final PortRepository portRepository;
    private final PortMapper portMapper;
    
    public PortService(PortRepository portRepository, PortMapper portMapper) {
        this.portRepository = portRepository;
        this.portMapper = portMapper;
    }
    
    public Optional<PortDto> findById(UUID id) {
        return portRepository.findById(id)
                .map(portMapper::toDto);
    }
    
    public Optional<PortDto> findByPortCodeAndSystem(String portCode, String systemCode) {
        return portRepository.findCurrentByPortCodeAndSystemCode(portCode, systemCode)
                .map(portMapper::toDto);
    }
    
    public Optional<PortDto> findByUnLocodeAndSystem(String unLocode, String systemCode) {
        return portRepository.findCurrentByUnLocodeAndSystemCode(unLocode, systemCode)
                .map(portMapper::toDto);
    }
    
    public Optional<PortDto> findByCbpPortCodeAndSystem(String cbpPortCode, String systemCode) {
        return portRepository.findCurrentByCbpPortCodeAndSystemCode(cbpPortCode, systemCode)
                .map(portMapper::toDto);
    }
    
    public Optional<PortDto> findByCodeAndSystem(String code, String systemCode) {
        // Try port code first, then UN/LOCODE, then CBP port code
        Optional<PortDto> portCodeResult = findByPortCodeAndSystem(code, systemCode);
        if (portCodeResult.isPresent()) {
            return portCodeResult;
        }
        
        Optional<PortDto> unLocodeResult = findByUnLocodeAndSystem(code, systemCode);
        if (unLocodeResult.isPresent()) {
            return unLocodeResult;
        }
        
        return findByCbpPortCodeAndSystem(code, systemCode);
    }
    
    public Optional<PortDto> findByPortCodeAndSystemAsOf(String portCode, String systemCode, LocalDate asOf) {
        return portRepository.findByPortCodeAndSystemAsOf(portCode, systemCode, asOf)
                .map(portMapper::toDto);
    }
    
    public Optional<PortDto> findByUnLocodeAndSystemAsOf(String unLocode, String systemCode, LocalDate asOf) {
        return portRepository.findByUnLocodeAndSystemAsOf(unLocode, systemCode, asOf)
                .map(portMapper::toDto);
    }
    
    public Optional<PortDto> findByCodeAndSystemAsOf(String code, String systemCode, LocalDate asOf) {
        // Try port code first, then UN/LOCODE
        Optional<PortDto> portCodeResult = findByPortCodeAndSystemAsOf(code, systemCode, asOf);
        if (portCodeResult.isPresent()) {
            return portCodeResult;
        }
        
        return findByUnLocodeAndSystemAsOf(code, systemCode, asOf);
    }
    
    public PagedResponse<PortDto> findBySystemCode(String systemCode, PageRequest pageRequest) {
        Page<Port> page = portRepository.findCurrentBySystemCode(systemCode, pageRequest);
        List<PortDto> dtos = page.getContent().stream()
                .map(portMapper::toDto)
                .collect(Collectors.toList());
        return new PagedResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }
    
    public PagedResponse<PortDto> searchByName(String name, PageRequest pageRequest) {
        Page<Port> page = portRepository.searchByName(name, pageRequest);
        List<PortDto> dtos = page.getContent().stream()
                .map(portMapper::toDto)
                .collect(Collectors.toList());
        return new PagedResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }
    
    public PagedResponse<PortDto> searchByNameCityCountry(String searchTerm, PageRequest pageRequest) {
        Page<Port> page = portRepository.searchByName(searchTerm, pageRequest);
        List<PortDto> dtos = page.getContent().stream()
                .map(portMapper::toDto)
                .collect(Collectors.toList());
        return new PagedResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }
    
    public List<PortDto> findByCountryCode(String countryCode) {
        return portRepository.findCurrentByCountryCode(countryCode).stream()
                .map(portMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public List<PortDto> findByCity(String city) {
        return portRepository.findCurrentByCity(city).stream()
                .map(portMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public List<PortDto> findByPortType(String portType) {
        return portRepository.findCurrentByPortType(portType).stream()
                .map(portMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public List<PortDto> findAllCurrent() {
        return portRepository.findAllActive().stream()
                .map(portMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public long getTotalCount() {
        return portRepository.count();
    }
    
    public long getActiveCount() {
        return portRepository.countByIsActiveTrue();
    }
    
    public LocalDateTime getLastUpdated() {
        return portRepository.findTopByOrderByRecordedAtDesc()
                .map(Port::getRecordedAt)
                .orElse(LocalDateTime.now());
    }
    
    public String getCurrentVersion() {
        return "1.0.0"; // TODO: Implement versioning system
    }
    
    // Write operations for future implementation (protected endpoints)
    
    @Transactional
    public PortDto save(PortDto portDto) {
        Port port = portMapper.toEntity(portDto);
        // TODO: Add bitemporal logic, validation, and code system resolution
        Port saved = portRepository.save(port);
        return portMapper.toDto(saved);
    }
    
    @Transactional
    public PortDto update(UUID id, PortDto portDto) {
        // TODO: Implement bitemporal update logic
        throw new UnsupportedOperationException("Update operations not yet implemented");
    }
    
    @Transactional
    public void deactivate(UUID id) {
        // TODO: Implement bitemporal deactivation
        throw new UnsupportedOperationException("Deactivate operations not yet implemented");
    }
}