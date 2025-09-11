package gov.dhs.cbp.reference.api.mapper;

import gov.dhs.cbp.reference.api.dto.ChangeRequestDto;
import gov.dhs.cbp.reference.core.entity.ChangeRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChangeRequestMapperImpl implements ChangeRequestMapper {

    @Override
    public ChangeRequestDto toDto(ChangeRequest entity) {
        if (entity == null) {
            return null;
        }

        ChangeRequestDto dto = new ChangeRequestDto();
        dto.setId(entity.getId());
        dto.setChangeType(entity.getChangeType());
        dto.setEntityType(entity.getEntityType());
        dto.setEntityId(entity.getEntityId());
        dto.setRequestor(entity.getRequestor());
        dto.setApprover(entity.getApprover());
        dto.setStatus(entity.getStatus());
        dto.setPriority(entity.getPriority());
        dto.setJustification(entity.getJustification());
        dto.setRejectionReason(entity.getRejectionReason());
        dto.setProposedChanges(entity.getProposedChanges());
        dto.setCurrentValues(entity.getCurrentValues());
        dto.setExternalTicketId(entity.getExternalTicketId());
        dto.setWorkflowInstanceId(entity.getWorkflowInstanceId());
        dto.setApprovedAt(entity.getApprovedAt());
        dto.setAppliedAt(entity.getAppliedAt());
        dto.setEffectiveDate(entity.getEffectiveDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    @Override
    public ChangeRequest toEntity(ChangeRequestDto dto) {
        if (dto == null) {
            return null;
        }

        ChangeRequest entity = new ChangeRequest();
        entity.setChangeType(dto.getChangeType());
        entity.setEntityType(dto.getEntityType());
        entity.setEntityId(dto.getEntityId());
        entity.setRequestor(dto.getRequestor());
        entity.setApprover(dto.getApprover());
        entity.setStatus(dto.getStatus());
        entity.setPriority(dto.getPriority());
        entity.setJustification(dto.getJustification());
        entity.setRejectionReason(dto.getRejectionReason());
        entity.setProposedChanges(dto.getProposedChanges());
        entity.setCurrentValues(dto.getCurrentValues());
        entity.setExternalTicketId(dto.getExternalTicketId());
        entity.setWorkflowInstanceId(dto.getWorkflowInstanceId());
        entity.setApprovedAt(dto.getApprovedAt());
        entity.setAppliedAt(dto.getAppliedAt());
        entity.setEffectiveDate(dto.getEffectiveDate());
        return entity;
    }

    @Override
    public List<ChangeRequestDto> toDtoList(List<ChangeRequest> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChangeRequest> toEntityList(List<ChangeRequestDto> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void updateEntityFromDto(ChangeRequestDto dto, ChangeRequest entity) {
        if (dto == null || entity == null) {
            return;
        }
        
        entity.setChangeType(dto.getChangeType());
        entity.setEntityType(dto.getEntityType());
        entity.setEntityId(dto.getEntityId());
        entity.setRequestor(dto.getRequestor());
        entity.setApprover(dto.getApprover());
        entity.setStatus(dto.getStatus());
        entity.setPriority(dto.getPriority());
        entity.setJustification(dto.getJustification());
        entity.setRejectionReason(dto.getRejectionReason());
        entity.setProposedChanges(dto.getProposedChanges());
        entity.setCurrentValues(dto.getCurrentValues());
        entity.setExternalTicketId(dto.getExternalTicketId());
        entity.setWorkflowInstanceId(dto.getWorkflowInstanceId());
        entity.setApprovedAt(dto.getApprovedAt());
        entity.setAppliedAt(dto.getAppliedAt());
        entity.setEffectiveDate(dto.getEffectiveDate());
    }
}