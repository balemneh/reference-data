import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, of, forkJoin } from 'rxjs';
import { map, catchError, debounceTime, distinctUntilChanged, switchMap, shareReplay, startWith } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ReferenceDataService } from './reference-data.service';
import { 
  SearchParams, 
  PagedResponse, 
  CountryDto, 
  OrganizationDto, 
  LocationDto, 
  ProductDto,
  ChangeRequestDto,
  BaseReferenceDataItem 
} from '../models/reference-data.models';

// Search-specific models
export interface SearchResult {
  id: string;
  title: string;
  description?: string;
  type: SearchResultType;
  category: SearchCategory;
  path: string;
  icon: string;
  highlights?: string[];
  confidence?: number;
  metadata?: any;
}

export interface SearchSuggestion {
  id: string;
  type: 'recent' | 'popular' | 'suggestion';
  title: string;
  path: string;
  icon: string;
  timestamp?: Date;
  category?: SearchCategory;
}

export interface SearchHistory {
  query: string;
  timestamp: Date;
  results: number;
}

export type SearchResultType = 
  | 'country' 
  | 'port' 
  | 'airport' 
  | 'organization' 
  | 'product' 
  | 'change_request' 
  | 'activity' 
  | 'page' 
  | 'help' 
  | 'setting';

export type SearchCategory = 
  | 'reference_data' 
  | 'workflow' 
  | 'administration' 
  | 'help' 
  | 'navigation';

export interface GlobalSearchRequest {
  query: string;
  types?: SearchResultType[];
  limit?: number;
  includeInactive?: boolean;
  fuzzySearch?: boolean;
}

export interface GlobalSearchResponse {
  results: SearchResult[];
  totalResults: number;
  categories: { [key in SearchCategory]: number };
  query: string;
  searchTime: number;
  suggestions?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private baseUrl = environment.apiUrl;
  private readonly SEARCH_HISTORY_KEY = 'cbp_search_history';
  private readonly SEARCH_CACHE_KEY = 'cbp_search_cache';
  private readonly MAX_HISTORY_ITEMS = 10;
  private readonly CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
  
  private searchHistorySubject = new BehaviorSubject<SearchHistory[]>([]);
  private searchCache = new Map<string, { data: GlobalSearchResponse; timestamp: number }>();
  
  // Observable streams
  readonly searchHistory$ = this.searchHistorySubject.asObservable();
  
  constructor(
    private http: HttpClient,
    private referenceDataService: ReferenceDataService
  ) {
    this.loadSearchHistory();
  }

  // Global Search
  
  searchGlobal(request: GlobalSearchRequest): Observable<GlobalSearchResponse> {
    const cacheKey = this.getCacheKey(request);
    const cached = this.getFromCache(cacheKey);
    
    if (cached) {
      return of(cached);
    }
    
    const startTime = Date.now();
    
    return this.performGlobalSearch(request).pipe(
      map(response => {
        response.searchTime = Date.now() - startTime;
        this.setCache(cacheKey, response);
        this.addToHistory(request.query, response.totalResults);
        return response;
      }),
      shareReplay(1)
    );
  }
  
