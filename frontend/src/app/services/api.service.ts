import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  ChangeRequestDto,
  ChangeRequestFilter,
  ChangeRequestBatchOperation,
  BatchOperationResponse,
  ChangeRequestStats,
  ApprovalHistoryEntry,
  ChangeRequestDiff
} from '../models/change-request.models';

// Re-export commonly used interfaces
export type { ChangeRequestDto } from '../models/change-request.models';

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface CountryDto {
  id: string;
  countryCode: string;
  countryName: string;
  iso2Code: string;
  iso3Code: string;
  numericCode: string;
  codeSystem: string;
  isActive: boolean;
  validFrom: string;
  validTo?: string;
  recordedAt: string;
  recordedBy: string;
  version: number;
}

export interface TranslationRequest {
  fromSystem: string;
  fromCode: string;
  toSystem: string;
  asOf?: string;
}

export interface TranslationResponse {
  fromSystem: string;
  fromCode: string;
  toSystem: string;
  toCode?: string;
  toName?: string;
  confidence: number;
  mappingRule?: string;
  validFrom: string;
  validTo?: string;
}

export interface PortDto {
  id: string;
  portCode: string;
  portName: string;
  countryCode: string;
  countryName?: string;
  stateProvince?: string;
  city: string;
  portType: string;
  latitude?: number;
  longitude?: number;
  capabilities?: string[];
  customsOffice?: string;
  timeZone?: string;
  codeSystem: string;
  isActive: boolean;
  validFrom: string;
  validTo?: string;
  recordedAt: string;
  recordedBy: string;
  version: number;
}

export interface AirportDto {
  id: string;
  iataCode: string;
  icaoCode: string;
  airportName: string;
  city: string;
  countryCode: string;
  stateProvince?: string;
  airportType: 'INTERNATIONAL' | 'DOMESTIC' | 'REGIONAL' | 'MILITARY' | 'PRIVATE';
  hubSize: 'LARGE_HUB' | 'MEDIUM_HUB' | 'SMALL_HUB' | 'NON_HUB';
  coordinates?: { latitude: number; longitude: number };
  timeZone?: string;
  elevationFeet?: number;
  runwayCount?: number;
  longestRunwayFeet?: number;
  terminalCount?: number;
  hasCustoms: boolean;
  status: 'ACTIVE' | 'INACTIVE' | 'SEASONAL' | 'CLOSED';
  codeSystem: string;
  website?: string;
  phone?: string;
  isActive: boolean;
  validFrom: string;
  validTo?: string;
  recordedAt: string;
  recordedBy: string;
  version: number;
}


