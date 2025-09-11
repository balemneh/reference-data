package gov.dhs.cbp.reference.loaders.iso.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IsoCountryDataTest {

    @Test
    void testIsoCountryData_GettersAndSetters() {
        // Given
        IsoCountryData data = new IsoCountryData();

        // When
        data.setAlpha2Code("US");
        data.setAlpha3Code("USA");
        data.setNumericCode("840");
        data.setName("United States");
        data.setRegion("Americas");
        data.setSubRegion("Northern America");

        // Then
        assertEquals("US", data.getAlpha2Code());
        assertEquals("USA", data.getAlpha3Code());
        assertEquals("840", data.getNumericCode());
        assertEquals("United States", data.getName());
        assertEquals("Americas", data.getRegion());
        assertEquals("Northern America", data.getSubRegion());
    }

    @Test
    void testIsoCountryData_DefaultConstructor() {
        // When
        IsoCountryData data = new IsoCountryData();

        // Then
        assertNotNull(data);
        assertNull(data.getAlpha2Code());
        assertNull(data.getAlpha3Code());
        assertNull(data.getNumericCode());
        assertNull(data.getName());
        assertNull(data.getRegion());
        assertNull(data.getSubRegion());
    }

    @Test
    void testIsoCountryData_WithNullValues() {
        // Given
        IsoCountryData data = new IsoCountryData();
        data.setAlpha2Code("US");
        data.setAlpha3Code("USA");
        data.setNumericCode(null);
        data.setName("United States");
        data.setRegion(null);
        data.setSubRegion(null);

        // Then
        assertEquals("US", data.getAlpha2Code());
        assertEquals("USA", data.getAlpha3Code());
        assertNull(data.getNumericCode());
        assertEquals("United States", data.getName());
        assertNull(data.getRegion());
        assertNull(data.getSubRegion());
    }

    @Test
    void testIsoCountryData_EmptyStrings() {
        // Given
        IsoCountryData data = new IsoCountryData();
        data.setAlpha2Code("");
        data.setAlpha3Code("");
        data.setName("");
        data.setRegion("");
        data.setSubRegion("");

        // Then
        assertEquals("", data.getAlpha2Code());
        assertEquals("", data.getAlpha3Code());
        assertEquals("", data.getName());
        assertEquals("", data.getRegion());
        assertEquals("", data.getSubRegion());
    }

    @Test
    void testIsoCountryData_UpdateValues() {
        // Given
        IsoCountryData data = new IsoCountryData();
        data.setAlpha2Code("CA");
        data.setName("Canada");

        // When - Update values
        data.setAlpha2Code("US");
        data.setName("United States");

        // Then
        assertEquals("US", data.getAlpha2Code());
        assertEquals("United States", data.getName());
    }

    @Test
    void testIsoCountryData_AllFields() {
        // Given
        IsoCountryData data = new IsoCountryData();

        // When
        data.setName("Germany");
        data.setAlpha2Code("DE");
        data.setAlpha3Code("DEU");
        data.setNumericCode("276");
        data.setRegion("Europe");
        data.setSubRegion("Western Europe");

        // Then
        assertEquals("Germany", data.getName());
        assertEquals("DE", data.getAlpha2Code());
        assertEquals("DEU", data.getAlpha3Code());
        assertEquals("276", data.getNumericCode());
        assertEquals("Europe", data.getRegion());
        assertEquals("Western Europe", data.getSubRegion());
    }
}