  private performGlobalSearch(request: GlobalSearchRequest): Observable<GlobalSearchResponse> {
    const { query, types, limit = 50, includeInactive = false, fuzzySearch = true } = request;
    
    if (!query.trim()) {
      return of({
        results: [],
        totalResults: 0,
        categories: {} as { [key in SearchCategory]: number },
        query,
        searchTime: 0
      });
    }
    
    // Prepare search requests for different data types
    const searchRequests: Observable<SearchResult[]>[] = [];
    
    // Reference data searches
    if (!types || types.includes('country')) {
      searchRequests.push(this.searchCountries(query, fuzzySearch, includeInactive));
    }
    
    if (!types || types.includes('port')) {
      searchRequests.push(this.searchPorts(query, fuzzySearch, includeInactive));
    }
    
    if (!types || types.includes('airport')) {
      searchRequests.push(this.searchAirports(query, fuzzySearch, includeInactive));
    }
    
    
    if (!types || types.includes('organization')) {
      searchRequests.push(this.searchOrganizations(query, fuzzySearch, includeInactive));
    }
    
    if (!types || types.includes('product')) {
      searchRequests.push(this.searchProducts(query, fuzzySearch, includeInactive));
    }
    
    // Workflow searches
    if (!types || types.includes('change_request')) {
      searchRequests.push(this.searchChangeRequests(query));
    }
    
    // System searches
    if (!types || types.includes('page')) {
      searchRequests.push(this.searchPages(query));
    }
    
    if (!types || types.includes('help')) {
      searchRequests.push(this.searchHelpDocuments(query));
    }
    
    if (!types || types.includes('setting')) {
      searchRequests.push(this.searchSettings(query));
    }
    
    // Execute all searches in parallel
    return forkJoin(searchRequests).pipe(
      map(resultArrays => {
        const allResults = resultArrays.flat();
        
        // Sort by relevance/confidence
        const sortedResults = this.sortSearchResults(allResults, query);
        
        // Apply limit
        const limitedResults = sortedResults.slice(0, limit);
        
        // Calculate categories
        const categories = this.calculateCategories(allResults);
        
        return {
          results: limitedResults,
          totalResults: allResults.length,
          categories,
          query,
          searchTime: 0, // Will be set by caller
          suggestions: this.generateSearchSuggestions(query, allResults)
        };
      }),
      catchError(error => {
        console.error('Global search failed:', error);
        return of({
          results: [],
          totalResults: 0,
          categories: {} as { [key in SearchCategory]: number },
          query,
          searchTime: 0
        });
      })
    );
  }
  
  // Individual search methods
  
  private searchCountries(query: string, fuzzy: boolean, includeInactive: boolean): Observable<SearchResult[]> {
    const searchParams: SearchParams = {
      query: fuzzy ? this.fuzzyQuery(query) : query,
      size: 20
    };
    
    return this.referenceDataService.searchItems<CountryDto>('countries', searchParams).pipe(
      map(response => response.content.map(country => ({
        id: country.id,
        title: country.countryName,
        description: `${country.countryCode} - ${country.codeSystem}`,
        type: 'country' as SearchResultType,
        category: 'reference_data' as SearchCategory,
        path: `/countries/${country.id}`,
        icon: 'public',
        confidence: this.calculateConfidence(query, [country.countryName, country.countryCode]),
        metadata: {
          countryCode: country.countryCode,
          iso2Code: country.iso2Code,
          iso3Code: country.iso3Code,
          isActive: country.isActive
        }
      }))),
      catchError(() => of([]))
    );
  }
  
  private searchPorts(query: string, fuzzy: boolean, includeInactive: boolean): Observable<SearchResult[]> {
    const searchParams: SearchParams = {
      query: fuzzy ? this.fuzzyQuery(query) : query,
      size: 20,
      filters: { locationType: 'PORT' }
    };
    
    return this.referenceDataService.searchItems<LocationDto>('locations', searchParams).pipe(
      map(response => response.content.map(port => ({
        id: port.id,
        title: port.locationName,
        description: `${port.locationCode} - ${port.countryCode}`,
        type: 'port' as SearchResultType,
        category: 'reference_data' as SearchCategory,
        path: `/ports/${port.id}`,
        icon: 'anchor',
        confidence: this.calculateConfidence(query, [port.locationName, port.locationCode]),
        metadata: {
          locationCode: port.locationCode,
          countryCode: port.countryCode,
          locationType: port.locationType,
          isActive: port.isActive
        }
      }))),
      catchError(() => of([]))
    );
  }
  
