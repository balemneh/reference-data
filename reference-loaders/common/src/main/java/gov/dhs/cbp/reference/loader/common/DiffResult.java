package gov.dhs.cbp.reference.loader.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of diff detection between staging and production
 */
public class DiffResult<S, T> {
    
    private final List<S> additions = new ArrayList<>();
    private final List<UpdatePair<S, T>> updates = new ArrayList<>();
    private final List<T> deletions = new ArrayList<>();
    private final List<T> unchanged = new ArrayList<>();
    
    public void addAddition(S staging) {
        additions.add(staging);
    }
    
    public void addUpdate(S staging, T production) {
        updates.add(new UpdatePair<>(staging, production));
    }
    
    public void addDeletion(T production) {
        deletions.add(production);
    }
    
    public void addUnchanged(T production) {
        unchanged.add(production);
    }
    
    public boolean hasChanges() {
        return !additions.isEmpty() || !updates.isEmpty() || !deletions.isEmpty();
    }
    
    public int getTotalChanges() {
        return additions.size() + updates.size() + deletions.size();
    }
    
    public List<S> getAdditions() {
        return additions;
    }
    
    public List<UpdatePair<S, T>> getUpdates() {
        return updates;
    }
    
    public List<T> getDeletions() {
        return deletions;
    }
    
    public List<T> getUnchanged() {
        return unchanged;
    }
    
    /**
     * Represents a pair of staging and production records for updates
     */
    public static class UpdatePair<S, T> {
        private final S staged;
        private final T current;
        
        public UpdatePair(S staged, T current) {
            this.staged = staged;
            this.current = current;
        }
        
        public S getStaged() {
            return staged;
        }
        
        public T getCurrent() {
            return current;
        }
    }
}