package gov.dhs.cbp.reference.api.config;

import org.ff4j.FF4j;
import org.ff4j.core.Feature;
import org.ff4j.springjdbc.store.EventRepositorySpringJdbc;
import org.ff4j.springjdbc.store.FeatureStoreSpringJdbc;
import org.ff4j.springjdbc.store.PropertyStoreSpringJdbc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass(FF4j.class)
public class FF4JConfig {

    @Autowired
    private DataSource dataSource;

    @Bean
    public FF4j ff4j() {
        FF4j ff4j = new FF4j();

        // Configure feature store (database persistence)
        FeatureStoreSpringJdbc featureStore = new FeatureStoreSpringJdbc();
        featureStore.setDataSource(dataSource);
        featureStore.createSchema(); // Create tables if they don't exist
        ff4j.setFeatureStore(featureStore);

        // Configure property store
        PropertyStoreSpringJdbc propertyStore = new PropertyStoreSpringJdbc();
        propertyStore.setDataSource(dataSource);
        propertyStore.createSchema();
        ff4j.setPropertiesStore(propertyStore);

        // Configure event store
        EventRepositorySpringJdbc eventStore = new EventRepositorySpringJdbc();
        eventStore.setDataSource(dataSource);
        eventStore.createSchema();
        ff4j.setEventRepository(eventStore);

        // Enable audit
        ff4j.audit(true);

        // Initialize default features if they don't exist
        initializeDefaultFeatures(ff4j);

        return ff4j;
    }

    private void initializeDefaultFeatures(FF4j ff4j) {
        // Reference Data Features
        createFeatureIfNotExists(ff4j, "countries", "Enable Countries Reference Data", true);
        createFeatureIfNotExists(ff4j, "ports", "Enable Ports Reference Data", true);
        createFeatureIfNotExists(ff4j, "airports", "Enable Airports Reference Data", true);
        createFeatureIfNotExists(ff4j, "carriers", "Enable Carriers Reference Data", true);
        createFeatureIfNotExists(ff4j, "units", "Enable Units Reference Data", true);
        createFeatureIfNotExists(ff4j, "languages", "Enable Languages Reference Data", true);

        // System Features
        createFeatureIfNotExists(ff4j, "changeRequests", "Enable Change Requests", true);
        createFeatureIfNotExists(ff4j, "analytics", "Enable Analytics", true);
        createFeatureIfNotExists(ff4j, "export", "Enable Export Functionality", true);
        createFeatureIfNotExists(ff4j, "import", "Enable Import Functionality", true);
        createFeatureIfNotExists(ff4j, "bulkOperations", "Enable Bulk Operations", true);
        createFeatureIfNotExists(ff4j, "advancedSearch", "Enable Advanced Search", true);
        createFeatureIfNotExists(ff4j, "apiAccess", "Enable API Access", true);
        createFeatureIfNotExists(ff4j, "webhooks", "Enable Webhooks", false);

        // Dashboard Features
        createFeatureIfNotExists(ff4j, "dashboard.showCountries", "Show Countries Card on Dashboard", true);
        createFeatureIfNotExists(ff4j, "dashboard.showPorts", "Show Ports Card on Dashboard", true);
        createFeatureIfNotExists(ff4j, "dashboard.showAirports", "Show Airports Card on Dashboard", true);
        createFeatureIfNotExists(ff4j, "dashboard.showCarriers", "Show Carriers Card on Dashboard", true);
        createFeatureIfNotExists(ff4j, "dashboard.showChangeRequests", "Show Change Requests on Dashboard", true);
        createFeatureIfNotExists(ff4j, "dashboard.showAnalytics", "Show Analytics on Dashboard", true);
        createFeatureIfNotExists(ff4j, "dashboard.showRecentActivity", "Show Recent Activity on Dashboard", true);
        createFeatureIfNotExists(ff4j, "dashboard.showSystemHealth", "Show System Health on Dashboard", true);

        // Experimental Features
        createFeatureIfNotExists(ff4j, "experimental.aiAssistant", "AI Assistant (Experimental)", false);
        createFeatureIfNotExists(ff4j, "experimental.graphView", "Graph View (Experimental)", false);
        createFeatureIfNotExists(ff4j, "experimental.realtimeSync", "Real-time Sync (Experimental)", false);
        createFeatureIfNotExists(ff4j, "experimental.collaboration", "Collaboration Features (Experimental)", false);
    }

    private void createFeatureIfNotExists(FF4j ff4j, String featureId, String description, boolean enabled) {
        if (!ff4j.exist(featureId)) {
            Feature feature = new Feature(featureId, enabled, description);
            ff4j.createFeature(feature);
        }
    }

}