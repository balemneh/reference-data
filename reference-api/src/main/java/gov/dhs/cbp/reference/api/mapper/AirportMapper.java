package gov.dhs.cbp.reference.api.mapper;

import gov.dhs.cbp.reference.api.dto.AirportDto;
import gov.dhs.cbp.reference.core.entity.Airport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// @Mapper(componentModel = "spring")
public interface AirportMapper {
    
    @Mapping(source = "codeSystem.code", target = "codeSystem")
    AirportDto toDto(Airport airport);
    
    @Mapping(target = "codeSystem", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "validFrom", ignore = true)
    @Mapping(target = "validTo", ignore = true)
    @Mapping(target = "recordedAt", ignore = true)
    @Mapping(target = "recordedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Airport toEntity(AirportDto dto);
    
    List<AirportDto> toDtoList(List<Airport> airports);
    
    List<Airport> toEntityList(List<AirportDto> dtos);
}