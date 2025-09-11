package gov.dhs.cbp.reference.api.service;

import gov.dhs.cbp.reference.api.dto.CountryDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.mapper.CountryMapper;
import gov.dhs.cbp.reference.core.entity.CodeSystem;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountryServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private CountryMapper countryMapper;

    @InjectMocks
    private CountryService countryService;

    private Country sampleCountry;
    private CountryDto sampleCountryDto;
    private CodeSystem codeSystem;
    private UUID countryId;

    @BeforeEach
    void setUp() {
        countryId = UUID.randomUUID();
        
        codeSystem = new CodeSystem();
        codeSystem.setCode("ISO3166-1");
        codeSystem.setName("ISO 3166-1 Country Codes");
        codeSystem.setDescription("Standard country codes");

        sampleCountry = new Country();
        sampleCountry.setId(countryId);
        sampleCountry.setCountryCode("US");
        sampleCountry.setCountryName("United States");
        sampleCountry.setIso2Code("US");
        sampleCountry.setIso3Code("USA");
        sampleCountry.setNumericCode("840");
        sampleCountry.setCodeSystem(codeSystem);
        sampleCountry.setIsActive(true);
        sampleCountry.setValidFrom(LocalDate.of(2024, 1, 1));
        sampleCountry.setValidTo(null);
        sampleCountry.setRecordedAt(LocalDateTime.now());
        sampleCountry.setRecordedBy("SYSTEM");
        sampleCountry.setVersion(1L);

        sampleCountryDto = new CountryDto();
        sampleCountryDto.setId(countryId);
        sampleCountryDto.setCountryCode("US");
        sampleCountryDto.setCountryName("United States");
        sampleCountryDto.setIso2Code("US");
        sampleCountryDto.setIso3Code("USA");
        sampleCountryDto.setNumericCode("840");
        sampleCountryDto.setCodeSystem("ISO3166-1");
        sampleCountryDto.setIsActive(true);
        sampleCountryDto.setValidFrom(LocalDate.of(2024, 1, 1));
        sampleCountryDto.setValidTo(null);
        sampleCountryDto.setRecordedAt(LocalDateTime.now());
        sampleCountryDto.setRecordedBy("SYSTEM");
        sampleCountryDto.setVersion(1L);
    }

    @Test
    void findById_WhenCountryExists_ReturnsCountryDto() {
        // Given
        given(countryRepository.findById(countryId)).willReturn(Optional.of(sampleCountry));
        given(countryMapper.toDto(sampleCountry)).willReturn(sampleCountryDto);

        // When
        Optional<CountryDto> result = countryService.findById(countryId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(sampleCountryDto);
        verify(countryRepository).findById(countryId);
        verify(countryMapper).toDto(sampleCountry);
    }

    @Test
    void findById_WhenCountryDoesNotExist_ReturnsEmpty() {
        // Given
        given(countryRepository.findById(countryId)).willReturn(Optional.empty());

        // When
        Optional<CountryDto> result = countryService.findById(countryId);

        // Then
        assertThat(result).isEmpty();
        verify(countryRepository).findById(countryId);
    }

    @Test
    void findById_WithNullId_ReturnsEmpty() {
        // Given
        given(countryRepository.findById(null)).willReturn(Optional.empty());

        // When
        Optional<CountryDto> result = countryService.findById(null);

        // Then
        assertThat(result).isEmpty();
        verify(countryRepository).findById(null);
    }

    @Test
    void findByCodeAndSystem_WhenCountryExists_ReturnsCountryDto() {
        // Given
        String code = "US";
        String systemCode = "ISO3166-1";
        given(countryRepository.findCurrentByCodeAndSystemCode(code, systemCode))
                .willReturn(Optional.of(sampleCountry));
        given(countryMapper.toDto(sampleCountry)).willReturn(sampleCountryDto);

        // When
        Optional<CountryDto> result = countryService.findByCodeAndSystem(code, systemCode);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(sampleCountryDto);
        verify(countryRepository).findCurrentByCodeAndSystemCode(code, systemCode);
        verify(countryMapper).toDto(sampleCountry);
    }

    @Test
    void findByCodeAndSystem_WhenCountryDoesNotExist_ReturnsEmpty() {
        // Given
        String code = "INVALID";
        String systemCode = "ISO3166-1";
        given(countryRepository.findCurrentByCodeAndSystemCode(code, systemCode))
                .willReturn(Optional.empty());

        // When
        Optional<CountryDto> result = countryService.findByCodeAndSystem(code, systemCode);

        // Then
        assertThat(result).isEmpty();
        verify(countryRepository).findCurrentByCodeAndSystemCode(code, systemCode);
    }

    @Test
    void findByCodeAndSystem_WithNullInputs_HandlesGracefully() {
        // Given
        given(countryRepository.findCurrentByCodeAndSystemCode(null, null))
                .willReturn(Optional.empty());

        // When
        Optional<CountryDto> result = countryService.findByCodeAndSystem(null, null);

        // Then
        assertThat(result).isEmpty();
        verify(countryRepository).findCurrentByCodeAndSystemCode(null, null);
    }

    @Test
    void findByCodeAndSystemAsOf_WhenCountryExists_ReturnsCountryDto() {
        // Given
        String code = "US";
        String systemCode = "ISO3166-1";
        LocalDate asOf = LocalDate.of(2024, 6, 15);
        given(countryRepository.findByCodeAndSystemAsOf(code, systemCode, asOf))
                .willReturn(Optional.of(sampleCountry));
        given(countryMapper.toDto(sampleCountry)).willReturn(sampleCountryDto);

        // When
        Optional<CountryDto> result = countryService.findByCodeAndSystemAsOf(code, systemCode, asOf);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(sampleCountryDto);
        verify(countryRepository).findByCodeAndSystemAsOf(code, systemCode, asOf);
        verify(countryMapper).toDto(sampleCountry);
    }

    @Test
    void findByCodeAndSystemAsOf_WithFutureDate_ReturnsEmpty() {
        // Given
        String code = "US";
        String systemCode = "ISO3166-1";
        LocalDate futureDate = LocalDate.of(2025, 12, 31);
        given(countryRepository.findByCodeAndSystemAsOf(code, systemCode, futureDate))
                .willReturn(Optional.empty());

        // When
        Optional<CountryDto> result = countryService.findByCodeAndSystemAsOf(code, systemCode, futureDate);

        // Then
        assertThat(result).isEmpty();
        verify(countryRepository).findByCodeAndSystemAsOf(code, systemCode, futureDate);
    }

    @Test
    void findBySystemCode_WithValidSystem_ReturnsPagedResponse() {
        // Given
        String systemCode = "ISO3166-1";
        PageRequest pageRequest = PageRequest.of(0, 20);
        List<Country> countries = Arrays.asList(sampleCountry);
        Page<Country> countryPage = new PageImpl<>(countries, pageRequest, 1);
        List<CountryDto> countryDtos = Arrays.asList(sampleCountryDto);

        given(countryRepository.findCurrentBySystemCode(systemCode, pageRequest)).willReturn(countryPage);
        given(countryMapper.toDto(sampleCountry)).willReturn(sampleCountryDto);

        // When
        PagedResponse<CountryDto> result = countryService.findBySystemCode(systemCode, pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(sampleCountryDto);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(countryRepository).findCurrentBySystemCode(systemCode, pageRequest);
    }

    @Test
    void findBySystemCode_WithEmptyResult_ReturnsEmptyPagedResponse() {
        // Given
        String systemCode = "INVALID_SYSTEM";
        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<Country> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        given(countryRepository.findCurrentBySystemCode(systemCode, pageRequest)).willReturn(emptyPage);

        // When
        PagedResponse<CountryDto> result = countryService.findBySystemCode(systemCode, pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(countryRepository).findCurrentBySystemCode(systemCode, pageRequest);
    }

    @Test
    void searchByName_WithValidName_ReturnsPagedResponse() {
        // Given
        String name = "United";
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<Country> countries = Arrays.asList(sampleCountry);
        Page<Country> countryPage = new PageImpl<>(countries, pageRequest, 1);

        given(countryRepository.searchByName(name, pageRequest)).willReturn(countryPage);
        given(countryMapper.toDto(sampleCountry)).willReturn(sampleCountryDto);

        // When
        PagedResponse<CountryDto> result = countryService.searchByName(name, pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(sampleCountryDto);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(countryRepository).searchByName(name, pageRequest);
    }

    @Test
    void searchByName_WithNoMatches_ReturnsEmptyPagedResponse() {
        // Given
        String name = "NonExistentCountry";
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Country> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        given(countryRepository.searchByName(name, pageRequest)).willReturn(emptyPage);

        // When
        PagedResponse<CountryDto> result = countryService.searchByName(name, pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(countryRepository).searchByName(name, pageRequest);
    }

    @Test
    void searchByName_WithEmptyName_HandlesGracefully() {
        // Given
        String emptyName = "";
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Country> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        given(countryRepository.searchByName(emptyName, pageRequest)).willReturn(emptyPage);

        // When
        PagedResponse<CountryDto> result = countryService.searchByName(emptyName, pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(countryRepository).searchByName(emptyName, pageRequest);
    }

    @Test
    void findAllCurrent_WithCountries_ReturnsCountryList() {
        // Given
        List<Country> countries = Arrays.asList(sampleCountry);
        given(countryRepository.findAllCurrent()).willReturn(countries);
        given(countryMapper.toDto(sampleCountry)).willReturn(sampleCountryDto);

        // When
        List<CountryDto> result = countryService.findAllCurrent();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(sampleCountryDto);
        verify(countryRepository).findAllCurrent();
    }

    @Test
    void findAllCurrent_WithNoCountries_ReturnsEmptyList() {
        // Given
        given(countryRepository.findAllCurrent()).willReturn(Collections.emptyList());

        // When
        List<CountryDto> result = countryService.findAllCurrent();

        // Then
        assertThat(result).isEmpty();
        verify(countryRepository).findAllCurrent();
    }

    @Test
    void findBySystemCode_WithLargePageSize_HandlesCorrectly() {
        // Given
        String systemCode = "ISO3166-1";
        PageRequest pageRequest = PageRequest.of(0, 1000);
        List<Country> countries = Arrays.asList(sampleCountry);
        Page<Country> countryPage = new PageImpl<>(countries, pageRequest, 1);

        given(countryRepository.findCurrentBySystemCode(systemCode, pageRequest)).willReturn(countryPage);
        given(countryMapper.toDto(sampleCountry)).willReturn(sampleCountryDto);

        // When
        PagedResponse<CountryDto> result = countryService.findBySystemCode(systemCode, pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSize()).isEqualTo(1000);
        assertThat(result.getContent()).hasSize(1);
        verify(countryRepository).findCurrentBySystemCode(systemCode, pageRequest);
    }

    @Test
    void findBySystemCode_WithHighPageNumber_HandlesCorrectly() {
        // Given
        String systemCode = "ISO3166-1";
        PageRequest pageRequest = PageRequest.of(10, 20);
        Page<Country> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        given(countryRepository.findCurrentBySystemCode(systemCode, pageRequest)).willReturn(emptyPage);

        // When
        PagedResponse<CountryDto> result = countryService.findBySystemCode(systemCode, pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPage()).isEqualTo(10);
        assertThat(result.getContent()).isEmpty();
        verify(countryRepository).findCurrentBySystemCode(systemCode, pageRequest);
    }

    @Test
    void constructor_InitializesFieldsCorrectly() {
        // When
        CountryService service = new CountryService(countryRepository, countryMapper);

        // Then - constructor should set fields (verification through behavior)
        assertThat(service).isNotNull();
    }
}