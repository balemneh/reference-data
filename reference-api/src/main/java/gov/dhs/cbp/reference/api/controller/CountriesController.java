package gov.dhs.cbp.reference.api.controller;

import gov.dhs.cbp.reference.api.dto.CountryDto;
import gov.dhs.cbp.reference.api.dto.PagedResponse;
import gov.dhs.cbp.reference.api.service.CountryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/countries")
public class CountriesController {
    
    private final CountryService countryService;
    
    public CountriesController(CountryService countryService) {
        this.countryService = countryService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CountryDto> getCountryById(@PathVariable UUID id) {
        return countryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<PagedResponse<CountryDto>> getCountriesBySystemCode(
            @RequestParam String systemCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CountryDto> response = countryService.findBySystemCode(systemCode, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/by-code")
    public ResponseEntity<CountryDto> getCountryByCodeAndSystem(
            @RequestParam String code,
            @RequestParam String systemCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        
        if (asOf != null) {
            return countryService.findByCodeAndSystemAsOf(code, systemCode, asOf)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } else {
            return countryService.findByCodeAndSystem(code, systemCode)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<CountryDto>> searchCountries(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CountryDto> response = countryService.searchByName(name, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/current")
    public ResponseEntity<List<CountryDto>> getAllCurrentCountries() {
        List<CountryDto> countries = countryService.findAllCurrent();
        return ResponseEntity.ok(countries);
    }
}