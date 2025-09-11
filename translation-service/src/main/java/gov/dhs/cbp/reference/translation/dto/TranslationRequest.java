package gov.dhs.cbp.reference.translation.dto;

import java.time.LocalDate;

public class TranslationRequest {
    
    private String fromSystem;
    private String fromCode;
    private String toSystem;
    private LocalDate asOf;
    
    public String getFromSystem() {
        return fromSystem;
    }
    
    public void setFromSystem(String fromSystem) {
        this.fromSystem = fromSystem;
    }
    
    public String getFromCode() {
        return fromCode;
    }
    
    public void setFromCode(String fromCode) {
        this.fromCode = fromCode;
    }
    
    public String getToSystem() {
        return toSystem;
    }
    
    public void setToSystem(String toSystem) {
        this.toSystem = toSystem;
    }
    
    public LocalDate getAsOf() {
        return asOf;
    }
    
    public void setAsOf(LocalDate asOf) {
        this.asOf = asOf;
    }
}