  private searchAirports(query: string, fuzzy: boolean, includeInactive: boolean): Observable<SearchResult[]> {
    const searchParams: SearchParams = {
      query: fuzzy ? this.fuzzyQuery(query) : query,
      size: 20,
      filters: { locationType: 'AIRPORT' }
    };
    
    return this.referenceDataService.searchItems<LocationDto>('locations', searchParams).pipe(
      map(response => response.content.map(airport => ({
        id: airport.id,
        title: airport.locationName,
        description: `${airport.locationCode} - ${airport.countryCode}`,
        type: 'airport' as SearchResultType,
        category: 'reference_data' as SearchCategory,
        path: `/airports/${airport.id}`,
        icon: 'flight',
        confidence: this.calculateConfidence(query, [airport.locationName, airport.locationCode]),
        metadata: {
          locationCode: airport.locationCode,
          countryCode: airport.countryCode,
          locationType: airport.locationType,
          isActive: airport.isActive
        }
      }))),
      catchError(() => of([]))
    );
  }
  
  private searchOrganizations(query: string, fuzzy: boolean, includeInactive: boolean): Observable<SearchResult[]> {
    const searchParams: SearchParams = {
      query: fuzzy ? this.fuzzyQuery(query) : query,
      size: 20
    };
    
    return this.referenceDataService.searchItems<OrganizationDto>('organizations', searchParams).pipe(
      map(response => response.content.map(org => ({
        id: org.id,
        title: org.organizationName,
        description: `${org.organizationCode} - ${org.organizationType}`,
        type: 'organization' as SearchResultType,
        category: 'reference_data' as SearchCategory,
        path: `/organizations/${org.id}`,
        icon: 'business',
        confidence: this.calculateConfidence(query, [org.organizationName, org.organizationCode]),
        metadata: {
          organizationCode: org.organizationCode,
          organizationType: org.organizationType,
          isActive: org.isActive
        }
      }))),
      catchError(() => of([]))
    );
  }
  
  private searchProducts(query: string, fuzzy: boolean, includeInactive: boolean): Observable<SearchResult[]> {
    const searchParams: SearchParams = {
      query: fuzzy ? this.fuzzyQuery(query) : query,
      size: 20
    };
    
    return this.referenceDataService.searchItems<ProductDto>('products', searchParams).pipe(
      map(response => response.content.map(product => ({
        id: product.id,
        title: product.productName,
        description: `${product.productCode} - ${product.category}`,
        type: 'product' as SearchResultType,
        category: 'reference_data' as SearchCategory,
        path: `/products/${product.id}`,
        icon: 'inventory',
        confidence: this.calculateConfidence(query, [product.productName, product.productCode]),
        metadata: {
          productCode: product.productCode,
          category: product.category,
          harmonizedCode: product.harmonizedCode,
          isActive: product.isActive
        }
      }))),
      catchError(() => of([]))
    );
  }
  
  private searchChangeRequests(query: string): Observable<SearchResult[]> {
    return this.referenceDataService.getChangeRequests({ page: 0, size: 20 }).pipe(
      map(response => 
        response.content
          .filter(cr => 
            cr.description.toLowerCase().includes(query.toLowerCase()) ||
            cr.entityType.toLowerCase().includes(query.toLowerCase()) ||
            cr.status.toLowerCase().includes(query.toLowerCase())
          )
          .map(cr => ({
            id: cr.id,
            title: `${cr.changeType} ${cr.entityType}`,
            description: cr.description,
            type: 'change_request' as SearchResultType,
            category: 'workflow' as SearchCategory,
            path: `/change-requests/${cr.id}`,
            icon: 'assignment',
            confidence: this.calculateConfidence(query, [cr.description, cr.entityType]),
            metadata: {
              changeType: cr.changeType,
              status: cr.status,
              requestedBy: cr.requestedBy,
              requestedAt: cr.requestedAt
            }
          }))
      ),
      catchError(() => of([]))
    );
  }
  
