package gov.dhs.cbp.reference.api.mapper;

import gov.dhs.cbp.reference.api.dto.PortDto;
import gov.dhs.cbp.reference.core.entity.Port;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PortMapperImpl implements PortMapper {
    
    @Override
    public PortDto toDto(Port port) {
        if (port == null) {
            return null;
        }
        
        PortDto dto = new PortDto();
        dto.setId(port.getId());
        dto.setPortCode(port.getPortCode());
        dto.setPortName(port.getPortName());
        dto.setCity(port.getCity());
        dto.setStateProvince(port.getStateProvince());
        dto.setCountryCode(port.getCountryCode());
        dto.setLatitude(port.getLatitude());
        dto.setLongitude(port.getLongitude());
        dto.setPortType(port.getPortType());
        dto.setUnLocode(port.getUnLocode());
        dto.setCbpPortCode(port.getCbpPortCode());
        dto.setTimezone(port.getTimezone());
        dto.setIsActive(port.getIsActive());
        dto.setValidFrom(port.getValidFrom());
        dto.setValidTo(port.getValidTo());
        
        if (port.getCodeSystem() != null) {
            dto.setCodeSystem(port.getCodeSystem().getCode());
        }
        
        return dto;
    }
    
    @Override
    public Port toEntity(PortDto dto) {
        if (dto == null) {
            return null;
        }
        
        Port port = new Port();
        port.setPortCode(dto.getPortCode());
        port.setPortName(dto.getPortName());
        port.setCity(dto.getCity());
        port.setStateProvince(dto.getStateProvince());
        port.setCountryCode(dto.getCountryCode());
        port.setLatitude(dto.getLatitude());
        port.setLongitude(dto.getLongitude());
        port.setPortType(dto.getPortType());
        port.setUnLocode(dto.getUnLocode());
        port.setCbpPortCode(dto.getCbpPortCode());
        port.setTimezone(dto.getTimezone());
        port.setIsActive(dto.getIsActive());
        
        return port;
    }
    
    @Override
    public List<PortDto> toDtoList(List<Port> ports) {
        if (ports == null) {
            return null;
        }
        
        List<PortDto> list = new ArrayList<>(ports.size());
        for (Port port : ports) {
            list.add(toDto(port));
        }
        return list;
    }
    
    @Override
    public List<Port> toEntityList(List<PortDto> dtos) {
        if (dtos == null) {
            return null;
        }
        
        List<Port> list = new ArrayList<>(dtos.size());
        for (PortDto dto : dtos) {
            list.add(toEntity(dto));
        }
        return list;
    }
}