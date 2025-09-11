package gov.dhs.cbp.reference.core.util;

import gov.dhs.cbp.reference.core.entity.Bitemporal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class BitemporalHelper {
    
    private BitemporalHelper() {
    }
    
    public static <T extends Bitemporal> T createNewVersion(T current, String recordedBy, String changeRequestId) {
        try {
            @SuppressWarnings("unchecked")
            T newVersion = (T) current.getClass().getDeclaredConstructor().newInstance();
            
            copyNonTemporalFields(current, newVersion);
            
            newVersion.setId(null);
            newVersion.setVersion(current.getVersion() + 1);
            newVersion.setValidFrom(LocalDate.now());
            newVersion.setValidTo(null);
            newVersion.setRecordedAt(LocalDateTime.now());
            newVersion.setRecordedBy(recordedBy);
            newVersion.setChangeRequestId(changeRequestId);
            newVersion.setIsCorrection(false);
            
            return newVersion;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create new version", e);
        }
    }
    
    public static <T extends Bitemporal> T createCorrection(T current, String recordedBy, String changeRequestId) {
        T correction = createNewVersion(current, recordedBy, changeRequestId);
        correction.setIsCorrection(true);
        correction.setValidFrom(current.getValidFrom());
        correction.setValidTo(current.getValidTo());
        return correction;
    }
    
    public static <T extends Bitemporal> void endValidity(T entity, LocalDate endDate) {
        if (entity.getValidTo() == null || entity.getValidTo().isAfter(endDate)) {
            entity.setValidTo(endDate);
        }
    }
    
    public static <T extends Bitemporal> List<T> getCurrentVersions(List<T> allVersions) {
        return allVersions.stream()
                .filter(Bitemporal::isCurrentlyValid)
                .collect(Collectors.toList());
    }
    
    public static <T extends Bitemporal> List<T> getVersionsAsOf(List<T> allVersions, LocalDate asOfDate) {
        return allVersions.stream()
                .filter(v -> v.wasValidOn(asOfDate))
                .collect(Collectors.toList());
    }
    
    public static <T extends Bitemporal> Optional<T> getLatestVersion(List<T> versions) {
        return versions.stream()
                .max(Comparator.comparing(Bitemporal::getVersion));
    }
    
    public static <T extends Bitemporal> Map<String, List<T>> groupByChangeRequest(List<T> entities) {
        return entities.stream()
                .filter(e -> e.getChangeRequestId() != null)
                .collect(Collectors.groupingBy(Bitemporal::getChangeRequestId));
    }
    
    public static <T extends Bitemporal> Timeline<T> buildTimeline(List<T> versions) {
        return new Timeline<>(versions);
    }
    
    public static class Timeline<T extends Bitemporal> {
        private final List<T> versions;
        private final Map<LocalDate, List<T>> versionsByDate;
        
        public Timeline(List<T> versions) {
            this.versions = new ArrayList<>(versions);
            this.versions.sort(Comparator.comparing(Bitemporal::getValidFrom)
                    .thenComparing(Bitemporal::getVersion));
            
            this.versionsByDate = new HashMap<>();
            for (T version : versions) {
                versionsByDate.computeIfAbsent(version.getValidFrom(), k -> new ArrayList<>()).add(version);
            }
        }
        
        public Optional<T> getVersionOn(LocalDate date) {
            return versions.stream()
                    .filter(v -> v.wasValidOn(date))
                    .max(Comparator.comparing(Bitemporal::getVersion));
        }
        
        public List<T> getAllVersionsBetween(LocalDate startDate, LocalDate endDate) {
            return versions.stream()
                    .filter(v -> !v.getValidFrom().isAfter(endDate) &&
                            (v.getValidTo() == null || !v.getValidTo().isBefore(startDate)))
                    .collect(Collectors.toList());
        }
        
        public List<LocalDate> getChangePoints() {
            Set<LocalDate> changePoints = new TreeSet<>();
            for (T version : versions) {
                changePoints.add(version.getValidFrom());
                if (version.getValidTo() != null) {
                    changePoints.add(version.getValidTo());
                }
            }
            return new ArrayList<>(changePoints);
        }
    }
    
    private static <T> void copyNonTemporalFields(T source, T target) {
    }
}