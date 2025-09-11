package gov.dhs.cbp.reference.catalog.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenMetadataClient {
    
    private static final Logger log = LoggerFactory.getLogger(OpenMetadataClient.class);
    
    private final WebClient webClient;
    
    public OpenMetadataClient(@Value("${openmetadata.url:http://localhost:8585}") String baseUrl,
                             @Value("${openmetadata.api.key:}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl + "/api/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
    
    public Mono<DatasetMetadata> getDatasetMetadata(String fqn) {
        return webClient.get()
                .uri("/tables/name/{fqn}", fqn)
                .retrieve()
                .bodyToMono(DatasetMetadata.class)
                .doOnSuccess(metadata -> log.info("Retrieved metadata for dataset: {}", fqn))
                .doOnError(error -> log.error("Error retrieving metadata for dataset: {}", fqn, error));
    }
    
    public Flux<DatasetMetadata> getDatasetsByTag(String tagFqn) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tables")
                        .queryParam("tagFQN", tagFqn)
                        .build())
                .retrieve()
                .bodyToFlux(DatasetMetadata.class)
                .doOnNext(metadata -> log.debug("Found dataset with tag {}: {}", tagFqn, metadata.getName()));
    }
    
    public Mono<LineageData> getLineage(String fqn) {
        return webClient.get()
                .uri("/lineage/table/{fqn}", fqn)
                .retrieve()
                .bodyToMono(LineageData.class)
                .doOnSuccess(lineage -> log.info("Retrieved lineage for dataset: {}", fqn))
                .doOnError(error -> log.error("Error retrieving lineage for dataset: {}", fqn, error));
    }
    
    public Mono<Void> registerGeneratedArtifact(String datasetFqn, GeneratedArtifact artifact) {
        return webClient.post()
                .uri("/tables/{fqn}/extensions", datasetFqn)
                .bodyValue(artifact)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Registered artifact {} for dataset {}", 
                                          artifact.getName(), datasetFqn))
                .doOnError(error -> log.error("Error registering artifact for dataset: {}", 
                                             datasetFqn, error));
    }
    
    public Flux<TagDefinition> getAllTags() {
        return webClient.get()
                .uri("/tags")
                .retrieve()
                .bodyToFlux(TagDefinition.class)
                .doOnNext(tag -> log.debug("Found tag: {}", tag.getName()));
    }
    
    public Mono<Void> updateTableCustomProperties(String fqn, Map<String, Object> customProperties) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("customProperties", customProperties);
        
        return webClient.patch()
                .uri("/tables/name/{fqn}", fqn)
                .bodyValue(patch)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Updated custom properties for table {}", fqn))
                .doOnError(error -> log.error("Error updating custom properties for table: {}", fqn, error));
    }
    
    // Data classes
    public static class DatasetMetadata {
        private String id;
        private String name;
        private String fullyQualifiedName;
        private String description;
        private List<Column> columns;
        private List<Tag> tags;
        private Map<String, Object> customProperties;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getFullyQualifiedName() { return fullyQualifiedName; }
        public void setFullyQualifiedName(String fullyQualifiedName) { 
            this.fullyQualifiedName = fullyQualifiedName; 
        }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public List<Column> getColumns() { return columns; }
        public void setColumns(List<Column> columns) { this.columns = columns; }
        
        public List<Tag> getTags() { return tags; }
        public void setTags(List<Tag> tags) { this.tags = tags; }
        
        public Map<String, Object> getCustomProperties() { return customProperties; }
        public void setCustomProperties(Map<String, Object> customProperties) { 
            this.customProperties = customProperties; 
        }
    }
    
    public static class Column {
        private String name;
        private String dataType;
        private String description;
        private boolean nullable;
        private Map<String, Object> customProperties;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isNullable() { return nullable; }
        public void setNullable(boolean nullable) { this.nullable = nullable; }
        
        public Map<String, Object> getCustomProperties() { return customProperties; }
        public void setCustomProperties(Map<String, Object> customProperties) { 
            this.customProperties = customProperties; 
        }
    }
    
    public static class Tag {
        private String tagFQN;
        private String labelType;
        private String state;
        private String source;
        
        // Getters and setters
        public String getTagFQN() { return tagFQN; }
        public void setTagFQN(String tagFQN) { this.tagFQN = tagFQN; }
        
        public String getLabelType() { return labelType; }
        public void setLabelType(String labelType) { this.labelType = labelType; }
        
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }
    
    public static class LineageData {
        private String entity;
        private List<Node> nodes;
        private List<Edge> edges;
        
        // Getters and setters
        public String getEntity() { return entity; }
        public void setEntity(String entity) { this.entity = entity; }
        
        public List<Node> getNodes() { return nodes; }
        public void setNodes(List<Node> nodes) { this.nodes = nodes; }
        
        public List<Edge> getEdges() { return edges; }
        public void setEdges(List<Edge> edges) { this.edges = edges; }
        
        public static class Node {
            private String id;
            private String type;
            private String fullyQualifiedName;
            
            // Getters and setters
            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            
            public String getFullyQualifiedName() { return fullyQualifiedName; }
            public void setFullyQualifiedName(String fullyQualifiedName) { 
                this.fullyQualifiedName = fullyQualifiedName; 
            }
        }
        
        public static class Edge {
            private String fromEntity;
            private String toEntity;
            
            // Getters and setters
            public String getFromEntity() { return fromEntity; }
            public void setFromEntity(String fromEntity) { this.fromEntity = fromEntity; }
            
            public String getToEntity() { return toEntity; }
            public void setToEntity(String toEntity) { this.toEntity = toEntity; }
        }
    }
    
    public static class GeneratedArtifact {
        private String name;
        private String type;
        private String engine;
        private String content;
        private Map<String, Object> metadata;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getEngine() { return engine; }
        public void setEngine(String engine) { this.engine = engine; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    public static class TagDefinition {
        private String id;
        private String name;
        private String fullyQualifiedName;
        private String description;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getFullyQualifiedName() { return fullyQualifiedName; }
        public void setFullyQualifiedName(String fullyQualifiedName) { 
            this.fullyQualifiedName = fullyQualifiedName; 
        }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}