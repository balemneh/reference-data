package gov.dhs.cbp.reference.api.config;

// import gov.dhs.cbp.reference.api.mapper.AirportMapper;  // TODO: Uncomment when AirportMapper is implemented
// import gov.dhs.cbp.reference.api.mapper.CarrierMapper;  // TODO: Uncomment when CarrierMapper is implemented
import gov.dhs.cbp.reference.api.mapper.CountryMapper;
// import gov.dhs.cbp.reference.api.mapper.PortMapper;     // TODO: Uncomment when PortMapper is implemented
import org.mapstruct.factory.Mappers;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration for WebMvcTest to provide required mapper beans
 */
@TestConfiguration
public class WebMvcTestConfiguration {
    
    @Bean
    public CountryMapper countryMapper() {
        return Mappers.getMapper(CountryMapper.class);
    }
    
    // TODO: Uncomment when AirportMapper is implemented
    // @Bean
    // public AirportMapper airportMapper() {
    //     return Mappers.getMapper(AirportMapper.class);
    // }
    
    // TODO: Uncomment when PortMapper is implemented
    // @Bean
    // public PortMapper portMapper() {
    //     return Mappers.getMapper(PortMapper.class);
    // }
    
    // TODO: Uncomment when CarrierMapper is implemented
    // @Bean
    // public CarrierMapper carrierMapper() {
    //     return Mappers.getMapper(CarrierMapper.class);
    // }
}