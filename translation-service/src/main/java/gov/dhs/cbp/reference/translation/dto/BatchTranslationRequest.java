package gov.dhs.cbp.reference.translation.dto;

import java.util.List;

public class BatchTranslationRequest {
    
    private List<TranslationRequest> translations;
    private boolean failOnError = false;
    
    public List<TranslationRequest> getTranslations() {
        return translations;
    }
    
    public void setTranslations(List<TranslationRequest> translations) {
        this.translations = translations;
    }
    
    public boolean isFailOnError() {
        return failOnError;
    }
    
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }
}