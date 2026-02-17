package gov.dhs.cbp.reference.api.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.dhs.cbp.reference.api.dto.ChangeRequestDto;
import gov.dhs.cbp.reference.core.entity.ChangeRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", 
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChangeRequestMapper {
    
    @Mapping(source = "id", target = "id")
    @Mapping(source = "operationType", target = "changeType")
    @Mapping(source = "requesterId", target = "requestedBy")
    @Mapping(source = "businessJustification", target = "businessJustification")
    @Mapping(source = "dataType", target = "entityType")
    @Mapping(source = "implementedAt", target = "appliedAt")
    @Mapping(source = "approvedBy", target = "approver")
    @Mapping(source = "priority", target = "priority")
    ChangeRequestDto toDto(ChangeRequest entity);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "changeType", target = "operationType")
    @Mapping(source = "requestedBy", target = "requesterId")
    @Mapping(source = "entityType", target = "dataType")
    @Mapping(source = "appliedAt", target = "implementedAt")
    @Mapping(source = "approver", target = "approvedBy")
    @Mapping(source = "priority", target = "priority")
    ChangeRequest toEntity(ChangeRequestDto dto);

    List<ChangeRequestDto> toDtoList(List<ChangeRequest> entities);

    List<ChangeRequest> toEntityList(List<ChangeRequestDto> dtos);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(ChangeRequestDto dto, @MappingTarget ChangeRequest entity);
}