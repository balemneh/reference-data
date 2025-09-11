package gov.dhs.cbp.reference.loaders.iso.model;

public class IsoCountryData {
    
    private String name;
    private String alpha2Code;
    private String alpha3Code;
    private String numericCode;
    private String region;
    private String subRegion;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAlpha2Code() {
        return alpha2Code;
    }
    
    public void setAlpha2Code(String alpha2Code) {
        this.alpha2Code = alpha2Code;
    }
    
    public String getAlpha3Code() {
        return alpha3Code;
    }
    
    public void setAlpha3Code(String alpha3Code) {
        this.alpha3Code = alpha3Code;
    }
    
    public String getNumericCode() {
        return numericCode;
    }
    
    public void setNumericCode(String numericCode) {
        this.numericCode = numericCode;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getSubRegion() {
        return subRegion;
    }
    
    public void setSubRegion(String subRegion) {
        this.subRegion = subRegion;
    }
}