package gov.dhs.cbp.reference.catalog.service;

import gov.dhs.cbp.reference.catalog.client.OpenMetadataClient;
import gov.dhs.cbp.reference.catalog.client.OpenMetadataClient.DatasetMetadata;
import gov.dhs.cbp.reference.catalog.client.OpenMetadataClient.GeneratedArtifact;
import gov.dhs.cbp.reference.catalog.generator.CodeGenerator;
import gov.dhs.cbp.reference.catalog.generator.PostgresViewGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class RefSynthService {
    
    private static final Logger log = LoggerFactory.getLogger(RefSynthService.class);
    
    private final OpenMetadataClient metadataClient;
    private final Map<String, CodeGenerator> generators;
    
    public RefSynthService(OpenMetadataClient metadataClient) {
        this.metadataClient = metadataClient;
        this.generators = new HashMap<>();
        
        // Initialize generators for different engines
        generators.put("postgres", new PostgresViewGenerator());
    }
    
    public Flux<GeneratedArtifact> generateArtifactsForTag(String tagFqn) {
        log.info("Generating artifacts for datasets tagged with: {}", tagFqn);
        
        return metadataClient.getDatasetsByTag(tagFqn)
                .flatMap(this::generateArtifactsForDataset)
                .doOnNext(artifact -> log.info("Generated artifact: {} for engine: {}", 
                                              artifact.getName(), artifact.getEngine()))
                .doOnComplete(() -> log.info("Completed artifact generation for tag: {}", tagFqn));
    }
    
    public Flux<GeneratedArtifact> generateArtifactsForDataset(DatasetMetadata dataset) {
        log.info("Generating artifacts for dataset: {}", dataset.getFullyQualifiedName());
        
        return Flux.fromIterable(generators.entrySet())
                .flatMap(entry -> {
                    String engine = entry.getKey();
                    CodeGenerator generator = entry.getValue();
                    
                    return Mono.fromCallable(() -> generator.generate(dataset))
                            .map(content -> createArtifact(dataset, engine, content))
                            .doOnSuccess(artifact -> registerArtifact(dataset.getFullyQualifiedName(), artifact))
                            .onErrorContinue((error, item) -> 
                                log.error("Error generating artifact for engine {} and dataset {}: {}", 
                                        engine, dataset.getName(), error.getMessage()));
                });
    }
    
    private GeneratedArtifact createArtifact(DatasetMetadata dataset, String engine, String content) {
        GeneratedArtifact artifact = new GeneratedArtifact();
        artifact.setName(dataset.getName() + "_" + engine);
        artifact.setType(getArtifactType(engine));
        artifact.setEngine(engine);
        artifact.setContent(content);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sourceDataset", dataset.getFullyQualifiedName());
        metadata.put("generatedAt", System.currentTimeMillis());
        metadata.put("version", "1.0");
        artifact.setMetadata(metadata);
        
        return artifact;
    }
    
    private String getArtifactType(String engine) {
        switch (engine) {
            case "postgres":
            case "snowflake":
            case "bigquery":
                return "VIEW";
            case "databricks":
                return "UDF";
            case "dbt":
                return "MODEL";
            default:
                return "UNKNOWN";
        }
    }
    
    private void registerArtifact(String datasetFqn, GeneratedArtifact artifact) {
        metadataClient.registerGeneratedArtifact(datasetFqn, artifact)
                .subscribe(
                    v -> log.info("Successfully registered artifact: {}", artifact.getName()),
                    error -> log.error("Failed to register artifact: {}", artifact.getName(), error)
                );
    }
    
    public Mono<Map<String, Collection<GeneratedArtifact>>> generateWithLineage(String datasetFqn) {
        log.info("Generating artifacts with lineage for dataset: {}", datasetFqn);
        
        return metadataClient.getLineage(datasetFqn)
                .flatMapMany(lineage -> {
                    // Generate artifacts for all datasets in the lineage
                    return Flux.fromIterable(lineage.getNodes())
                            .filter(node -> "TABLE".equals(node.getType()))
                            .flatMap(node -> metadataClient.getDatasetMetadata(node.getFullyQualifiedName()))
                            .flatMap(this::generateArtifactsForDataset);
                })
                .collectMultimap(GeneratedArtifact::getEngine);
    }
}