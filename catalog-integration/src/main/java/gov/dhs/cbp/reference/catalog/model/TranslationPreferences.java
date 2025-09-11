package gov.dhs.cbp.reference.catalog.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Consumer preferences for how translations should be generated
 */
public class TranslationPreferences {
    
    @JsonProperty("translation_type")
    private TranslationType translationType = TranslationType.VIEW;
    
    @JsonProperty("materialized")
    private boolean materialized = false;
    
    @JsonProperty("refresh_strategy")
    private RefreshStrategy refreshStrategy = RefreshStrategy.ON_DEMAND;
    
    @JsonProperty("refresh_interval_minutes")
    private Integer refreshIntervalMinutes;
    
    @JsonProperty("include_historical")
    private boolean includeHistorical = false;
    
    @JsonProperty("include_inactive")
    private boolean includeInactive = false;
    
    @JsonProperty("delivery_method")
    private DeliveryMethod deliveryMethod = DeliveryMethod.PULL;
    
    @JsonProperty("notification_webhook")
    private String notificationWebhook;
    
    @JsonProperty("database_engine")
    private String databaseEngine;
    
    @JsonProperty("schema_name")
    private String schemaName;
    
    @JsonProperty("naming_convention")
    private NamingConvention namingConvention = NamingConvention.SNAKE_CASE;
    
    @JsonProperty("include_metadata")
    private boolean includeMetadata = true;
    
    @JsonProperty("performance_hints")
    private PerformanceHints performanceHints;
    
    // Getters and setters
    public TranslationType getTranslationType() {
        return translationType;
    }
    
    public void setTranslationType(TranslationType translationType) {
        this.translationType = translationType;
    }
    
    public boolean isMaterialized() {
        return materialized;
    }
    
    public void setMaterialized(boolean materialized) {
        this.materialized = materialized;
    }
    
    public RefreshStrategy getRefreshStrategy() {
        return refreshStrategy;
    }
    
    public void setRefreshStrategy(RefreshStrategy refreshStrategy) {
        this.refreshStrategy = refreshStrategy;
    }
    
    public Integer getRefreshIntervalMinutes() {
        return refreshIntervalMinutes;
    }
    
    public void setRefreshIntervalMinutes(Integer refreshIntervalMinutes) {
        this.refreshIntervalMinutes = refreshIntervalMinutes;
    }
    
    public boolean isIncludeHistorical() {
        return includeHistorical;
    }
    
    public void setIncludeHistorical(boolean includeHistorical) {
        this.includeHistorical = includeHistorical;
    }
    
    public boolean isIncludeInactive() {
        return includeInactive;
    }
    
    public void setIncludeInactive(boolean includeInactive) {
        this.includeInactive = includeInactive;
    }
    
    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }
    
    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }
    
    public String getNotificationWebhook() {
        return notificationWebhook;
    }
    
    public void setNotificationWebhook(String notificationWebhook) {
        this.notificationWebhook = notificationWebhook;
    }
    
    public String getDatabaseEngine() {
        return databaseEngine;
    }
    
    public void setDatabaseEngine(String databaseEngine) {
        this.databaseEngine = databaseEngine;
    }
    
    public String getSchemaName() {
        return schemaName;
    }
    
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }
    
    public NamingConvention getNamingConvention() {
        return namingConvention;
    }
    
    public void setNamingConvention(NamingConvention namingConvention) {
        this.namingConvention = namingConvention;
    }
    
    public boolean isIncludeMetadata() {
        return includeMetadata;
    }
    
    public void setIncludeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }
    
    public PerformanceHints getPerformanceHints() {
        return performanceHints;
    }
    
    public void setPerformanceHints(PerformanceHints performanceHints) {
        this.performanceHints = performanceHints;
    }
    
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize TranslationPreferences to JSON", e);
        }
    }
    
    /**
     * Translation type preference
     */
    public enum TranslationType {
        VIEW,           // Database view
        MATERIALIZED_VIEW, // Materialized view
        TABLE,          // Physical table
        UDF,            // User-defined function
        STORED_PROC,    // Stored procedure
        API,            // REST API endpoint
        CDC_STREAM      // Change data capture stream
    }
    
    /**
     * Refresh strategy for materialized translations
     */
    public enum RefreshStrategy {
        ON_DEMAND,      // Manual refresh
        SCHEDULED,      // Periodic refresh
        REAL_TIME,      // Event-driven refresh
        ON_COMMIT       // Refresh on reference data change
    }
    
    /**
     * Delivery method for updates
     */
    public enum DeliveryMethod {
        PULL,           // Consumer pulls updates
        PUSH,           // Updates pushed to consumer
        WEBHOOK,        // Webhook notifications
        EVENT_STREAM    // Kafka/event stream
    }
    
    /**
     * Naming convention for generated artifacts
     */
    public enum NamingConvention {
        SNAKE_CASE,     // ref_country_v
        CAMEL_CASE,     // refCountryV
        PASCAL_CASE,    // RefCountryV
        KEBAB_CASE      // ref-country-v
    }
    
    /**
     * Performance hints for optimization
     */
    public static class PerformanceHints {
        private boolean useIndexes = true;
        private boolean usePartitioning = false;
        private String partitionColumn;
        private boolean useCaching = true;
        private Integer maxCacheSize;
        
        // Getters and setters
        public boolean isUseIndexes() {
            return useIndexes;
        }
        
        public void setUseIndexes(boolean useIndexes) {
            this.useIndexes = useIndexes;
        }
        
        public boolean isUsePartitioning() {
            return usePartitioning;
        }
        
        public void setUsePartitioning(boolean usePartitioning) {
            this.usePartitioning = usePartitioning;
        }
        
        public String getPartitionColumn() {
            return partitionColumn;
        }
        
        public void setPartitionColumn(String partitionColumn) {
            this.partitionColumn = partitionColumn;
        }
        
        public boolean isUseCaching() {
            return useCaching;
        }
        
        public void setUseCaching(boolean useCaching) {
            this.useCaching = useCaching;
        }
        
        public Integer getMaxCacheSize() {
            return maxCacheSize;
        }
        
        public void setMaxCacheSize(Integer maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
        }
    }
}