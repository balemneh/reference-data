import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, BehaviorSubject, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  PagedResponse,
  ReferenceDataType,
  BaseReferenceDataItem,
  CountryDto,
  OrganizationDto,
  LocationDto,
  ProductDto,
  SearchParams,
  FilterOption,
  ChangeRequestDto,
  TranslationRequest,
  TranslationResponse,
  DashboardStats,
  ExportRequest,
  ImportRequest
} from '../models/reference-data.models';

@Injectable({
  providedIn: 'root'
})
export class ReferenceDataService {
  private baseUrl = environment.apiUrl;
  private referenceDataTypesSubject = new BehaviorSubject<ReferenceDataType[]>([]);
  
  // Available reference data types
  readonly referenceDataTypes$ = this.referenceDataTypesSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadReferenceDataTypes();
  }

  // Reference Data Type Management
  
  getReferenceDataTypes(): Observable<ReferenceDataType[]> {
    return this.referenceDataTypes$;
  }

  private loadReferenceDataTypes() {
    this.http.get<ReferenceDataType[]>(`${this.baseUrl}/v1/reference-data/types`)
      .pipe(
        tap(types => this.referenceDataTypesSubject.next(types)),
        catchError(error => {
          console.error('Failed to load reference data types:', error);
          // Fallback to minimal set if API is unavailable
          const fallbackTypes: ReferenceDataType[] = [
            {
              id: 'countries',
              name: 'countries',
              displayName: 'Countries',
              description: 'Country reference data',
              icon: 'public',
              category: 'Geographic',
              isEnabled: true,
              permissions: { read: true, create: false, update: false, delete: false },
              systemCodes: ['ISO3166-1'],
              fields: [
                { name: 'countryCode', displayName: 'Country Code', type: 'string', required: true, maxLength: 5, searchable: true, sortable: true, showInList: true },
                { name: 'countryName', displayName: 'Country Name', type: 'string', required: true, maxLength: 100, searchable: true, sortable: true, showInList: true }
              ]
            }
          ];
          this.referenceDataTypesSubject.next(fallbackTypes);
          return of(fallbackTypes);
        })
      )
      .subscribe();
  }

  // Generic CRUD Operations

  getItems<T extends BaseReferenceDataItem>(
    dataType: string, 
    params?: SearchParams
  ): Observable<PagedResponse<T>> {
    let httpParams = new HttpParams();
    
    if (params) {
      if (params.query) httpParams = httpParams.set('q', params.query);
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
      if (params.sort) httpParams = httpParams.set('sort', params.sort);
      
      // Add filters
      if (params.filters) {
        Object.keys(params.filters).forEach(key => {
          const value = params.filters![key];
          if (value !== null && value !== undefined && value !== '') {
            httpParams = httpParams.set(key, value.toString());
          }
        });
      }
    }

    return this.http.get<PagedResponse<T>>(`${this.baseUrl}/v1/reference-data/${dataType}`, { params: httpParams })
      .pipe(
        catchError(error => {
          console.error(`Failed to load ${dataType}:`, error);
          // Return empty result set as fallback
          const fallbackResponse: PagedResponse<T> = {
            content: [],
            number: params?.page || 0,
            size: params?.size || 20,
            totalElements: 0,
            totalPages: 0,
            first: true,
            last: true
          };
          return of(fallbackResponse);
        })
      );
  }

  getItem<T extends BaseReferenceDataItem>(dataType: string, id: string): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}/v1/reference-data/${dataType}/${id}`)
      .pipe(
        catchError(error => {
          console.error(`Failed to load ${dataType} item ${id}:`, error);
          return throwError(() => error);
        })
      );
  }

  createItem<T extends BaseReferenceDataItem>(dataType: string, item: Partial<T>): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}/v1/reference-data/${dataType}`, item)
      .pipe(
        catchError(error => {
          console.error(`Failed to create ${dataType} item:`, error);
          return throwError(() => error);
        })
      );
  }

  updateItem<T extends BaseReferenceDataItem>(dataType: string, id: string, item: Partial<T>): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}/v1/reference-data/${dataType}/${id}`, item)
      .pipe(
        catchError(error => {
          console.error(`Failed to update ${dataType} item ${id}:`, error);
          return throwError(() => error);
        })
      );
  }

  deleteItem(dataType: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/v1/reference-data/${dataType}/${id}`)
      .pipe(
        catchError(error => {
          console.error(`Failed to delete ${dataType} item ${id}:`, error);
          return throwError(() => error);
        })
      );
  }

  // Search Operations

  searchItems<T extends BaseReferenceDataItem>(
    dataType: string,
    searchParams: SearchParams
  ): Observable<PagedResponse<T>> {
    return this.getItems<T>(dataType, searchParams);
  }

  // Specialized methods for existing data types (backward compatibility)

  getCountries(params?: SearchParams): Observable<PagedResponse<CountryDto>> {
    return this.getItems<CountryDto>('countries', params);
  }

  getOrganizations(params?: SearchParams): Observable<PagedResponse<OrganizationDto>> {
    return this.getItems<OrganizationDto>('organizations', params);
  }

  getLocations(params?: SearchParams): Observable<PagedResponse<LocationDto>> {
    return this.getItems<LocationDto>('locations', params);
  }

  getProducts(params?: SearchParams): Observable<PagedResponse<ProductDto>> {
    return this.getItems<ProductDto>('products', params);
  }

  // Change Request Operations

  getChangeRequests(params?: {
    status?: string;
    entityType?: string;
    page?: number;
    size?: number;
  }): Observable<PagedResponse<ChangeRequestDto>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.status) httpParams = httpParams.set('status', params.status);
      if (params.entityType) httpParams = httpParams.set('entityType', params.entityType);
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    }
    return this.http.get<PagedResponse<ChangeRequestDto>>(`${this.baseUrl}/v1/change-requests`, { params: httpParams })
      .pipe(
        catchError(error => {
          console.error('Failed to load change requests:', error);
          const fallbackResponse: PagedResponse<ChangeRequestDto> = {
            content: [],
            number: params?.page || 0,
            size: params?.size || 20,
            totalElements: 0,
            totalPages: 0,
            first: true,
            last: true
          };
          return of(fallbackResponse);
        })
      );
  }

  createChangeRequest(request: Partial<ChangeRequestDto>): Observable<ChangeRequestDto> {
    return this.http.post<ChangeRequestDto>(`${this.baseUrl}/v1/change-requests`, request)
      .pipe(
        catchError(error => {
          console.error('Failed to create change request:', error);
          return throwError(() => error);
        })
      );
  }

  approveChangeRequest(id: string, comments?: string): Observable<ChangeRequestDto> {
    return this.http.post<ChangeRequestDto>(`${this.baseUrl}/v1/change-requests/${id}/approve`, { comments })
      .pipe(
        catchError(error => {
          console.error(`Failed to approve change request ${id}:`, error);
          return throwError(() => error);
        })
      );
  }

  rejectChangeRequest(id: string, comments?: string): Observable<ChangeRequestDto> {
    return this.http.post<ChangeRequestDto>(`${this.baseUrl}/v1/change-requests/${id}/reject`, { comments })
      .pipe(
        catchError(error => {
          console.error(`Failed to reject change request ${id}:`, error);
          return throwError(() => error);
        })
      );
  }

  // Translation Operations

  translate(request: TranslationRequest): Observable<TranslationResponse> {
    let httpParams = new HttpParams()
      .set('fromSystem', request.fromSystem)
      .set('fromCode', request.fromCode)
      .set('toSystem', request.toSystem);
    if (request.asOf) httpParams = httpParams.set('asOf', request.asOf);
    return this.http.get<TranslationResponse>(`${this.baseUrl}/v1/translate`, { params: httpParams })
      .pipe(
        catchError(error => {
          console.error('Failed to translate code:', error);
          return throwError(() => error);
        })
      );
  }

  // Dashboard Operations

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.baseUrl}/v1/reference-data/stats`)
      .pipe(
        catchError(error => {
          console.error('Failed to load dashboard stats:', error);
          // Fallback to minimal stats if API is unavailable
          const fallbackStats: DashboardStats = {
            referenceDataTypes: [
              { type: 'countries', total: 0, active: 0, lastUpdated: new Date().toISOString() }
            ],
            changeRequests: {
              pending: 0,
              approved: 0,
              rejected: 0
            },
            systemHealth: {
              status: 'DOWN',
              components: {
                database: { status: 'DOWN' },
                redis: { status: 'DOWN' },
                kafka: { status: 'DOWN' },
                api: { status: 'DOWN' }
              }
            },
            recentActivity: []
          };
          return of(fallbackStats);
        })
      );
  }

  // Import/Export Operations

  exportData(request: ExportRequest): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/v1/export`, request, { 
      responseType: 'blob' 
    })
    .pipe(
      catchError(error => {
        console.error('Failed to export data:', error);
        return throwError(() => error);
      })
    );
  }

  importData(request: ImportRequest): Observable<any> {
    const formData = new FormData();
    formData.append('file', request.file);
    formData.append('dataType', request.dataType);
    formData.append('format', request.format);
    formData.append('options', JSON.stringify(request.options));

    return this.http.post(`${this.baseUrl}/v1/import`, formData)
      .pipe(
        catchError(error => {
          console.error('Failed to import data:', error);
          return throwError(() => error);
        })
      );
  }

  // Utility Methods

  getReferenceDataType(typeId: string): ReferenceDataType | undefined {
    return this.referenceDataTypesSubject.value.find(type => type.id === typeId);
  }

  getSystemCodesForType(typeId: string): string[] {
    const type = this.getReferenceDataType(typeId);
    return type ? type.systemCodes : [];
  }

  buildSearchParams(params: any): SearchParams {
    const searchParams: SearchParams = {};
    
    if (params.query) searchParams.query = params.query;
    if (params.page !== undefined) searchParams.page = params.page;
    if (params.size !== undefined) searchParams.size = params.size;
    if (params.sort) searchParams.sort = params.sort;
    
    // Extract filters
    const filters: { [key: string]: any } = {};
    Object.keys(params).forEach(key => {
      if (!['query', 'page', 'size', 'sort'].includes(key) && params[key] !== null && params[key] !== undefined && params[key] !== '') {
        filters[key] = params[key];
      }
    });
    
    if (Object.keys(filters).length > 0) {
      searchParams.filters = filters;
    }
    
    return searchParams;
  }
}