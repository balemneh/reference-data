package gov.dhs.cbp.reference.catalog.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a generated translation artifact (view, UDF, table, etc.)
 */
public class TranslationArtifact {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("engine")
    private String engine;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("consumer_id")
    private String consumerId;
    
    @JsonProperty("table_fqn")
    private String tableFqn;
    
    @JsonProperty("reference_type")
    private ReferenceDataType referenceType;
    
    @JsonProperty("generated_at")
    private LocalDateTime generatedAt;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("deployment_status")
    private DeploymentStatus deploymentStatus = DeploymentStatus.PENDING;
    
    @JsonProperty("deployed_at")
    private LocalDateTime deployedAt;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("validation_result")
    private ValidationResult validationResult;
    
    @JsonProperty("size_bytes")
    private Long sizeBytes;
    
    @JsonProperty("checksum")
    private String checksum;
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getEngine() {
        return engine;
    }
    
    public void setEngine(String engine) {
        this.engine = engine;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
        this.sizeBytes = content != null ? (long) content.getBytes().length : 0L;
    }
    
    public String getConsumerId() {
        return consumerId;
    }
    
    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }
    
    public String getTableFqn() {
        return tableFqn;
    }
    
    public void setTableFqn(String tableFqn) {
        this.tableFqn = tableFqn;
    }
    
    public ReferenceDataType getReferenceType() {
        return referenceType;
    }
    
    public void setReferenceType(ReferenceDataType referenceType) {
        this.referenceType = referenceType;
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public DeploymentStatus getDeploymentStatus() {
        return deploymentStatus;
    }
    
    public void setDeploymentStatus(DeploymentStatus deploymentStatus) {
        this.deploymentStatus = deploymentStatus;
    }
    
    public LocalDateTime getDeployedAt() {
        return deployedAt;
    }
    
    public void setDeployedAt(LocalDateTime deployedAt) {
        this.deployedAt = deployedAt;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public ValidationResult getValidationResult() {
        return validationResult;
    }
    
    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
    }
    
    public Long getSizeBytes() {
        return sizeBytes;
    }
    
    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
    
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    /**
     * Deployment status of the artifact
     */
    public enum DeploymentStatus {
        PENDING,
        DEPLOYING,
        DEPLOYED,
        FAILED,
        ROLLBACK
    }
}