package gov.dhs.cbp.reference.api.mapper;

import gov.dhs.cbp.reference.api.dto.CountryDto;
import gov.dhs.cbp.reference.core.entity.Country;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CountryMapperImpl implements CountryMapper {

    @Override
    public CountryDto toDto(Country entity) {
        if (entity == null) {
            return null;
        }

        CountryDto dto = new CountryDto();
        dto.setId(entity.getId());
        dto.setCountryCode(entity.getCountryCode());
        dto.setCountryName(entity.getCountryName());
        dto.setIso2Code(entity.getIso2Code());
        dto.setIso3Code(entity.getIso3Code());
        dto.setNumericCode(entity.getNumericCode());
        dto.setIsActive(entity.getIsActive());
        // dto.setCodeSystemId(entity.getCodeSystemId()); // TODO: Fix this field mapping
        dto.setValidFrom(entity.getValidFrom());
        dto.setValidTo(entity.getValidTo());
        dto.setVersion(entity.getVersion());
        return dto;
    }

    @Override
    public Country toEntity(CountryDto dto) {
        if (dto == null) {
            return null;
        }

        Country entity = new Country();
        entity.setCountryCode(dto.getCountryCode());
        entity.setCountryName(dto.getCountryName());
        entity.setIso2Code(dto.getIso2Code());
        entity.setIso3Code(dto.getIso3Code());
        entity.setNumericCode(dto.getNumericCode());
        entity.setIsActive(dto.getIsActive());
        // entity.setCodeSystemId(dto.getCodeSystemId()); // TODO: Fix this field mapping
        entity.setValidFrom(dto.getValidFrom());
        entity.setValidTo(dto.getValidTo());
        return entity;
    }

    @Override
    public List<CountryDto> toDtoList(List<Country> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<Country> toEntityList(List<CountryDto> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    // updateEntityFromDto removed - not part of the interface
}