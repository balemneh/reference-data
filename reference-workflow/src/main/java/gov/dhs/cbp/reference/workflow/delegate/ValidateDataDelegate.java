package gov.dhs.cbp.reference.workflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("validateDataDelegate")
public class ValidateDataDelegate implements JavaDelegate {
    
    private static final Logger log = LoggerFactory.getLogger(ValidateDataDelegate.class);
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String datasetType = (String) execution.getVariable("datasetType");
        Map<String, Object> payload = (Map<String, Object>) execution.getVariable("payload");
        
        log.info("Validating data for dataset type: {} with payload: {}", datasetType, payload);
        
        boolean isValid = validatePayload(datasetType, payload);
        execution.setVariable("dataValid", isValid);
        
        if (!isValid) {
            String validationErrors = getValidationErrors(datasetType, payload);
            execution.setVariable("validationErrors", validationErrors);
            log.warn("Validation failed for dataset type: {}. Errors: {}", datasetType, validationErrors);
        } else {
            log.info("Validation successful for dataset type: {}", datasetType);
        }
    }
    
    private boolean validatePayload(String datasetType, Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return false;
        }
        
        switch (datasetType) {
            case "COUNTRY":
                return validateCountryData(payload);
            case "PORT":
                return validatePortData(payload);
            case "AIRPORT":
                return validateAirportData(payload);
            case "CARRIER":
                return validateCarrierData(payload);
            default:
                log.warn("Unknown dataset type: {}", datasetType);
                return false;
        }
    }
    
    private boolean validateCountryData(Map<String, Object> payload) {
        return payload.containsKey("countryCode") && 
               payload.containsKey("countryName") &&
               payload.get("countryCode") != null &&
               payload.get("countryName") != null;
    }
    
    private boolean validatePortData(Map<String, Object> payload) {
        return payload.containsKey("portCode") && 
               payload.containsKey("portName") &&
               payload.containsKey("countryCode") &&
               payload.get("portCode") != null;
    }
    
    private boolean validateAirportData(Map<String, Object> payload) {
        return (payload.containsKey("iataCode") || payload.containsKey("icaoCode")) &&
               payload.containsKey("airportName") &&
               payload.containsKey("countryCode");
    }
    
    private boolean validateCarrierData(Map<String, Object> payload) {
        return payload.containsKey("carrierCode") && 
               payload.containsKey("carrierName") &&
               payload.get("carrierCode") != null;
    }
    
    private String getValidationErrors(String datasetType, Map<String, Object> payload) {
        StringBuilder errors = new StringBuilder();
        
        if (payload == null || payload.isEmpty()) {
            return "Payload is empty or null";
        }
        
        switch (datasetType) {
            case "COUNTRY":
                if (!payload.containsKey("countryCode")) errors.append("Missing countryCode; ");
                if (!payload.containsKey("countryName")) errors.append("Missing countryName; ");
                break;
            case "PORT":
                if (!payload.containsKey("portCode")) errors.append("Missing portCode; ");
                if (!payload.containsKey("portName")) errors.append("Missing portName; ");
                if (!payload.containsKey("countryCode")) errors.append("Missing countryCode; ");
                break;
            case "AIRPORT":
                if (!payload.containsKey("iataCode") && !payload.containsKey("icaoCode")) {
                    errors.append("Missing both iataCode and icaoCode; ");
                }
                if (!payload.containsKey("airportName")) errors.append("Missing airportName; ");
                if (!payload.containsKey("countryCode")) errors.append("Missing countryCode; ");
                break;
            case "CARRIER":
                if (!payload.containsKey("carrierCode")) errors.append("Missing carrierCode; ");
                if (!payload.containsKey("carrierName")) errors.append("Missing carrierName; ");
                break;
            default:
                errors.append("Unknown dataset type: ").append(datasetType);
        }
        
        return errors.toString();
    }
}