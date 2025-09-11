package gov.dhs.cbp.reference.api.mapper;

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
    
    ChangeRequestDto toDto(ChangeRequest entity);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChangeRequest toEntity(ChangeRequestDto dto);
    
    List<ChangeRequestDto> toDtoList(List<ChangeRequest> entities);
    
    List<ChangeRequest> toEntityList(List<ChangeRequestDto> dtos);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(ChangeRequestDto dto, @MappingTarget ChangeRequest entity);
}