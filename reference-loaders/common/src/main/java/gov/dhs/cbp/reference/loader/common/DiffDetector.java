package gov.dhs.cbp.reference.loader.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Detects differences between staging and production data
 */
@Component
public class DiffDetector<S extends StagingEntity, T> {
    
    private static final Logger logger = LoggerFactory.getLogger(DiffDetector.class);
    
    private final Function<S, String> stagingKeyExtractor;
    private final Function<T, String> productionKeyExtractor;
    private final DiffComparator<S, T> comparator;
    
    public DiffDetector(
            Function<S, String> stagingKeyExtractor,
            Function<T, String> productionKeyExtractor,
            DiffComparator<S, T> comparator) {
        this.stagingKeyExtractor = stagingKeyExtractor;
        this.productionKeyExtractor = productionKeyExtractor;
        this.comparator = comparator;
    }
    
    public DiffResult<S, T> detectDifferences(List<S> stagingData, List<T> productionData) {
        logger.debug("Detecting differences: {} staging records, {} production records", 
            stagingData.size(), productionData.size());
        
        DiffResult<S, T> result = new DiffResult<>();
        
        // Create maps for efficient lookup
        Map<String, S> stagingMap = stagingData.stream()
            .collect(Collectors.toMap(stagingKeyExtractor, Function.identity(), (v1, v2) -> v1));
        
        Map<String, T> productionMap = productionData.stream()
            .collect(Collectors.toMap(productionKeyExtractor, Function.identity(), (v1, v2) -> v1));
        
        // Find additions (in staging but not in production)
        for (Map.Entry<String, S> entry : stagingMap.entrySet()) {
            if (!productionMap.containsKey(entry.getKey())) {
                result.addAddition(entry.getValue());
            }
        }
        
        // Find updates and unchanged
        for (Map.Entry<String, T> entry : productionMap.entrySet()) {
            S stagingRecord = stagingMap.get(entry.getKey());
            if (stagingRecord != null) {
                if (comparator.hasChanged(stagingRecord, entry.getValue())) {
                    result.addUpdate(stagingRecord, entry.getValue());
                } else {
                    result.addUnchanged(entry.getValue());
                }
            }
        }
        
        // Find deletions (in production but not in staging)
        for (Map.Entry<String, T> entry : productionMap.entrySet()) {
            if (!stagingMap.containsKey(entry.getKey())) {
                result.addDeletion(entry.getValue());
            }
        }
        
        logger.info("Diff complete: {} additions, {} updates, {} deletions, {} unchanged",
            result.getAdditions().size(),
            result.getUpdates().size(),
            result.getDeletions().size(),
            result.getUnchanged().size());
        
        return result;
    }
    
    /**
     * Interface for comparing staging and production records
     */
    public interface DiffComparator<S, T> {
        boolean hasChanged(S staging, T production);
    }
}