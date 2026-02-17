package gov.dhs.cbp.reference.api.service;

import gov.dhs.cbp.reference.api.dto.ScheduledExportDto;
import gov.dhs.cbp.reference.core.entity.ScheduledExport;
import gov.dhs.cbp.reference.core.repository.ScheduledExportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ScheduledExportService {

    private final ScheduledExportRepository scheduledExportRepository;

    public ScheduledExportService(ScheduledExportRepository scheduledExportRepository) {
        this.scheduledExportRepository = scheduledExportRepository;
    }

    public List<ScheduledExportDto> getAllScheduledExports() {
        return scheduledExportRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<ScheduledExportDto> getScheduledExportById(UUID id) {
        return scheduledExportRepository.findById(id)
                .map(this::convertToDto);
    }

    @Transactional
    public ScheduledExportDto createScheduledExport(ScheduledExportDto scheduledExportDto) {
        ScheduledExport scheduledExport = convertToEntity(scheduledExportDto);
        ScheduledExport savedScheduledExport = scheduledExportRepository.save(scheduledExport);
        return convertToDto(savedScheduledExport);
    }

    @Transactional
    public ScheduledExportDto updateScheduledExport(UUID id, ScheduledExportDto scheduledExportDto) {
        ScheduledExport scheduledExport = scheduledExportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled export not found: " + id));

        scheduledExport.setName(scheduledExportDto.getName());
        scheduledExport.setEntityType(scheduledExportDto.getEntityType());
        scheduledExport.setFormat(scheduledExportDto.getFormat());
        scheduledExport.setSchedule(scheduledExportDto.getSchedule());
        scheduledExport.setEnabled(scheduledExportDto.isEnabled());
        scheduledExport.setFilters(scheduledExportDto.getFilters());
        scheduledExport.setRecipients(scheduledExportDto.getRecipients());
        // lastRun and nextRun will be updated by the scheduling engine

        ScheduledExport updatedScheduledExport = scheduledExportRepository.save(scheduledExport);
        return convertToDto(updatedScheduledExport);
    }

    @Transactional
    public void deleteScheduledExport(UUID id) {
        scheduledExportRepository.deleteById(id);
    }

    private ScheduledExportDto convertToDto(ScheduledExport scheduledExport) {
        ScheduledExportDto dto = new ScheduledExportDto();
        dto.setId(scheduledExport.getId());
        dto.setName(scheduledExport.getName());
        dto.setEntityType(scheduledExport.getEntityType());
        dto.setFormat(scheduledExport.getFormat());
        dto.setSchedule(scheduledExport.getSchedule());
        dto.setEnabled(scheduledExport.isEnabled());
        dto.setFilters(scheduledExport.getFilters());
        dto.setLastRun(scheduledExport.getLastRun());
        dto.setNextRun(scheduledExport.getNextRun());
        dto.setCreatedBy(scheduledExport.getCreatedBy());
        dto.setRecipients(scheduledExport.getRecipients());
        dto.setCreatedAt(scheduledExport.getCreatedAt());
        dto.setUpdatedAt(scheduledExport.getUpdatedAt());
        return dto;
    }

    private ScheduledExport convertToEntity(ScheduledExportDto dto) {
        ScheduledExport scheduledExport = new ScheduledExport();
        scheduledExport.setName(dto.getName());
        scheduledExport.setEntityType(dto.getEntityType());
        scheduledExport.setFormat(dto.getFormat());
        scheduledExport.setSchedule(dto.getSchedule());
        scheduledExport.setEnabled(dto.isEnabled());
        scheduledExport.setFilters(dto.getFilters());
        scheduledExport.setRecipients(dto.getRecipients());
        scheduledExport.setCreatedBy(dto.getCreatedBy() != null && !dto.getCreatedBy().isBlank() ? dto.getCreatedBy() : "system");
        // lastRun and nextRun are not set from the DTO
        return scheduledExport;
    }
}
