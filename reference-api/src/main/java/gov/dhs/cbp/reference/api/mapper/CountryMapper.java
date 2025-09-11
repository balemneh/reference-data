package gov.dhs.cbp.reference.api.mapper;

import gov.dhs.cbp.reference.api.dto.CountryDto;
import gov.dhs.cbp.reference.core.entity.Country;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CountryMapper {
    
    @Mapping(source = "codeSystem.code", target = "codeSystem")
    CountryDto toDto(Country country);
    
    @Mapping(target = "codeSystem", ignore = true)
    Country toEntity(CountryDto dto);
    
    List<CountryDto> toDtoList(List<Country> countries);
    
    List<Country> toEntityList(List<CountryDto> dtos);
}