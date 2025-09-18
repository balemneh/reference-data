package gov.dhs.cbp.reference.api.mapper;

import gov.dhs.cbp.reference.api.dto.PortDto;
import gov.dhs.cbp.reference.core.entity.Port;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// @Mapper(componentModel = "spring")
public interface PortMapper {
    
    @Mapping(source = "codeSystem.code", target = "codeSystem")
    PortDto toDto(Port port);
    
    @Mapping(target = "codeSystem", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "validFrom", ignore = true)
    @Mapping(target = "validTo", ignore = true)
    @Mapping(target = "recordedAt", ignore = true)
    @Mapping(target = "recordedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Port toEntity(PortDto dto);
    
    List<PortDto> toDtoList(List<Port> ports);
    
    List<Port> toEntityList(List<PortDto> dtos);
}