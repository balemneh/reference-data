package gov.dhs.cbp.reference.workflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidateDataDelegateTest {

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private ValidateDataDelegate validateDataDelegate;

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    @Test
    void testExecute_ValidCountryData() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("countryCode", "US");
        payload.put("countryName", "United States");
        
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", true);
        verify(execution, never()).setVariable(eq("validationErrors"), anyString());
    }

    @Test
    void testExecute_InvalidCountryData_MissingCode() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("countryName", "United States");
        
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", false);
        verify(execution).setVariable(eq("validationErrors"), contains("Missing countryCode"));
    }

    @Test
    void testExecute_InvalidCountryData_MissingName() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("countryCode", "US");
        
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", false);
        verify(execution).setVariable(eq("validationErrors"), contains("Missing countryName"));
    }

    @Test
    void testExecute_ValidPortData() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("portCode", "LAX");
        payload.put("portName", "Los Angeles");
        payload.put("countryCode", "US");
        
        when(execution.getVariable("datasetType")).thenReturn("PORT");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", true);
        verify(execution, never()).setVariable(eq("validationErrors"), anyString());
    }

    @Test
    void testExecute_InvalidPortData() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("portName", "Los Angeles");
        
        when(execution.getVariable("datasetType")).thenReturn("PORT");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", false);
        verify(execution).setVariable(eq("validationErrors"), contains("Missing portCode"));
    }

    @Test
    void testExecute_ValidAirportData_WithIataCode() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("iataCode", "JFK");
        payload.put("airportName", "John F Kennedy International");
        payload.put("countryCode", "US");
        
        when(execution.getVariable("datasetType")).thenReturn("AIRPORT");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", true);
    }

    @Test
    void testExecute_ValidAirportData_WithIcaoCode() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("icaoCode", "KJFK");
        payload.put("airportName", "John F Kennedy International");
        payload.put("countryCode", "US");
        
        when(execution.getVariable("datasetType")).thenReturn("AIRPORT");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", true);
    }

    @Test
    void testExecute_InvalidAirportData_MissingBothCodes() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("airportName", "John F Kennedy International");
        payload.put("countryCode", "US");
        
        when(execution.getVariable("datasetType")).thenReturn("AIRPORT");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", false);
        verify(execution).setVariable(eq("validationErrors"), contains("Missing both iataCode and icaoCode"));
    }

    @Test
    void testExecute_ValidCarrierData() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("carrierCode", "AA");
        payload.put("carrierName", "American Airlines");
        
        when(execution.getVariable("datasetType")).thenReturn("CARRIER");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", true);
    }

    @Test
    void testExecute_InvalidCarrierData() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("carrierName", "American Airlines");
        
        when(execution.getVariable("datasetType")).thenReturn("CARRIER");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", false);
        verify(execution).setVariable(eq("validationErrors"), contains("Missing carrierCode"));
    }

    @Test
    void testExecute_NullPayload() throws Exception {
        // Given
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
        when(execution.getVariable("payload")).thenReturn(null);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", false);
        verify(execution).setVariable("validationErrors", "Payload is empty or null");
    }

    @Test
    void testExecute_EmptyPayload() throws Exception {
        // Given
        when(execution.getVariable("datasetType")).thenReturn("PORT");
        when(execution.getVariable("payload")).thenReturn(new HashMap<>());

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", false);
        verify(execution).setVariable("validationErrors", "Payload is empty or null");
    }

    @Test
    void testExecute_UnknownDatasetType() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("someField", "someValue");
        
        when(execution.getVariable("datasetType")).thenReturn("UNKNOWN");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", false);
        verify(execution).setVariable(eq("validationErrors"), contains("Unknown dataset type: UNKNOWN"));
    }

    @Test
    void testExecute_CountryDataWithNullValues() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("countryCode", null);
        payload.put("countryName", "United States");
        
        when(execution.getVariable("datasetType")).thenReturn("COUNTRY");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", false);
    }

    @Test
    void testExecute_PortDataWithNullCode() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("portCode", null);
        payload.put("portName", "Los Angeles");
        payload.put("countryCode", "US");
        
        when(execution.getVariable("datasetType")).thenReturn("PORT");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", false);
    }

    @Test
    void testExecute_CarrierDataWithNullCode() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("carrierCode", null);
        payload.put("carrierName", "American Airlines");
        
        when(execution.getVariable("datasetType")).thenReturn("CARRIER");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", false);
    }

    @Test
    void testExecute_CompletePortValidationErrors() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("dummy", "value"); // Add a dummy value so it's not empty
        
        when(execution.getVariable("datasetType")).thenReturn("PORT");
        when(execution.getVariable("payload")).thenReturn(payload);

        // When
        validateDataDelegate.execute(execution);

        // Then
        verify(execution).setVariable("dataValid", false);
        verify(execution).setVariable(eq("validationErrors"), argThat(errors -> {
            String errorStr = (String) errors;
            return errorStr.contains("Missing portCode") &&
                   errorStr.contains("Missing portName") &&
                   errorStr.contains("Missing countryCode");
        }));
    }
}