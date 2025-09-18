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
        dto.setChangeType(entity.getOperationType());
        dto.setEntityType(entity.getDataType());
        dto.setEntityId(null); // No longer in entity
        dto.setRequestor(entity.getRequesterId());
        dto.setApprover(entity.getApprovedBy());
        dto.setStatus(entity.getStatus());
        // Convert priority from String to Integer
        String priority = entity.getPriority();
        if ("HIGH".equals(priority)) {
            dto.setPriority(1);
        } else if ("MEDIUM".equals(priority)) {
            dto.setPriority(2);
        } else if ("LOW".equals(priority)) {
            dto.setPriority(3);
        } else {
            dto.setPriority(2); // Default to MEDIUM
        }
        dto.setJustification(entity.getBusinessJustification());
        dto.setRejectionReason(entity.getRejectionReason());
        dto.setProposedChanges(entity.getProposedChanges());
        dto.setCurrentValues(entity.getCurrentValues());
        dto.setExternalTicketId(null); // No longer in entity
        dto.setWorkflowInstanceId(entity.getWorkflowInstanceId());
        dto.setApprovedAt(entity.getApprovedAt());
        dto.setAppliedAt(entity.getImplementedAt());
        dto.setEffectiveDate(entity.getSubmittedAt());
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
        entity.setOperationType(dto.getChangeType());
        entity.setDataType(dto.getEntityType());
        // entity.setEntityId not available
        entity.setRequesterId(dto.getRequestor());
        entity.setApprovedBy(dto.getApprover());
        entity.setStatus(dto.getStatus());
        // Convert priority from Integer to String
        Integer priority = dto.getPriority();
        if (priority == null || priority == 2) {
            entity.setPriority("MEDIUM");
        } else if (priority == 1) {
            entity.setPriority("HIGH");
        } else if (priority == 3) {
            entity.setPriority("LOW");
        } else {
            entity.setPriority("MEDIUM");
        }
        entity.setBusinessJustification(dto.getJustification());
        entity.setRejectionReason(dto.getRejectionReason());
        entity.setProposedChanges(dto.getProposedChanges());
        entity.setCurrentValues(dto.getCurrentValues());
        // entity.setExternalTicketId not available
        entity.setWorkflowInstanceId(dto.getWorkflowInstanceId());
        entity.setApprovedAt(dto.getApprovedAt());
        entity.setImplementedAt(dto.getAppliedAt());
        entity.setSubmittedAt(dto.getEffectiveDate());
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
        
        entity.setOperationType(dto.getChangeType());
        entity.setDataType(dto.getEntityType());
        // entity.setEntityId not available
        entity.setRequesterId(dto.getRequestor());
        entity.setApprovedBy(dto.getApprover());
        entity.setStatus(dto.getStatus());
        // Convert priority from Integer to String
        Integer priority = dto.getPriority();
        if (priority == null || priority == 2) {
            entity.setPriority("MEDIUM");
        } else if (priority == 1) {
            entity.setPriority("HIGH");
        } else if (priority == 3) {
            entity.setPriority("LOW");
        } else {
            entity.setPriority("MEDIUM");
        }
        entity.setBusinessJustification(dto.getJustification());
        entity.setRejectionReason(dto.getRejectionReason());
        entity.setProposedChanges(dto.getProposedChanges());
        entity.setCurrentValues(dto.getCurrentValues());
        // entity.setExternalTicketId not available
        entity.setWorkflowInstanceId(dto.getWorkflowInstanceId());
        entity.setApprovedAt(dto.getApprovedAt());
        entity.setImplementedAt(dto.getAppliedAt());
        entity.setSubmittedAt(dto.getEffectiveDate());
    }
}