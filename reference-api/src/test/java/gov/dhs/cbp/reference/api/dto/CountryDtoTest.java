package gov.dhs.cbp.reference.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CountryDtoTest {

    private ObjectMapper objectMapper;
    private Validator validator;
    private CountryDto sampleDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        
        ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        sampleDto = new CountryDto();
        sampleDto.setId(UUID.randomUUID());
        sampleDto.setCountryCode("US");
        sampleDto.setCountryName("United States");
        sampleDto.setIso2Code("US");
        sampleDto.setIso3Code("USA");
        sampleDto.setNumericCode("840");
        sampleDto.setCodeSystem("ISO3166-1");
        sampleDto.setIsActive(true);
        sampleDto.setValidFrom(LocalDate.of(2024, 1, 1));
        sampleDto.setRecordedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
        sampleDto.setRecordedBy("SYSTEM");
        sampleDto.setVersion(1L);
    }

    @Test
    void serialization_WithCompleteDto_SerializesCorrectly() throws Exception {
        // When
        String json = objectMapper.writeValueAsString(sampleDto);

        // Then
        assertThat(json).contains("\"countryCode\":\"US\"");
        assertThat(json).contains("\"countryName\":\"United States\"");
        assertThat(json).contains("\"iso3Code\":\"USA\"");
        assertThat(json).contains("\"codeSystem\":\"ISO3166-1\"");
        assertThat(json).contains("\"isActive\":true");
    }

    @Test
    void deserialization_WithValidJson_DeserializesCorrectly() throws Exception {
        // Given
        String json = """
            {
                "countryCode": "CA",
                "countryName": "Canada",
                "iso2Code": "CA",
                "iso3Code": "CAN",
                "numericCode": "124",
                "codeSystem": "ISO3166-1",
                "isActive": true,
                "validFrom": "2024-01-01",
                "recordedAt": "2024-01-01T12:00:00",
                "recordedBy": "TEST",
                "version": 1
            }
            """;

        // When
        CountryDto result = objectMapper.readValue(json, CountryDto.class);

        // Then
        assertThat(result.getCountryCode()).isEqualTo("CA");
        assertThat(result.getCountryName()).isEqualTo("Canada");
        assertThat(result.getIso3Code()).isEqualTo("CAN");
        assertThat(result.getNumericCode()).isEqualTo("124");
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getVersion()).isEqualTo(1L);
    }

    @Test
    void validation_WithValidDto_PassesValidation() {
        // When
        Set<ConstraintViolation<CountryDto>> violations = validator.validate(sampleDto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void gettersAndSetters_WorkCorrectly() {
        // Given
        CountryDto dto = new CountryDto();
        UUID id = UUID.randomUUID();
        LocalDate validFrom = LocalDate.of(2024, 1, 1);
        LocalDateTime recordedAt = LocalDateTime.of(2024, 1, 1, 12, 0);

        // When
        dto.setId(id);
        dto.setCountryCode("DE");
        dto.setCountryName("Germany");
        dto.setIso2Code("DE");
        dto.setIso3Code("DEU");
        dto.setNumericCode("276");
        dto.setCodeSystem("ISO3166-1");
        dto.setIsActive(false);
        dto.setValidFrom(validFrom);
        dto.setValidTo(LocalDate.of(2024, 12, 31));
        dto.setRecordedAt(recordedAt);
        dto.setRecordedBy("USER");
        dto.setVersion(2L);

        // Then
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getCountryCode()).isEqualTo("DE");
        assertThat(dto.getCountryName()).isEqualTo("Germany");
        assertThat(dto.getIso2Code()).isEqualTo("DE");
        assertThat(dto.getIso3Code()).isEqualTo("DEU");
        assertThat(dto.getNumericCode()).isEqualTo("276");
        assertThat(dto.getCodeSystem()).isEqualTo("ISO3166-1");
        assertThat(dto.getIsActive()).isFalse();
        assertThat(dto.getValidFrom()).isEqualTo(validFrom);
        assertThat(dto.getValidTo()).isEqualTo(LocalDate.of(2024, 12, 31));
        assertThat(dto.getRecordedAt()).isEqualTo(recordedAt);
        assertThat(dto.getRecordedBy()).isEqualTo("USER");
        assertThat(dto.getVersion()).isEqualTo(2L);
    }

    @Test
    void nullValues_HandleGracefully() {
        // Given
        CountryDto dto = new CountryDto();

        // When
        dto.setId(null);
        dto.setCountryCode(null);
        dto.setValidTo(null);
        dto.setRecordedAt(null);

        // Then
        assertThat(dto.getId()).isNull();
        assertThat(dto.getCountryCode()).isNull();
        assertThat(dto.getValidTo()).isNull();
        assertThat(dto.getRecordedAt()).isNull();
    }

    @Test
    void defaultConstructor_CreatesEmptyDto() {
        // When
        CountryDto dto = new CountryDto();

        // Then
        assertThat(dto.getId()).isNull();
        assertThat(dto.getCountryCode()).isNull();
        assertThat(dto.getCountryName()).isNull();
        assertThat(dto.getVersion()).isNull();
        assertThat(dto.getIsActive()).isNull();
    }

    @Test
    void booleanField_HandlesTrueAndFalse() {
        // Given
        CountryDto dto = new CountryDto();

        // When setting to true
        dto.setIsActive(true);

        // Then
        assertThat(dto.getIsActive()).isTrue();

        // When setting to false
        dto.setIsActive(false);

        // Then
        assertThat(dto.getIsActive()).isFalse();

        // When setting to null
        dto.setIsActive(null);

        // Then
        assertThat(dto.getIsActive()).isNull();
    }

    @Test
    void serialization_WithNullFields_IncludesNullFields() throws Exception {
        // Given
        CountryDto dto = new CountryDto();
        dto.setCountryCode("FR");
        dto.setCountryName("France");
        // Leave other fields null

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then
        assertThat(json).contains("\"countryCode\":\"FR\"");
        assertThat(json).contains("\"countryName\":\"France\"");
        // Note: Jackson by default includes null fields, the DTO doesn't have @JsonInclude(NON_NULL) at class level
        assertThat(json).contains("\"id\":null");
        assertThat(json).contains("\"validTo\":null");
        assertThat(json).contains("\"recordedAt\":null");
    }
}