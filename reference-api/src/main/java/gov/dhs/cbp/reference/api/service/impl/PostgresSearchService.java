package gov.dhs.cbp.reference.api.service.impl;

import gov.dhs.cbp.reference.api.dto.search.AutocompleteResult;
import gov.dhs.cbp.reference.api.dto.search.SearchResult;
import gov.dhs.cbp.reference.api.service.SearchService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PostgresSearchService implements SearchService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public Page<SearchResult> universalSearch(String query, List<String> dataTypes, 
                                             Float similarityThreshold, Pageable pageable) {
        if (dataTypes == null || dataTypes.isEmpty()) {
            dataTypes = Arrays.asList("countries", "ports", "airports");
        }
        
        String sql = "SELECT * FROM reference_data.universal_search(:query, :dataTypes, :threshold, :limit)";
        
        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("query", query);
        nativeQuery.setParameter("dataTypes", dataTypes.toArray(new String[0]));
        nativeQuery.setParameter("threshold", similarityThreshold != null ? similarityThreshold : 0.3f);
        nativeQuery.setParameter("limit", pageable.getPageSize());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = nativeQuery.getResultList();
        
        List<SearchResult> searchResults = results.stream()
            .map(this::mapToSearchResult)
            .collect(Collectors.toList());
        
        // Get total count
        String countSql = "SELECT COUNT(*) FROM reference_data.universal_search(:query, :dataTypes, :threshold, 1000)";
        Query countQuery = entityManager.createNativeQuery(countSql);
        countQuery.setParameter("query", query);
        countQuery.setParameter("dataTypes", dataTypes.toArray(new String[0]));
        countQuery.setParameter("threshold", similarityThreshold != null ? similarityThreshold : 0.3f);
        
        Long totalCount = ((Number) countQuery.getSingleResult()).longValue();
        
        return new PageImpl<>(searchResults, pageable, totalCount);
    }
    
    @Override
    public Page<SearchResult> searchCountries(String query, Float similarityThreshold, 
                                             Pageable pageable) {
        String sql = "SELECT * FROM reference_data.search_countries(:query, :threshold, :limit) " +
                    "OFFSET :offset";
        
        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("query", query);
        nativeQuery.setParameter("threshold", similarityThreshold != null ? similarityThreshold : 0.3f);
        nativeQuery.setParameter("limit", pageable.getPageSize());
        nativeQuery.setParameter("offset", pageable.getOffset());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = nativeQuery.getResultList();
        
        List<SearchResult> searchResults = results.stream()
            .map(row -> {
                SearchResult result = new SearchResult();
                result.setEntityType("country");
                result.setEntityId((UUID) row[0]);
                result.setCode((String) row[1]);
                result.setName((String) row[2]);
                
                Map<String, Object> additionalInfo = new HashMap<>();
                additionalInfo.put("iso2_code", row[3]);
                additionalInfo.put("iso3_code", row[4]);
                result.setAdditionalInfo(additionalInfo);
                
                result.setSimilarityScore(((Number) row[5]).floatValue());
                result.setRank(((Number) row[6]).floatValue());
                
                return result;
            })
            .collect(Collectors.toList());
        
        // Get total count
        String countSql = "SELECT COUNT(*) FROM reference_data.search_countries(:query, :threshold, 1000)";
        Query countQuery = entityManager.createNativeQuery(countSql);
        countQuery.setParameter("query", query);
        countQuery.setParameter("threshold", similarityThreshold != null ? similarityThreshold : 0.3f);
        
        Long totalCount = ((Number) countQuery.getSingleResult()).longValue();
        
        return new PageImpl<>(searchResults, pageable, totalCount);
    }
    
    @Override
    public Page<SearchResult> searchPorts(String query, String countryCode, 
                                         Float similarityThreshold, Pageable pageable) {
        String sql = "SELECT * FROM reference_data.search_ports(:query, :countryCode, :threshold, :limit) " +
                    "OFFSET :offset";
        
        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("query", query);
        nativeQuery.setParameter("countryCode", countryCode);
        nativeQuery.setParameter("threshold", similarityThreshold != null ? similarityThreshold : 0.3f);
        nativeQuery.setParameter("limit", pageable.getPageSize());
        nativeQuery.setParameter("offset", pageable.getOffset());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = nativeQuery.getResultList();
        
        List<SearchResult> searchResults = results.stream()
            .map(row -> {
                SearchResult result = new SearchResult();
                result.setEntityType("port");
                result.setEntityId((UUID) row[0]);
                result.setCode((String) row[1]);
                result.setName((String) row[2]);
                
                Map<String, Object> additionalInfo = new HashMap<>();
                additionalInfo.put("city", row[3]);
                additionalInfo.put("country_code", row[4]);
                result.setAdditionalInfo(additionalInfo);
                
                result.setSimilarityScore(((Number) row[5]).floatValue());
                result.setRank(((Number) row[6]).floatValue());
                
                return result;
            })
            .collect(Collectors.toList());
        
        // Get total count
        String countSql = "SELECT COUNT(*) FROM reference_data.search_ports(:query, :countryCode, :threshold, 1000)";
        Query countQuery = entityManager.createNativeQuery(countSql);
        countQuery.setParameter("query", query);
        countQuery.setParameter("countryCode", countryCode);
        countQuery.setParameter("threshold", similarityThreshold != null ? similarityThreshold : 0.3f);
        
        Long totalCount = ((Number) countQuery.getSingleResult()).longValue();
        
        return new PageImpl<>(searchResults, pageable, totalCount);
    }
    
    @Override
    public Page<SearchResult> searchAirports(String query, Float similarityThreshold, 
                                            Pageable pageable) {
        // Since we don't have a specific airports search function yet, use universal search
        return universalSearch(query, Arrays.asList("airports"), similarityThreshold, pageable);
    }
    
    @Override
    public List<AutocompleteResult> autocomplete(String prefix, String dataType, int limit) {
        if (limit <= 0) {
            limit = 10;
        }
        
        String sql = "SELECT * FROM reference_data.autocomplete(:prefix, :dataType, :limit)";
        
        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("prefix", prefix);
        nativeQuery.setParameter("dataType", dataType != null ? dataType : "countries");
        nativeQuery.setParameter("limit", limit);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = nativeQuery.getResultList();
        
        return results.stream()
            .map(row -> {
                AutocompleteResult result = new AutocompleteResult();
                result.setCode((String) row[0]);
                result.setName((String) row[1]);
                result.setMatchType((String) row[2]);
                result.setEntityType(dataType);
                return result;
            })
            .collect(Collectors.toList());
    }
    
    private SearchResult mapToSearchResult(Object[] row) {
        SearchResult result = new SearchResult();
        result.setEntityType((String) row[0]);
        result.setEntityId((UUID) row[1]);
        result.setCode((String) row[2]);
        result.setName((String) row[3]);
        
        // Parse JSONB additional_info
        if (row[4] != null) {
            // The additional_info comes as a string representation of JSON
            // In a real implementation, you might want to use a JSON parser
            result.setAdditionalInfo(new HashMap<>());
        }
        
        result.setSimilarityScore(((Number) row[5]).floatValue());
        result.setRank(((Number) row[6]).floatValue());
        
        return result;
    }
}