@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = environment.apiUrl;
  private apiUrl = environment.apiUrl + '/api';

  constructor(private http: HttpClient) {}

  // Countries
  getCountries(params?: {
    systemCode?: string;
    page?: number;
    size?: number;
    sort?: string;
    active?: boolean;
  }): Observable<PagedResponse<CountryDto>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.systemCode) httpParams = httpParams.set('systemCode', params.systemCode);
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
      if (params.sort) httpParams = httpParams.set('sort', params.sort);
      if (params.active !== undefined) httpParams = httpParams.set('active', params.active.toString());
    }
    // Default to ISO3166-1 if no system code provided
    if (!params?.systemCode) {
      httpParams = httpParams.set('systemCode', 'ISO3166-1');
    }
    return this.http.get<PagedResponse<CountryDto>>(`${this.baseUrl}/v1/countries`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error fetching countries:', error);
          return throwError(() => error);
        })
      );
  }

  getCountry(id: string): Observable<CountryDto> {
    return this.http.get<CountryDto>(`${this.baseUrl}/v1/countries/${id}`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching country:', error);
          return throwError(() => error);
        })
      );
  }

  getCountryByCode(code: string, systemCode: string, asOf?: string): Observable<CountryDto> {
    let httpParams = new HttpParams()
      .set('code', code)
      .set('systemCode', systemCode);
    if (asOf) httpParams = httpParams.set('asOf', asOf);
    return this.http.get<CountryDto>(`${this.baseUrl}/v1/countries/by-code`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error fetching country by code:', error);
          return throwError(() => error);
        })
      );
  }

  searchCountries(params: {
    name: string;
    page?: number;
    size?: number;
  }): Observable<PagedResponse<CountryDto>> {
    let httpParams = new HttpParams().set('name', params.name);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    return this.http.get<PagedResponse<CountryDto>>(`${this.baseUrl}/v1/countries/search`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error searching countries:', error);
          return throwError(() => error);
        })
      );
  }

  getAllCurrentCountries(): Observable<CountryDto[]> {
    return this.http.get<CountryDto[]>(`${this.baseUrl}/v1/countries/current`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching current countries:', error);
          return throwError(() => error);
        })
      );
  }

  createCountry(country: Partial<CountryDto>): Observable<CountryDto> {
    return this.http.post<CountryDto>(`${this.baseUrl}/v1/countries`, country)
      .pipe(
        catchError((error) => {
          console.error('Error creating country:', error);
          return throwError(() => error);
        })
      );
  }

  updateCountry(id: string, country: Partial<CountryDto>): Observable<CountryDto> {
    return this.http.put<CountryDto>(`${this.baseUrl}/v1/countries/${id}`, country)
      .pipe(
        catchError((error) => {
          console.error('Error updating country:', error);
          return throwError(() => error);
        })
      );
  }

  deleteCountry(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/v1/countries/${id}`)
      .pipe(
        catchError((error) => {
          console.error('Error deleting country:', error);
          return throwError(() => error);
        })
      );
  }

  // Ports
  getPorts(params?: {
    codeSystem?: string;
    page?: number;
    size?: number;
    sort?: string;
    active?: boolean;
  }): Observable<PagedResponse<PortDto>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.codeSystem) httpParams = httpParams.set('codeSystem', params.codeSystem);
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
      if (params.sort) httpParams = httpParams.set('sort', params.sort);
      if (params.active !== undefined) httpParams = httpParams.set('active', params.active.toString());
    }
    // Default to UNLOCODE if no system code provided
    if (!params?.codeSystem) {
      httpParams = httpParams.set('codeSystem', 'UNLOCODE');
    }
    return this.http.get<PagedResponse<PortDto>>(`${this.baseUrl}/v1/ports`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error fetching ports:', error);
          return throwError(() => error);
        })
      );
  }

  getPort(id: string): Observable<PortDto> {
    return this.http.get<PortDto>(`${this.baseUrl}/v1/ports/${id}`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching port:', error);
          return throwError(() => error);
        })
      );
  }

  getPortByCode(code: string, codeSystem: string, asOf?: string): Observable<PortDto> {
    let httpParams = new HttpParams()
      .set('code', code)
      .set('codeSystem', codeSystem);
    if (asOf) httpParams = httpParams.set('asOf', asOf);
    return this.http.get<PortDto>(`${this.baseUrl}/v1/ports/by-code`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error fetching port by code:', error);
          return throwError(() => error);
        })
      );
  }

  searchPorts(params: {
    name: string;
    page?: number;
    size?: number;
  }): Observable<PagedResponse<PortDto>> {
    let httpParams = new HttpParams().set('name', params.name);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    return this.http.get<PagedResponse<PortDto>>(`${this.baseUrl}/v1/ports/search`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error searching ports:', error);
          return throwError(() => error);
        })
      );
  }

  getAllCurrentPorts(): Observable<PortDto[]> {
    return this.http.get<PortDto[]>(`${this.baseUrl}/v1/ports/current`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching current ports:', error);
          return throwError(() => error);
        })
      );
  }

  createPort(port: Partial<PortDto>): Observable<PortDto> {
    return this.http.post<PortDto>(`${this.baseUrl}/v1/ports`, port)
      .pipe(
        catchError((error) => {
          console.error('Error creating port:', error);
          return throwError(() => error);
        })
      );
  }

  updatePort(id: string, port: Partial<PortDto>): Observable<PortDto> {
    return this.http.put<PortDto>(`${this.baseUrl}/v1/ports/${id}`, port)
      .pipe(
        catchError((error) => {
          console.error('Error updating port:', error);
          return throwError(() => error);
        })
      );
  }

  deletePort(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/v1/ports/${id}`)
      .pipe(
        catchError((error) => {
          console.error('Error deleting port:', error);
          return throwError(() => error);
        })
      );
  }

  // Airports
  getAirports(params?: {
    codeSystem?: string;
    page?: number;
    size?: number;
    sort?: string;
    active?: boolean;
  }): Observable<PagedResponse<AirportDto>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.codeSystem) httpParams = httpParams.set('codeSystem', params.codeSystem);
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
      if (params.sort) httpParams = httpParams.set('sort', params.sort);
      if (params.active !== undefined) httpParams = httpParams.set('active', params.active.toString());
    }
    // Default to IATA if no system code provided
    if (!params?.codeSystem) {
      httpParams = httpParams.set('codeSystem', 'IATA');
    }
    return this.http.get<PagedResponse<AirportDto>>(`${this.baseUrl}/v1/airports`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error fetching airports:', error);
          return throwError(() => error);
        })
      );
  }

  getAirport(id: string): Observable<AirportDto> {
    return this.http.get<AirportDto>(`${this.baseUrl}/v1/airports/${id}`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching airport:', error);
          return throwError(() => error);
        })
      );
  }

  getAirportByCode(code: string, codeSystem: string, asOf?: string): Observable<AirportDto> {
    let httpParams = new HttpParams()
      .set('code', code)
      .set('codeSystem', codeSystem);
    if (asOf) httpParams = httpParams.set('asOf', asOf);
    return this.http.get<AirportDto>(`${this.baseUrl}/v1/airports/by-code`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error fetching airport by code:', error);
          return throwError(() => error);
        })
      );
  }

  searchAirports(params: {
    name: string;
    page?: number;
    size?: number;
  }): Observable<PagedResponse<AirportDto>> {
    let httpParams = new HttpParams().set('name', params.name);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    return this.http.get<PagedResponse<AirportDto>>(`${this.baseUrl}/v1/airports/search`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error searching airports:', error);
          return throwError(() => error);
        })
      );
  }

  getAllCurrentAirports(): Observable<AirportDto[]> {
    return this.http.get<AirportDto[]>(`${this.baseUrl}/v1/airports/current`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching current airports:', error);
          return throwError(() => error);
        })
      );
  }

  createAirport(airport: Partial<AirportDto>): Observable<AirportDto> {
    return this.http.post<AirportDto>(`${this.baseUrl}/v1/airports`, airport)
      .pipe(
        catchError((error) => {
          console.error('Error creating airport:', error);
          return throwError(() => error);
        })
      );
  }

  updateAirport(id: string, airport: Partial<AirportDto>): Observable<AirportDto> {
    return this.http.put<AirportDto>(`${this.baseUrl}/v1/airports/${id}`, airport)
      .pipe(
        catchError((error) => {
          console.error('Error updating airport:', error);
          return throwError(() => error);
        })
      );
  }

  deleteAirport(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/v1/airports/${id}`)
      .pipe(
        catchError((error) => {
          console.error('Error deleting airport:', error);
          return throwError(() => error);
        })
      );
  }

  // Change Requests - Enhanced
  getChangeRequests(params?: {
    status?: string;
    entityType?: string;
    page?: number;
    size?: number;
    priority?: string;
    changeType?: string;
    requestedBy?: string;
    dateFrom?: string;
    dateTo?: string;
    sort?: string;
  }): Observable<PagedResponse<ChangeRequestDto>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.status) httpParams = httpParams.set('status', params.status);
      if (params.entityType) httpParams = httpParams.set('entityType', params.entityType);
      if (params.priority) httpParams = httpParams.set('priority', params.priority);
      if (params.changeType) httpParams = httpParams.set('changeType', params.changeType);
      if (params.requestedBy) httpParams = httpParams.set('requestedBy', params.requestedBy);
      if (params.dateFrom) httpParams = httpParams.set('dateFrom', params.dateFrom);
      if (params.dateTo) httpParams = httpParams.set('dateTo', params.dateTo);
      if (params.sort) httpParams = httpParams.set('sort', params.sort);
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    }
    return this.http.get<PagedResponse<ChangeRequestDto>>(`${this.baseUrl}/v1/change-requests`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error fetching change requests:', error);
          return throwError(() => error);
        })
      );
  }

  // Advanced change request filtering
  getChangeRequestsFiltered(filter: ChangeRequestFilter): Observable<PagedResponse<ChangeRequestDto>> {
    let httpParams = new HttpParams();

    if (filter.status?.length) httpParams = httpParams.set('status', filter.status.join(','));
    if (filter.entityType?.length) httpParams = httpParams.set('entityType', filter.entityType.join(','));
    if (filter.priority?.length) httpParams = httpParams.set('priority', filter.priority.join(','));
    if (filter.changeType?.length) httpParams = httpParams.set('changeType', filter.changeType.join(','));
    if (filter.requestedBy) httpParams = httpParams.set('requestedBy', filter.requestedBy);
    if (filter.reviewedBy) httpParams = httpParams.set('reviewedBy', filter.reviewedBy);
    if (filter.dateFrom) httpParams = httpParams.set('dateFrom', filter.dateFrom);
    if (filter.dateTo) httpParams = httpParams.set('dateTo', filter.dateTo);
    if (filter.searchTerm) httpParams = httpParams.set('search', filter.searchTerm);
    if (filter.tags?.length) httpParams = httpParams.set('tags', filter.tags.join(','));
    if (filter.department) httpParams = httpParams.set('department', filter.department);

    return this.http.get<PagedResponse<ChangeRequestDto>>(`${this.baseUrl}/v1/change-requests/filter`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error filtering change requests:', error);
          return throwError(() => error);
        })
      );
  }

  getChangeRequest(id: string): Observable<ChangeRequestDto> {
    return this.http.get<ChangeRequestDto>(`${this.baseUrl}/v1/change-requests/${id}`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching change request:', error);
          return throwError(() => error);
        })
      );
  }

  createChangeRequest(request: Partial<ChangeRequestDto>): Observable<ChangeRequestDto> {
    return this.http.post<ChangeRequestDto>(`${this.baseUrl}/v1/change-requests`, request)
      .pipe(
        catchError((error) => {
          console.error('Error creating change request:', error);
          return throwError(() => error);
        })
      );
  }

  updateChangeRequest(id: string, request: Partial<ChangeRequestDto>): Observable<ChangeRequestDto> {
    return this.http.put<ChangeRequestDto>(`${this.baseUrl}/v1/change-requests/${id}`, request)
      .pipe(
        catchError((error) => {
          console.error('Error updating change request:', error);
          return throwError(() => error);
        })
      );
  }

  approveChangeRequest(id: string, comments?: string): Observable<ChangeRequestDto> {
    return this.http.post<ChangeRequestDto>(`${this.baseUrl}/v1/change-requests/${id}/approve`, { comments })
      .pipe(
        catchError((error) => {
          console.error('Error approving change request:', error);
          return throwError(() => error);
        })
      );
  }

  rejectChangeRequest(id: string, comments?: string): Observable<ChangeRequestDto> {
    return this.http.post<ChangeRequestDto>(`${this.baseUrl}/v1/change-requests/${id}/reject`, { comments })
      .pipe(
        catchError((error) => {
          console.error('Error rejecting change request:', error);
          return throwError(() => error);
        })
      );
  }

  // Batch operations
  batchApproveChangeRequests(operation: ChangeRequestBatchOperation): Observable<BatchOperationResponse> {
    return this.http.post<BatchOperationResponse>(`${this.baseUrl}/v1/change-requests/batch/approve`, operation)
      .pipe(
        catchError((error) => {
          console.error('Error batch approving change requests:', error);
          return throwError(() => error);
        })
      );
  }

  batchRejectChangeRequests(operation: ChangeRequestBatchOperation): Observable<BatchOperationResponse> {
    return this.http.post<BatchOperationResponse>(`${this.baseUrl}/v1/change-requests/batch/reject`, operation)
      .pipe(
        catchError((error) => {
          console.error('Error batch rejecting change requests:', error);
          return throwError(() => error);
        })
      );
  }

  batchCancelChangeRequests(operation: ChangeRequestBatchOperation): Observable<BatchOperationResponse> {
    return this.http.post<BatchOperationResponse>(`${this.baseUrl}/v1/change-requests/batch/cancel`, operation)
      .pipe(
        catchError((error) => {
          console.error('Error batch cancelling change requests:', error);
          return throwError(() => error);
        })
      );
  }

  // Statistics and analytics
  getChangeRequestStats(params?: {
    dateFrom?: string;
    dateTo?: string;
    entityType?: string;
  }): Observable<ChangeRequestStats> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.dateFrom) httpParams = httpParams.set('dateFrom', params.dateFrom);
      if (params.dateTo) httpParams = httpParams.set('dateTo', params.dateTo);
      if (params.entityType) httpParams = httpParams.set('entityType', params.entityType);
    }
    return this.http.get<ChangeRequestStats>(`${this.baseUrl}/v1/change-requests/stats`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error fetching change request stats:', error);
          // Return fallback stats if API fails
          return of({
            total: 0,
            pending: 0,
            approved: 0,
            rejected: 0,
            cancelled: 0,
            applied: 0,
            byEntityType: {},
            byPriority: {},
            byChangeType: {},
            avgProcessingTime: 0
          });
        })
      );
  }

  // Approval history
  getChangeRequestHistory(id: string): Observable<ApprovalHistoryEntry[]> {
    return this.http.get<ApprovalHistoryEntry[]>(`${this.baseUrl}/v1/change-requests/${id}/history`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching change request history:', error);
          return throwError(() => error);
        })
      );
  }

  // Diff computation
  getChangeRequestDiff(id: string): Observable<ChangeRequestDiff> {
    return this.http.get<ChangeRequestDiff>(`${this.baseUrl}/v1/change-requests/${id}/diff`)
      .pipe(
        catchError((error) => {
          console.error('Error computing change request diff:', error);
          return throwError(() => error);
        })
      );
  }

  // Export functionality
  exportChangeRequests(filter?: ChangeRequestFilter, format: 'CSV' | 'EXCEL' | 'PDF' = 'CSV'): Observable<Blob> {
    let httpParams = new HttpParams().set('format', format);

    if (filter) {
      if (filter.status?.length) httpParams = httpParams.set('status', filter.status.join(','));
      if (filter.entityType?.length) httpParams = httpParams.set('entityType', filter.entityType.join(','));
      if (filter.priority?.length) httpParams = httpParams.set('priority', filter.priority.join(','));
      if (filter.dateFrom) httpParams = httpParams.set('dateFrom', filter.dateFrom);
      if (filter.dateTo) httpParams = httpParams.set('dateTo', filter.dateTo);
    }

    return this.http.get(`${this.baseUrl}/v1/change-requests/export`, {
      params: httpParams,
      responseType: 'blob'
    }).pipe(
      catchError((error) => {
        console.error('Error exporting change requests:', error);
        return throwError(() => error);
      })
    );
  }

  // Pending approvals for current user
  getPendingApprovals(userId?: string): Observable<PagedResponse<ChangeRequestDto>> {
    let httpParams = new HttpParams().set('status', 'PENDING');
    if (userId) httpParams = httpParams.set('assignedTo', userId);

    return this.http.get<PagedResponse<ChangeRequestDto>>(`${this.baseUrl}/v1/change-requests/pending-approvals`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error fetching pending approvals:', error);
          return throwError(() => error);
        })
      );
  }

  // Overdue requests
  getOverdueRequests(): Observable<PagedResponse<ChangeRequestDto>> {
    return this.http.get<PagedResponse<ChangeRequestDto>>(`${this.baseUrl}/v1/change-requests/overdue`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching overdue requests:', error);
          return throwError(() => error);
        })
      );
  }

  // Translation
  translate(request: TranslationRequest): Observable<TranslationResponse> {
    let httpParams = new HttpParams()
      .set('fromSystem', request.fromSystem)
      .set('fromCode', request.fromCode)
      .set('toSystem', request.toSystem);
    if (request.asOf) httpParams = httpParams.set('asOf', request.asOf);
    return this.http.get<TranslationResponse>(`${this.baseUrl}/v1/translate`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error translating code:', error);
          return throwError(() => error);
        })
      );
  }

  // Dashboard Statistics
  getDatasetStats(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/v1/datasets/stats`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching dataset stats:', error);
          // Return minimal stats structure if API fails
          return of({
            countries: { total: 0, active: 0 },
            mappings: { total: 0 },
            pendingRequests: 0
          });
        })
      );
  }

  getDashboardStats(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/v1/dashboard/stats`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching dashboard stats:', error);
          return throwError(() => error);
        })
      );
  }

  getSystemHealth(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/actuator/health`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching system health:', error);
          return of({
            status: 'DOWN',
            components: {
              database: { status: 'UNKNOWN' },
              redis: { status: 'UNKNOWN' },
              kafka: { status: 'UNKNOWN' },
              api: { status: 'UNKNOWN' }
            }
          });
        })
      );
  }

  // Activity Log
  getActivityLog(params?: {
    page?: number;
    size?: number;
    startDate?: string;
    endDate?: string;
    action?: string;
    entity?: string;
    user?: string;
  }): Observable<PagedResponse<any>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
      if (params.startDate) httpParams = httpParams.set('startDate', params.startDate);
      if (params.endDate) httpParams = httpParams.set('endDate', params.endDate);
      if (params.action) httpParams = httpParams.set('action', params.action);
      if (params.entity) httpParams = httpParams.set('entity', params.entity);
      if (params.user) httpParams = httpParams.set('user', params.user);
    }
    return this.http.get<PagedResponse<any>>(`${this.baseUrl}/v1/audit-log`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error fetching activity log:', error);
          // Return empty page if API fails
          return of({
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: params?.size || 20,
            number: params?.page || 0,
            first: true,
            last: true
          });
        })
      );
  }

  // ==================== BULK IMPORT API METHODS ====================

  /**
   * Initiate bulk import by uploading a file
   */
  initiateBulkImport(formData: FormData): Observable<{batchId: string, message: string, success: boolean}> {
    return this.http.post<{batchId: string, message: string, success: boolean}>(`${this.apiUrl}/v1/bulk-import/initiate`, formData)
      .pipe(
        catchError((error) => {
          console.error('Error initiating bulk import:', error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Validate bulk import data for a specific batch
   */
  validateBulkImport(batchId: string): Observable<{
    batchId: string;
    validCount: number;
    invalidCount: number;
    warningCount: number;
    message: string;
    success: boolean;
    errors?: any[];
    warnings?: any[];
    previewData?: any[];
  }> {
    return this.http.post<{
      batchId: string;
      validCount: number;
      invalidCount: number;
      warningCount: number;
      message: string;
      success: boolean;
      errors?: any[];
      warnings?: any[];
      previewData?: any[];
    }>(`${this.apiUrl}/v1/bulk-import/validate/${batchId}`, {})
      .pipe(
        catchError((error) => {
          console.error('Error validating bulk import:', error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Process bulk import for a specific batch
   */
  processBulkImport(batchId: string, justification?: any): Observable<{
    batchId: string;
    processedCount: number;
    failedCount: number;
    message: string;
    success: boolean;
  }> {
    const body = justification ? { justification } : {};
    return this.http.post<{
      batchId: string;
      processedCount: number;
      failedCount: number;
      message: string;
      success: boolean;
    }>(`${this.apiUrl}/v1/bulk-import/process/${batchId}`, body)
      .pipe(
        catchError((error) => {
          console.error('Error processing bulk import:', error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Get bulk import status for a specific batch
   */
  getBulkImportStatus(batchId: string): Observable<{
    batchId: string;
    status: string;
    progress: number;
    totalRecords: number;
    processedCount: number;
    failedCount: number;
    validCount: number;
    invalidCount: number;
    warningCount: number;
    startTime: string;
    endTime?: string;
    errorMessage?: string;
  }> {
    return this.http.get<{
      batchId: string;
      status: string;
      progress: number;
      totalRecords: number;
      processedCount: number;
      failedCount: number;
      validCount: number;
      invalidCount: number;
      warningCount: number;
      startTime: string;
      endTime?: string;
      errorMessage?: string;
    }>(`${this.apiUrl}/v1/bulk-import/status/${batchId}`)
      .pipe(
        catchError((error) => {
          console.error('Error getting bulk import status:', error);
          return throwError(() => error);
        })
      );
  }
}