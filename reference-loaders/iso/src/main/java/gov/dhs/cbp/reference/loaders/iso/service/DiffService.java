package gov.dhs.cbp.reference.loaders.iso.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DiffService {
    
    public <T> DiffResult<T> calculateDiff(Collection<T> current, Collection<T> incoming, 
                                           KeyExtractor<T> keyExtractor) {
        Map<String, T> currentMap = new HashMap<>();
        Map<String, T> incomingMap = new HashMap<>();
        
        for (T item : current) {
            currentMap.put(keyExtractor.getKey(item), item);
        }
        
        for (T item : incoming) {
            incomingMap.put(keyExtractor.getKey(item), item);
        }
        
        List<T> toAdd = new ArrayList<>();
        List<T> toUpdate = new ArrayList<>();
        List<T> toRemove = new ArrayList<>();
        
        // Find items to add and update
        for (Map.Entry<String, T> entry : incomingMap.entrySet()) {
            String key = entry.getKey();
            T incomingItem = entry.getValue();
            
            if (!currentMap.containsKey(key)) {
                toAdd.add(incomingItem);
            } else {
                T currentItem = currentMap.get(key);
                if (!itemsEqual(currentItem, incomingItem)) {
                    toUpdate.add(incomingItem);
                }
            }
        }
        
        // Find items to remove
        for (Map.Entry<String, T> entry : currentMap.entrySet()) {
            String key = entry.getKey();
            if (!incomingMap.containsKey(key)) {
                toRemove.add(entry.getValue());
            }
        }
        
        return new DiffResult<>(toAdd, toUpdate, toRemove);
    }
    
    private <T> boolean itemsEqual(T item1, T item2) {
        // Simple equality check - can be overridden for complex comparisons
        return Objects.equals(item1, item2);
    }
    
    public interface KeyExtractor<T> {
        String getKey(T item);
    }
    
    public static class DiffResult<T> {
        private final List<T> toAdd;
        private final List<T> toUpdate;
        private final List<T> toRemove;
        
        public DiffResult(List<T> toAdd, List<T> toUpdate, List<T> toRemove) {
            this.toAdd = toAdd;
            this.toUpdate = toUpdate;
            this.toRemove = toRemove;
        }
        
        public List<T> getToAdd() {
            return toAdd;
        }
        
        public List<T> getToUpdate() {
            return toUpdate;
        }
        
        public List<T> getToRemove() {
            return toRemove;
        }
        
        public boolean hasChanges() {
            return !toAdd.isEmpty() || !toUpdate.isEmpty() || !toRemove.isEmpty();
        }
        
        public int getTotalChanges() {
            return toAdd.size() + toUpdate.size() + toRemove.size();
        }
    }
}