package gov.dhs.cbp.reference.api.mapper;

import gov.dhs.cbp.reference.api.dto.AirportDto;
import gov.dhs.cbp.reference.core.entity.Airport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AirportMapperImpl implements AirportMapper {
    
    @Override
    public AirportDto toDto(Airport airport) {
        if (airport == null) {
            return null;
        }
        
        AirportDto dto = new AirportDto();
        dto.setId(airport.getId());
        dto.setIataCode(airport.getIataCode());
        dto.setIcaoCode(airport.getIcaoCode());
        dto.setAirportName(airport.getAirportName());
        dto.setCity(airport.getCity());
        dto.setStateProvince(airport.getStateProvince());
        dto.setCountryCode(airport.getCountryCode());
        dto.setLatitude(airport.getLatitude());
        dto.setLongitude(airport.getLongitude());
        dto.setElevation(airport.getElevation());
        dto.setAirportType(airport.getAirportType());
        dto.setTimezone(airport.getTimezone());
        dto.setIsActive(airport.getIsActive());
        dto.setValidFrom(airport.getValidFrom());
        dto.setValidTo(airport.getValidTo());
        
        if (airport.getCodeSystem() != null) {
            dto.setCodeSystem(airport.getCodeSystem().getCode());
        }
        
        return dto;
    }
    
    @Override
    public Airport toEntity(AirportDto dto) {
        if (dto == null) {
            return null;
        }
        
        Airport airport = new Airport();
        airport.setIataCode(dto.getIataCode());
        airport.setIcaoCode(dto.getIcaoCode());
        airport.setAirportName(dto.getAirportName());
        airport.setCity(dto.getCity());
        airport.setStateProvince(dto.getStateProvince());
        airport.setCountryCode(dto.getCountryCode());
        airport.setLatitude(dto.getLatitude());
        airport.setLongitude(dto.getLongitude());
        airport.setElevation(dto.getElevation());
        airport.setAirportType(dto.getAirportType());
        airport.setTimezone(dto.getTimezone());
        airport.setIsActive(dto.getIsActive());
        
        return airport;
    }
    
    @Override
    public List<AirportDto> toDtoList(List<Airport> airports) {
        if (airports == null) {
            return null;
        }
        
        List<AirportDto> list = new ArrayList<>(airports.size());
        for (Airport airport : airports) {
            list.add(toDto(airport));
        }
        return list;
    }
    
    @Override
    public List<Airport> toEntityList(List<AirportDto> dtos) {
        if (dtos == null) {
            return null;
        }
        
        List<Airport> list = new ArrayList<>(dtos.size());
        for (AirportDto dto : dtos) {
            list.add(toEntity(dto));
        }
        return list;
    }
}