  private searchPages(query: string): Observable<SearchResult[]> {
    const pages = [
      { title: 'Dashboard', path: '/dashboard', icon: 'dashboard', description: 'System overview and statistics' },
      { title: 'Countries', path: '/countries', icon: 'public', description: 'Country reference data' },
      { title: 'Ports', path: '/ports', icon: 'anchor', description: 'Port reference data' },
      { title: 'Airports', path: '/airports', icon: 'flight', description: 'Airport reference data' },
      { title: 'Organizations', path: '/organizations', icon: 'business', description: 'Organization reference data' },
      { title: 'Products', path: '/products', icon: 'inventory', description: 'Product reference data' },
      { title: 'Change Requests', path: '/change-requests', icon: 'assignment', description: 'Workflow management' },
      { title: 'Activity Log', path: '/activity-log', icon: 'history', description: 'System activity and audit trail' },
      { title: 'Reports', path: '/reports', icon: 'description', description: 'Reports and analytics' },
      { title: 'Settings', path: '/settings', icon: 'settings', description: 'System configuration' },
      { title: 'Users', path: '/users', icon: 'people', description: 'User management' }
    ];
    
    return of(
      pages
        .filter(page => 
          page.title.toLowerCase().includes(query.toLowerCase()) ||
          page.description.toLowerCase().includes(query.toLowerCase())
        )
        .map(page => ({
          id: `page_${page.path}`,
          title: page.title,
          description: page.description,
          type: 'page' as SearchResultType,
          category: 'navigation' as SearchCategory,
          path: page.path,
          icon: page.icon,
          confidence: this.calculateConfidence(query, [page.title, page.description])
        }))
    );
  }
  
  private searchHelpDocuments(query: string): Observable<SearchResult[]> {
    const helpDocs = [
      { 
        title: 'User Guide', 
        path: '/help/user-guide', 
        icon: 'help_outline', 
        description: 'Complete user guide and documentation',
        content: 'user guide documentation help tutorial getting started reference data countries ports airports'
      },
      { 
        title: 'API Documentation', 
        path: '/help/api-docs', 
        icon: 'code', 
        description: 'REST API documentation and examples',
        content: 'api documentation rest endpoints swagger openapi integration development'
      },
      { 
        title: 'Tutorials', 
        path: '/help/tutorials', 
        icon: 'school', 
        description: 'Step-by-step tutorials',
        content: 'tutorials walkthrough examples training videos step-by-step'
      }
    ];
    
    return of(
      helpDocs
        .filter(doc => 
          doc.title.toLowerCase().includes(query.toLowerCase()) ||
          doc.description.toLowerCase().includes(query.toLowerCase()) ||
          doc.content.toLowerCase().includes(query.toLowerCase())
        )
        .map(doc => ({
          id: `help_${doc.path}`,
          title: doc.title,
          description: doc.description,
          type: 'help' as SearchResultType,
          category: 'help' as SearchCategory,
          path: doc.path,
          icon: doc.icon,
          confidence: this.calculateConfidence(query, [doc.title, doc.description, doc.content])
        }))
    );
  }
  
  private searchSettings(query: string): Observable<SearchResult[]> {
    const settings = [
      { 
        title: 'System Settings', 
        path: '/settings', 
        icon: 'settings', 
        description: 'General system configuration',
        keywords: 'system configuration general settings preferences'
      },
      { 
        title: 'User Management', 
        path: '/users', 
        icon: 'people', 
        description: 'Manage users and permissions',
        keywords: 'users permissions roles access control security'
      },
      { 
        title: 'Data Import/Export', 
        path: '/data-management', 
        icon: 'import_export', 
        description: 'Data import and export tools',
        keywords: 'import export data management bulk upload download'
      }
    ];
    
    return of(
      settings
        .filter(setting => 
          setting.title.toLowerCase().includes(query.toLowerCase()) ||
          setting.description.toLowerCase().includes(query.toLowerCase()) ||
          setting.keywords.toLowerCase().includes(query.toLowerCase())
        )
        .map(setting => ({
          id: `setting_${setting.path}`,
          title: setting.title,
          description: setting.description,
          type: 'setting' as SearchResultType,
          category: 'administration' as SearchCategory,
          path: setting.path,
          icon: setting.icon,
          confidence: this.calculateConfidence(query, [setting.title, setting.description, setting.keywords])
        }))
    );
  }
  
