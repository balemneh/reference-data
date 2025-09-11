package gov.dhs.cbp.reference.translation.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchTranslationResponse {
    
    private int totalRequested;
    private int successCount;
    private int failureCount;
    private List<TranslationResponse> successful = new ArrayList<>();
    private List<TranslationError> failed = new ArrayList<>();
    
    public int getTotalRequested() {
        return totalRequested;
    }
    
    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    
    public int getFailureCount() {
        return failureCount;
    }
    
    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }
    
    public List<TranslationResponse> getSuccessful() {
        return successful;
    }
    
    public void setSuccessful(List<TranslationResponse> successful) {
        this.successful = successful;
    }
    
    public List<TranslationError> getFailed() {
        return failed;
    }
    
    public void setFailed(List<TranslationError> failed) {
        this.failed = failed;
    }
    
    public static class TranslationError {
        private String fromSystem;
        private String fromCode;
        private String toSystem;
        private String errorMessage;
        private String errorCode;
        
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
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }
    }
}