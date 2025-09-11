package gov.dhs.cbp.reference.api.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResult {
    
    private String entityType;
    private UUID entityId;
    private String code;
    private String name;
    private String description;
    private Map<String, Object> additionalInfo;
    private Float similarityScore;
    private Float rank;
    private String highlightedText;
    
    public SearchResult() {}
    
    public SearchResult(String entityType, UUID entityId, String code, String name) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.code = code;
        this.name = name;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public UUID getEntityId() {
        return entityId;
    }
    
    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }
    
    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
    
    public Float getSimilarityScore() {
        return similarityScore;
    }
    
    public void setSimilarityScore(Float similarityScore) {
        this.similarityScore = similarityScore;
    }
    
    public Float getRank() {
        return rank;
    }
    
    public void setRank(Float rank) {
        this.rank = rank;
    }
    
    public String getHighlightedText() {
        return highlightedText;
    }
    
    public void setHighlightedText(String highlightedText) {
        this.highlightedText = highlightedText;
    }
}