package gov.dhs.cbp.reference.api.service;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import gov.dhs.cbp.reference.core.entity.Country;
import gov.dhs.cbp.reference.core.repository.CountryRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

@Service
public class ExportService {

    private final CountryRepository countryRepository;

    public ExportService(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    public Resource exportData(String entityType, String format) {
        if ("COUNTRIES".equalsIgnoreCase(entityType)) {
            if ("CSV".equalsIgnoreCase(format)) {
                return exportCountriesAsCsv();
            } else {
                throw new IllegalArgumentException("Unsupported format: " + format);
            }
        } else {
            throw new IllegalArgumentException("Unsupported entity type: " + entityType);
        }
    }

    private Resource exportCountriesAsCsv() {
        List<Country> countries = countryRepository.findAll();
        try (StringWriter writer = new StringWriter()) {
            StatefulBeanToCsv<Country> beanToCsv = new StatefulBeanToCsvBuilder<Country>(writer)
                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .build();
            beanToCsv.write(countries);
            return new ByteArrayResource(writer.toString().getBytes());
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new RuntimeException("Failed to generate CSV file", e);
        }
    }
}