  // Search Suggestions
  
  getSearchSuggestions(): Observable<SearchSuggestion[]> {
    const recentSearches = this.getRecentSearches();
    const popularPages = this.getPopularPages();
    
    return of([...recentSearches, ...popularPages]);
  }
  
  private getRecentSearches(): SearchSuggestion[] {
    return this.searchHistorySubject.value
      .slice(0, 5)
      .map(history => ({
        id: `recent_${history.query}`,
        type: 'recent' as const,
        title: history.query,
        path: `/search?q=${encodeURIComponent(history.query)}`,
        icon: 'history',
        timestamp: history.timestamp
      }));
  }
  
  private getPopularPages(): SearchSuggestion[] {
    return [
      { id: 'popular_countries', type: 'popular', title: 'Countries', path: '/countries', icon: 'public', category: 'reference_data' },
      { id: 'popular_change_requests', type: 'popular', title: 'Change Requests', path: '/change-requests', icon: 'assignment', category: 'workflow' },
      { id: 'popular_dashboard', type: 'popular', title: 'Dashboard', path: '/dashboard', icon: 'dashboard', category: 'navigation' },
      { id: 'popular_ports', type: 'popular', title: 'Ports', path: '/ports', icon: 'anchor', category: 'reference_data' }
    ];
  }
  
  // Search History Management
  
  addToHistory(query: string, resultCount: number): void {
    if (!query.trim()) return;
    
    const history = this.searchHistorySubject.value;
    const newEntry: SearchHistory = {
      query: query.trim(),
      timestamp: new Date(),
      results: resultCount
    };
    
    // Remove duplicate if exists
    const filtered = history.filter(h => h.query !== newEntry.query);
    
    // Add new entry at the beginning
    const updated = [newEntry, ...filtered].slice(0, this.MAX_HISTORY_ITEMS);
    
    this.searchHistorySubject.next(updated);
    this.saveSearchHistory(updated);
  }
  
  clearSearchHistory(): void {
    this.searchHistorySubject.next([]);
    localStorage.removeItem(this.SEARCH_HISTORY_KEY);
  }
  
  removeFromHistory(query: string): void {
    const history = this.searchHistorySubject.value;
    const updated = history.filter(h => h.query !== query);
    this.searchHistorySubject.next(updated);
    this.saveSearchHistory(updated);
  }
  
  private loadSearchHistory(): void {
    try {
      const stored = localStorage.getItem(this.SEARCH_HISTORY_KEY);
      if (stored) {
        const history: SearchHistory[] = JSON.parse(stored);
        // Convert string dates back to Date objects
        history.forEach(h => h.timestamp = new Date(h.timestamp));
        this.searchHistorySubject.next(history);
      }
    } catch (error) {
      console.warn('Failed to load search history:', error);
    }
  }
  
  private saveSearchHistory(history: SearchHistory[]): void {
    try {
      localStorage.setItem(this.SEARCH_HISTORY_KEY, JSON.stringify(history));
    } catch (error) {
      console.warn('Failed to save search history:', error);
    }
  }
  
  // Utility Methods
  
  private fuzzyQuery(query: string): string {
    // Simple fuzzy search implementation
    // Add wildcard characters for partial matches
    return query.split(' ').map(term => `${term}*`).join(' ');
  }
  
