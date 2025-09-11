package gov.dhs.cbp.reference.loader.genc.config;

import gov.dhs.cbp.reference.loader.common.*;
import gov.dhs.cbp.reference.loader.genc.model.GencData;
import gov.dhs.cbp.reference.loader.genc.entity.GencEntityStaging;
import gov.dhs.cbp.reference.core.entity.Country;
import jakarta.validation.Validator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;
import java.util.function.Function;

@Configuration
@EnableScheduling
public class GencLoaderConfig {

    @Bean
    @ConfigurationProperties(prefix = "loader.genc")
    public LoaderConfiguration gencLoaderConfiguration() {
        LoaderConfiguration config = new LoaderConfiguration();
        
        // Default configuration values
        config.setBatchSize(1000);
        config.setAutoApplyChanges(false); // Require approval by default
        config.setFailOnValidationError(false); // Continue with warnings
        config.setPublishEvents(true);
        config.setIncrementalMode(false); // Full load by default
        
        return config;
    }

    @Bean
    public ValidationService<GencData> gencValidationService(Validator validator) {
        ValidationService<GencData> service = new ValidationService<>(validator);
        
        // Add custom validation rules
        service.addRule(new ValidationService.ValidationRule<GencData>() {
            @Override
            public String getName() {
                return "genc3CodeValidation";
            }
            
            @Override
            public Result validate(GencData record) {
                if (record.getGenc3Code() == null || record.getGenc3Code().isEmpty()) {
                    return Result.invalid("GENC 3-character code is required");
                }
                if (!record.getGenc3Code().matches("^[A-Z]{3}$")) {
                    return Result.invalid("GENC 3-character code must be 3 uppercase letters");
                }
                return Result.valid();
            }
        });
        
        service.addRule(new ValidationService.ValidationRule<GencData>() {
            @Override
            public String getName() {
                return "nameValidation";
            }
            
            @Override
            public Result validate(GencData record) {
                if (record.getName() == null || record.getName().isEmpty()) {
                    return Result.invalid("Entity name is required");
                }
                return Result.valid();
            }
        });
        
        service.addRule(new ValidationService.ValidationRule<GencData>() {
            @Override
            public String getName() {
                return "coordinateValidation";
            }
            
            @Override
            public Result validate(GencData record) {
                if (record.getLatitude() != null && !record.getLatitude().isEmpty()) {
                    try {
                        double lat = Double.parseDouble(record.getLatitude());
                        if (lat < -90 || lat > 90) {
                            return Result.warning("Latitude must be between -90 and 90");
                        }
                    } catch (NumberFormatException e) {
                        return Result.warning("Invalid latitude format");
                    }
                }
                
                if (record.getLongitude() != null && !record.getLongitude().isEmpty()) {
                    try {
                        double lon = Double.parseDouble(record.getLongitude());
                        if (lon < -180 || lon > 180) {
                            return Result.warning("Longitude must be between -180 and 180");
                        }
                    } catch (NumberFormatException e) {
                        return Result.warning("Invalid longitude format");
                    }
                }
                
                return Result.valid();
            }
        });
        
        return service;
    }

    @Bean
    public DiffDetector<GencEntityStaging, Country> gencDiffDetector() {
        Function<GencEntityStaging, String> stagingKeyExtractor = 
            staging -> staging.getGenc3Code();
            
        Function<Country, String> productionKeyExtractor = 
            country -> country.getCountryCode();
            
        DiffDetector.DiffComparator<GencEntityStaging, Country> comparator = 
            (staging, production) -> {
                return !staging.getName().equals(production.getCountryName()) ||
                       !staging.getGenc2Code().equals(production.getIso2Code()) ||
                       !staging.getGencNumeric().equals(production.getNumericCode()) ||
                       !("current".equalsIgnoreCase(staging.getGencStatus()) == production.getIsActive());
            };
            
        return new DiffDetector<>(stagingKeyExtractor, productionKeyExtractor, comparator);
    }
}