  private calculateConfidence(query: string, searchFields: string[]): number {
    const queryLower = query.toLowerCase();
    let maxConfidence = 0;
    
    searchFields.forEach(field => {
      if (!field) return;
      
      const fieldLower = field.toLowerCase();
      
      // Exact match
      if (fieldLower === queryLower) {
        maxConfidence = Math.max(maxConfidence, 1.0);
      }
      // Starts with
      else if (fieldLower.startsWith(queryLower)) {
        maxConfidence = Math.max(maxConfidence, 0.9);
      }
      // Contains
      else if (fieldLower.includes(queryLower)) {
        maxConfidence = Math.max(maxConfidence, 0.7);
      }
      // Word boundary match
      else if (fieldLower.includes(' ' + queryLower)) {
        maxConfidence = Math.max(maxConfidence, 0.8);
      }
    });
    
    return maxConfidence;
  }
  
  private sortSearchResults(results: SearchResult[], query: string): SearchResult[] {
    return results.sort((a, b) => {
      // Sort by confidence first
      const confidenceDiff = (b.confidence || 0) - (a.confidence || 0);
      if (confidenceDiff !== 0) return confidenceDiff;
      
      // Then by type priority
      const typePriority = this.getTypePriority();
      const aPriority = typePriority[a.type] || 999;
      const bPriority = typePriority[b.type] || 999;
      
      if (aPriority !== bPriority) return aPriority - bPriority;
      
      // Finally by title alphabetically
      return a.title.localeCompare(b.title);
    });
  }
  
  private getTypePriority(): { [key in SearchResultType]: number } {
    return {
      'page': 1,
      'country': 2,
      'port': 3,
      'airport': 4,
      'organization': 6,
      'product': 7,
      'change_request': 8,
      'activity': 9,
      'help': 10,
      'setting': 11
    };
  }
  
  private calculateCategories(results: SearchResult[]): { [key in SearchCategory]: number } {
    const categories = {
      reference_data: 0,
      workflow: 0,
      administration: 0,
      help: 0,
      navigation: 0
    } as { [key in SearchCategory]: number };
    
    results.forEach(result => {
      categories[result.category] = (categories[result.category] || 0) + 1;
    });
    
    return categories;
  }
  
  private generateSearchSuggestions(query: string, results: SearchResult[]): string[] {
    // Simple suggestion generation based on results
    const suggestions = new Set<string>();
    
    results.forEach(result => {
      // Add title words as suggestions
      result.title.split(' ').forEach(word => {
        if (word.length > 2 && !query.toLowerCase().includes(word.toLowerCase())) {
          suggestions.add(word);
        }
      });
      
      // Add description words as suggestions
      if (result.description) {
        result.description.split(' ').forEach(word => {
          if (word.length > 2 && !query.toLowerCase().includes(word.toLowerCase())) {
            suggestions.add(word);
          }
        });
      }
    });
    
    return Array.from(suggestions).slice(0, 5);
  }
  
  // Cache Management
  
  private getCacheKey(request: GlobalSearchRequest): string {
    return JSON.stringify({
      query: request.query.toLowerCase().trim(),
      types: request.types?.sort(),
      limit: request.limit,
      includeInactive: request.includeInactive,
      fuzzySearch: request.fuzzySearch
    });
  }
  
  private getFromCache(key: string): GlobalSearchResponse | null {
    const cached = this.searchCache.get(key);
    if (!cached) return null;
    
    if (Date.now() - cached.timestamp > this.CACHE_DURATION) {
      this.searchCache.delete(key);
      return null;
    }
    
    return cached.data;
  }
  
  private setCache(key: string, data: GlobalSearchResponse): void {
    // Clean old cache entries
    if (this.searchCache.size > 100) {
      const oldEntries = Array.from(this.searchCache.entries())
        .filter(([, value]) => Date.now() - value.timestamp > this.CACHE_DURATION)
        .map(([key]) => key);
      
      oldEntries.forEach(key => this.searchCache.delete(key));
    }
    
    this.searchCache.set(key, {
      data,
      timestamp: Date.now()
    });
  }
  
  clearCache(): void {
    this.searchCache.clear();
  }
}