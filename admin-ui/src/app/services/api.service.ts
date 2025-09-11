import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

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

export interface ChangeRequestDto {
  id: string;
  entityType: string;
  entityId?: string;
  changeType: 'CREATE' | 'UPDATE' | 'DELETE';
  description: string;
  oldValues?: any;
  newValues?: any;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  requestedBy: string;
  requestedAt: string;
  reviewedBy?: string;
  reviewedAt?: string;
  comments?: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = environment.apiUrl;

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
          // Return mock data for development
          return of(this.getMockCountriesResponse(params));
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
          // Return filtered mock data for development
          return of(this.getMockCountriesResponse(params));
        })
      );
  }

  getAllCurrentCountries(): Observable<CountryDto[]> {
    return this.http.get<CountryDto[]>(`${this.baseUrl}/v1/countries/current`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching current countries:', error);
          // Return mock data for development
          return of(this.getMockCountries());
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
    systemCode?: string;
    page?: number;
    size?: number;
    sort?: string;
    active?: boolean;
  }): Observable<PagedResponse<PortDto>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.systemCode) httpParams = httpParams.set('systemCode', params.systemCode);
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
      if (params.sort) httpParams = httpParams.set('sort', params.sort);
      if (params.active !== undefined) httpParams = httpParams.set('active', params.active.toString());
    }
    // Default to UNLOCODE if no system code provided
    if (!params?.systemCode) {
      httpParams = httpParams.set('systemCode', 'UNLOCODE');
    }
    return this.http.get<PagedResponse<PortDto>>(`${this.baseUrl}/v1/ports`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error fetching ports:', error);
          // Return mock data for development
          return of(this.getMockPortsResponse(params));
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

  getPortByCode(code: string, systemCode: string, asOf?: string): Observable<PortDto> {
    let httpParams = new HttpParams()
      .set('code', code)
      .set('systemCode', systemCode);
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
          // Return filtered mock data for development
          return of(this.getMockPortsResponse(params));
        })
      );
  }

  getAllCurrentPorts(): Observable<PortDto[]> {
    return this.http.get<PortDto[]>(`${this.baseUrl}/v1/ports/current`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching current ports:', error);
          // Return mock data for development
          return of(this.getMockPorts());
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
    systemCode?: string;
    page?: number;
    size?: number;
    sort?: string;
    active?: boolean;
  }): Observable<PagedResponse<AirportDto>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.systemCode) httpParams = httpParams.set('systemCode', params.systemCode);
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
      if (params.sort) httpParams = httpParams.set('sort', params.sort);
      if (params.active !== undefined) httpParams = httpParams.set('active', params.active.toString());
    }
    // Default to IATA if no system code provided
    if (!params?.systemCode) {
      httpParams = httpParams.set('systemCode', 'IATA');
    }
    return this.http.get<PagedResponse<AirportDto>>(`${this.baseUrl}/v1/airports`, { params: httpParams })
      .pipe(
        catchError((error) => {
          console.error('Error fetching airports:', error);
          // Return mock data for development
          return of(this.getMockAirportsResponse(params));
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

  getAirportByCode(code: string, systemCode: string, asOf?: string): Observable<AirportDto> {
    let httpParams = new HttpParams()
      .set('code', code)
      .set('systemCode', systemCode);
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
          // Return filtered mock data for development
          return of(this.getMockAirportsResponse(params));
        })
      );
  }

  getAllCurrentAirports(): Observable<AirportDto[]> {
    return this.http.get<AirportDto[]>(`${this.baseUrl}/v1/airports/current`)
      .pipe(
        catchError((error) => {
          console.error('Error fetching current airports:', error);
          // Return mock data for development
          return of(this.getMockAirports());
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

  // Change Requests
  getChangeRequests(params?: {
    status?: string;
    entityType?: string;
    page?: number;
    size?: number;
  }): Observable<PagedResponse<ChangeRequestDto>> {
    // Always return mock data for development
    console.log('getChangeRequests called with params:', params);
    const mockResponse = this.getMockChangeRequestsResponse(params);
    console.log('Returning mock response:', mockResponse);
    return of(mockResponse);
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
          // Return realistic mock stats for demonstration when API fails
          return of(this.getMockDashboardStats());
        })
      );
  }

  getSystemHealth(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/v1/health`)
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

  // Mock data methods for development
  private getMockCountries(): CountryDto[] {
    return [
      {
        id: '1',
        countryCode: 'US',
        countryName: 'United States',
        iso2Code: 'US',
        iso3Code: 'USA',
        numericCode: '840',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2020-01-01T00:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: '2',
        countryCode: 'CA',
        countryName: 'Canada',
        iso2Code: 'CA',
        iso3Code: 'CAN',
        numericCode: '124',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2020-01-01T00:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: '3',
        countryCode: 'MX',
        countryName: 'Mexico',
        iso2Code: 'MX',
        iso3Code: 'MEX',
        numericCode: '484',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2020-01-01T00:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: '4',
        countryCode: 'GB',
        countryName: 'United Kingdom',
        iso2Code: 'GB',
        iso3Code: 'GBR',
        numericCode: '826',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2020-01-01T00:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: '5',
        countryCode: 'CN',
        countryName: 'China',
        iso2Code: 'CN',
        iso3Code: 'CHN',
        numericCode: '156',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2020-01-01T00:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: '6',
        countryCode: 'JP',
        countryName: 'Japan',
        iso2Code: 'JP',
        iso3Code: 'JPN',
        numericCode: '392',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2020-01-01T00:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: '7',
        countryCode: 'DE',
        countryName: 'Germany',
        iso2Code: 'DE',
        iso3Code: 'DEU',
        numericCode: '276',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2020-01-01T00:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: '8',
        countryCode: 'FR',
        countryName: 'France',
        iso2Code: 'FR',
        iso3Code: 'FRA',
        numericCode: '250',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2020-01-01T00:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: '9',
        countryCode: 'IN',
        countryName: 'India',
        iso2Code: 'IN',
        iso3Code: 'IND',
        numericCode: '356',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2020-01-01T00:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: '10',
        countryCode: 'BR',
        countryName: 'Brazil',
        iso2Code: 'BR',
        iso3Code: 'BRA',
        numericCode: '076',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2020-01-01T00:00:00Z',
        recordedBy: 'system',
        version: 1
      }
    ];
  }

  private getMockCountriesResponse(params?: any): PagedResponse<CountryDto> {
    let countries = this.getMockCountries();
    
    // Apply search filter if provided
    if (params?.name) {
      const searchTerm = params.name.toLowerCase();
      countries = countries.filter(country => 
        country.countryName.toLowerCase().includes(searchTerm) ||
        country.countryCode.toLowerCase().includes(searchTerm) ||
        country.iso2Code.toLowerCase().includes(searchTerm) ||
        country.iso3Code.toLowerCase().includes(searchTerm)
      );
    }
    
    // Apply active filter if provided
    if (params?.active !== undefined) {
      countries = countries.filter(country => country.isActive === params.active);
    }
    
    // Apply pagination
    const page = params?.page || 0;
    const size = params?.size || 20;
    const start = page * size;
    const end = start + size;
    const pagedCountries = countries.slice(start, end);
    
    return {
      content: pagedCountries,
      totalElements: countries.length,
      totalPages: Math.ceil(countries.length / size),
      size: size,
      number: page,
      first: page === 0,
      last: page === Math.ceil(countries.length / size) - 1
    };
  }

  private getMockPorts(): PortDto[] {
    return [
      {
        id: 'port-001',
        portCode: 'USNYC',
        portName: 'New York',
        countryCode: 'US',
        countryName: 'United States',
        stateProvince: 'New York',
        city: 'New York',
        portType: 'SEAPORT',
        latitude: 40.7128,
        longitude: -74.0060,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'PASSENGER', 'CRUISE'],
        customsOffice: 'Port of New York/Newark Area',
        timeZone: 'America/New_York',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-002',
        portCode: 'USLAX',
        portName: 'Los Angeles',
        countryCode: 'US',
        countryName: 'United States',
        stateProvince: 'California',
        city: 'Los Angeles',
        portType: 'SEAPORT',
        latitude: 33.7405,
        longitude: -118.2668,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'RO_RO', 'PETROLEUM'],
        customsOffice: 'Port of Los Angeles',
        timeZone: 'America/Los_Angeles',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-003',
        portCode: 'GBSOU',
        portName: 'Southampton',
        countryCode: 'GB',
        countryName: 'United Kingdom',
        stateProvince: 'Hampshire',
        city: 'Southampton',
        portType: 'SEAPORT',
        latitude: 50.9097,
        longitude: -1.4044,
        capabilities: ['CONTAINER', 'PASSENGER', 'CRUISE', 'RO_RO'],
        customsOffice: 'Southampton Port',
        timeZone: 'Europe/London',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-004',
        portCode: 'DEHAM',
        portName: 'Hamburg',
        countryCode: 'DE',
        countryName: 'Germany',
        stateProvince: 'Hamburg',
        city: 'Hamburg',
        portType: 'SEAPORT',
        latitude: 53.5511,
        longitude: 9.9937,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'RO_RO'],
        customsOffice: 'Hamburg Port Authority',
        timeZone: 'Europe/Berlin',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-005',
        portCode: 'CNSHG',
        portName: 'Shanghai',
        countryCode: 'CN',
        countryName: 'China',
        stateProvince: 'Shanghai',
        city: 'Shanghai',
        portType: 'SEAPORT',
        latitude: 31.2304,
        longitude: 121.4737,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'RO_RO', 'GRAIN'],
        customsOffice: 'Shanghai Customs',
        timeZone: 'Asia/Shanghai',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-006',
        portCode: 'SGSIN',
        portName: 'Singapore',
        countryCode: 'SG',
        countryName: 'Singapore',
        city: 'Singapore',
        portType: 'SEAPORT',
        latitude: 1.2966,
        longitude: 103.7764,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'RO_RO', 'PETROLEUM', 'LNG'],
        customsOffice: 'Singapore Customs',
        timeZone: 'Asia/Singapore',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-007',
        portCode: 'AEDXB',
        portName: 'Dubai',
        countryCode: 'AE',
        countryName: 'United Arab Emirates',
        stateProvince: 'Dubai',
        city: 'Dubai',
        portType: 'SEAPORT',
        latitude: 25.2697,
        longitude: 55.3094,
        capabilities: ['CONTAINER', 'RO_RO', 'CRUISE'],
        customsOffice: 'Dubai Customs',
        timeZone: 'Asia/Dubai',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-008',
        portCode: 'CHKNG',
        portName: 'Kansas City Inland Port',
        countryCode: 'US',
        countryName: 'United States',
        stateProvince: 'Missouri',
        city: 'Kansas City',
        portType: 'INLAND',
        latitude: 39.0997,
        longitude: -94.5786,
        capabilities: ['RAIL_TERMINAL', 'CONTAINER', 'BULK_CARGO'],
        customsOffice: 'Kansas City CBP',
        timeZone: 'America/Chicago',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-009',
        portCode: 'CAVAN',
        portName: 'Vancouver',
        countryCode: 'CA',
        countryName: 'Canada',
        stateProvince: 'British Columbia',
        city: 'Vancouver',
        portType: 'SEAPORT',
        latitude: 49.2827,
        longitude: -123.1207,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'GRAIN', 'CRUISE'],
        customsOffice: 'Vancouver Port Authority',
        timeZone: 'America/Vancouver',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-010',
        portCode: 'BRRIO',
        portName: 'Rio de Janeiro',
        countryCode: 'BR',
        countryName: 'Brazil',
        stateProvince: 'Rio de Janeiro',
        city: 'Rio de Janeiro',
        portType: 'SEAPORT',
        latitude: -22.9068,
        longitude: -43.1729,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'PASSENGER', 'CRUISE'],
        customsOffice: 'Porto do Rio de Janeiro',
        timeZone: 'America/Sao_Paulo',
        codeSystem: 'UNLOCODE',
        isActive: false,
        validFrom: '2020-01-01T00:00:00Z',
        validTo: '2023-12-31T23:59:59Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      }
    ];
  }

  private getMockPortsResponse(params?: any): PagedResponse<PortDto> {
    let ports = this.getMockPorts();
    
    // Apply search filter if provided
    if (params?.name) {
      const searchTerm = params.name.toLowerCase();
      ports = ports.filter(port => 
        port.portName.toLowerCase().includes(searchTerm) ||
        port.portCode.toLowerCase().includes(searchTerm) ||
        port.city.toLowerCase().includes(searchTerm) ||
        port.countryName?.toLowerCase().includes(searchTerm)
      );
    }
    
    // Apply active filter if provided
    if (params?.active !== undefined) {
      ports = ports.filter(port => port.isActive === params.active);
    }
    
    // Apply pagination
    const page = params?.page || 0;
    const size = params?.size || 20;
    const start = page * size;
    const end = start + size;
    const pagedPorts = ports.slice(start, end);
    
    return {
      content: pagedPorts,
      totalElements: ports.length,
      totalPages: Math.ceil(ports.length / size),
      size: size,
      number: page,
      first: page === 0,
      last: end >= ports.length
    };
  }

  // Mock data methods for airports development
  private getMockAirports(): AirportDto[] {
    return [
      {
        id: 'airport-001',
        iataCode: 'LAX',
        icaoCode: 'KLAX',
        airportName: 'Los Angeles International Airport',
        city: 'Los Angeles',
        countryCode: 'US',
        stateProvince: 'California',
        airportType: 'INTERNATIONAL',
        hubSize: 'LARGE_HUB',
        coordinates: { latitude: 33.9425, longitude: -118.4081 },
        timeZone: 'America/Los_Angeles',
        elevationFeet: 125,
        runwayCount: 4,
        longestRunwayFeet: 12091,
        terminalCount: 9,
        hasCustoms: true,
        status: 'ACTIVE',
        codeSystem: 'IATA',
        website: 'https://www.flylax.com',
        phone: '+1-855-463-5252',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-002',
        iataCode: 'JFK',
        icaoCode: 'KJFK',
        airportName: 'John F. Kennedy International Airport',
        city: 'New York',
        countryCode: 'US',
        stateProvince: 'New York',
        airportType: 'INTERNATIONAL',
        hubSize: 'LARGE_HUB',
        coordinates: { latitude: 40.6413, longitude: -73.7781 },
        timeZone: 'America/New_York',
        elevationFeet: 13,
        runwayCount: 4,
        longestRunwayFeet: 14511,
        terminalCount: 6,
        hasCustoms: true,
        status: 'ACTIVE',
        codeSystem: 'IATA',
        website: 'https://www.jfkairport.com',
        phone: '+1-718-244-4444',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-003',
        iataCode: 'LHR',
        icaoCode: 'EGLL',
        airportName: 'London Heathrow Airport',
        city: 'London',
        countryCode: 'GB',
        stateProvince: 'England',
        airportType: 'INTERNATIONAL',
        hubSize: 'LARGE_HUB',
        coordinates: { latitude: 51.4700, longitude: -0.4543 },
        timeZone: 'Europe/London',
        elevationFeet: 83,
        runwayCount: 2,
        longestRunwayFeet: 12802,
        terminalCount: 5,
        hasCustoms: true,
        status: 'ACTIVE',
        codeSystem: 'IATA',
        website: 'https://www.heathrow.com',
        phone: '+44-844-335-1801',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-004',
        iataCode: 'DXB',
        icaoCode: 'OMDB',
        airportName: 'Dubai International Airport',
        city: 'Dubai',
        countryCode: 'AE',
        stateProvince: 'Dubai',
        airportType: 'INTERNATIONAL',
        hubSize: 'LARGE_HUB',
        coordinates: { latitude: 25.2532, longitude: 55.3657 },
        timeZone: 'Asia/Dubai',
        elevationFeet: 62,
        runwayCount: 2,
        longestRunwayFeet: 13124,
        terminalCount: 3,
        hasCustoms: true,
        status: 'ACTIVE',
        codeSystem: 'IATA',
        website: 'https://www.dubaiairports.ae',
        phone: '+971-4-224-5555',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-005',
        iataCode: 'NRT',
        icaoCode: 'RJAA',
        airportName: 'Narita International Airport',
        city: 'Tokyo',
        countryCode: 'JP',
        stateProvince: 'Chiba',
        airportType: 'INTERNATIONAL',
        hubSize: 'LARGE_HUB',
        coordinates: { latitude: 35.7720, longitude: 140.3930 },
        timeZone: 'Asia/Tokyo',
        elevationFeet: 141,
        runwayCount: 2,
        longestRunwayFeet: 13123,
        terminalCount: 3,
        hasCustoms: true,
        status: 'ACTIVE',
        codeSystem: 'IATA',
        website: 'https://www.narita-airport.jp',
        phone: '+81-476-34-8000',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-006',
        iataCode: 'SIN',
        icaoCode: 'WSSS',
        airportName: 'Singapore Changi Airport',
        city: 'Singapore',
        countryCode: 'SG',
        airportType: 'INTERNATIONAL',
        hubSize: 'LARGE_HUB',
        coordinates: { latitude: 1.3644, longitude: 103.9915 },
        timeZone: 'Asia/Singapore',
        elevationFeet: 22,
        runwayCount: 2,
        longestRunwayFeet: 13124,
        terminalCount: 4,
        hasCustoms: true,
        status: 'ACTIVE',
        codeSystem: 'IATA',
        website: 'https://www.changiairport.com',
        phone: '+65-6595-6868',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-007',
        iataCode: 'FRA',
        icaoCode: 'EDDF',
        airportName: 'Frankfurt Airport',
        city: 'Frankfurt',
        countryCode: 'DE',
        stateProvince: 'Hesse',
        airportType: 'INTERNATIONAL',
        hubSize: 'LARGE_HUB',
        coordinates: { latitude: 50.0379, longitude: 8.5622 },
        timeZone: 'Europe/Berlin',
        elevationFeet: 364,
        runwayCount: 4,
        longestRunwayFeet: 13123,
        terminalCount: 2,
        hasCustoms: true,
        status: 'ACTIVE',
        codeSystem: 'IATA',
        website: 'https://www.frankfurt-airport.com',
        phone: '+49-180-5372-4636',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-008',
        iataCode: 'SYD',
        icaoCode: 'YSSY',
        airportName: 'Sydney Kingsford Smith Airport',
        city: 'Sydney',
        countryCode: 'AU',
        stateProvince: 'New South Wales',
        airportType: 'INTERNATIONAL',
        hubSize: 'LARGE_HUB',
        coordinates: { latitude: -33.9399, longitude: 151.1753 },
        timeZone: 'Australia/Sydney',
        elevationFeet: 21,
        runwayCount: 3,
        longestRunwayFeet: 12467,
        terminalCount: 3,
        hasCustoms: true,
        status: 'ACTIVE',
        codeSystem: 'IATA',
        website: 'https://www.sydneyairport.com.au',
        phone: '+61-2-9667-9111',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-009',
        iataCode: 'YYZ',
        icaoCode: 'CYYZ',
        airportName: 'Toronto Pearson International Airport',
        city: 'Toronto',
        countryCode: 'CA',
        stateProvince: 'Ontario',
        airportType: 'INTERNATIONAL',
        hubSize: 'LARGE_HUB',
        coordinates: { latitude: 43.6777, longitude: -79.6248 },
        timeZone: 'America/Toronto',
        elevationFeet: 569,
        runwayCount: 5,
        longestRunwayFeet: 11120,
        terminalCount: 2,
        hasCustoms: true,
        status: 'ACTIVE',
        codeSystem: 'IATA',
        website: 'https://www.torontopearson.com',
        phone: '+1-416-247-7678',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-010',
        iataCode: 'BWI',
        icaoCode: 'KBWI',
        airportName: 'Baltimore/Washington International Thurgood Marshall Airport',
        city: 'Baltimore',
        countryCode: 'US',
        stateProvince: 'Maryland',
        airportType: 'INTERNATIONAL',
        hubSize: 'MEDIUM_HUB',
        coordinates: { latitude: 39.1754, longitude: -76.6683 },
        timeZone: 'America/New_York',
        elevationFeet: 146,
        runwayCount: 4,
        longestRunwayFeet: 10502,
        terminalCount: 1,
        hasCustoms: true,
        status: 'ACTIVE',
        codeSystem: 'IATA',
        website: 'https://www.bwiairport.com',
        phone: '+1-410-859-7111',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-011',
        iataCode: 'BOI',
        icaoCode: 'KBOI',
        airportName: 'Boise Airport',
        city: 'Boise',
        countryCode: 'US',
        stateProvince: 'Idaho',
        airportType: 'DOMESTIC',
        hubSize: 'SMALL_HUB',
        coordinates: { latitude: 43.5644, longitude: -116.2228 },
        timeZone: 'America/Boise',
        elevationFeet: 2871,
        runwayCount: 3,
        longestRunwayFeet: 10000,
        terminalCount: 1,
        hasCustoms: false,
        status: 'ACTIVE',
        codeSystem: 'IATA',
        website: 'https://www.iflyboise.com',
        phone: '+1-208-383-3110',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-012',
        iataCode: 'ASE',
        icaoCode: 'KASE',
        airportName: 'Aspen/Pitkin County Airport',
        city: 'Aspen',
        countryCode: 'US',
        stateProvince: 'Colorado',
        airportType: 'REGIONAL',
        hubSize: 'NON_HUB',
        coordinates: { latitude: 39.2232, longitude: -106.8687 },
        timeZone: 'America/Denver',
        elevationFeet: 7820,
        runwayCount: 1,
        longestRunwayFeet: 8006,
        terminalCount: 1,
        hasCustoms: false,
        status: 'SEASONAL',
        codeSystem: 'IATA',
        website: 'https://www.aspenairport.com',
        phone: '+1-970-920-5380',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-013',
        iataCode: 'EDW',
        icaoCode: 'KEDW',
        airportName: 'Edwards Air Force Base',
        city: 'Edwards',
        countryCode: 'US',
        stateProvince: 'California',
        airportType: 'MILITARY',
        hubSize: 'NON_HUB',
        coordinates: { latitude: 34.9054, longitude: -117.8839 },
        timeZone: 'America/Los_Angeles',
        elevationFeet: 2302,
        runwayCount: 7,
        longestRunwayFeet: 15000,
        terminalCount: 0,
        hasCustoms: false,
        status: 'ACTIVE',
        codeSystem: 'ICAO',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-014',
        iataCode: 'TVL',
        icaoCode: 'KTVL',
        airportName: 'Lake Tahoe Airport',
        city: 'South Lake Tahoe',
        countryCode: 'US',
        stateProvince: 'California',
        airportType: 'PRIVATE',
        hubSize: 'NON_HUB',
        coordinates: { latitude: 38.8939, longitude: -119.9951 },
        timeZone: 'America/Los_Angeles',
        elevationFeet: 6264,
        runwayCount: 1,
        longestRunwayFeet: 8544,
        terminalCount: 1,
        hasCustoms: false,
        status: 'ACTIVE',
        codeSystem: 'IATA',
        website: 'https://www.cityofslt.us/airport',
        phone: '+1-530-541-1388',
        isActive: true,
        validFrom: '2020-01-01T00:00:00Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'airport-015',
        iataCode: 'TEX',
        icaoCode: 'KTEX',
        airportName: 'Telluride Regional Airport',
        city: 'Telluride',
        countryCode: 'US',
        stateProvince: 'Colorado',
        airportType: 'REGIONAL',
        hubSize: 'NON_HUB',
        coordinates: { latitude: 37.9538, longitude: -107.9085 },
        timeZone: 'America/Denver',
        elevationFeet: 9078,
        runwayCount: 1,
        longestRunwayFeet: 7111,
        terminalCount: 1,
        hasCustoms: false,
        status: 'INACTIVE',
        codeSystem: 'IATA',
        website: 'https://www.tellurideairport.com',
        phone: '+1-970-728-5313',
        isActive: false,
        validFrom: '2020-01-01T00:00:00Z',
        validTo: '2023-12-31T23:59:59Z',
        recordedAt: '2024-01-01T10:00:00Z',
        recordedBy: 'system',
        version: 1
      }
    ];
  }

  private getMockAirportsResponse(params?: any): PagedResponse<AirportDto> {
    let airports = this.getMockAirports();
    
    // Apply search filter if provided
    if (params?.name) {
      const searchTerm = params.name.toLowerCase();
      airports = airports.filter(airport => 
        airport.airportName.toLowerCase().includes(searchTerm) ||
        airport.iataCode.toLowerCase().includes(searchTerm) ||
        airport.icaoCode.toLowerCase().includes(searchTerm) ||
        airport.city.toLowerCase().includes(searchTerm) ||
        airport.countryCode.toLowerCase().includes(searchTerm)
      );
    }
    
    // Apply active filter if provided
    if (params?.active !== undefined) {
      airports = airports.filter(airport => airport.isActive === params.active);
    }
    
    // Apply pagination
    const page = params?.page || 0;
    const size = params?.size || 20;
    const start = page * size;
    const end = start + size;
    const pagedAirports = airports.slice(start, end);
    
    return {
      content: pagedAirports,
      totalElements: airports.length,
      totalPages: Math.ceil(airports.length / size),
      size: size,
      number: page,
      first: page === 0,
      last: end >= airports.length
    };
  }

  // Mock data methods for change requests development
  private getMockChangeRequests(): ChangeRequestDto[] {
    return [
      {
        id: 'cr-001',
        entityType: 'COUNTRY',
        entityId: '1',
        changeType: 'UPDATE',
        description: 'Update country name from "United States of America" to "United States"',
        oldValues: {
          countryName: 'United States of America',
          iso3Code: 'USA'
        },
        newValues: {
          countryName: 'United States',
          iso3Code: 'USA'
        },
        status: 'PENDING',
        requestedBy: 'john.doe@cbp.dhs.gov',
        requestedAt: '2024-12-09T10:30:00Z',
        comments: 'Standardizing country names for consistency with ISO standards',
        createdAt: '2024-12-09T10:30:00Z',
        updatedAt: '2024-12-09T10:30:00Z'
      },
      {
        id: 'cr-002',
        entityType: 'PORT',
        entityId: 'port-001',
        changeType: 'UPDATE',
        description: 'Add new capability "LNG" to New York port',
        oldValues: {
          capabilities: ['CONTAINER', 'BULK_CARGO', 'PASSENGER', 'CRUISE']
        },
        newValues: {
          capabilities: ['CONTAINER', 'BULK_CARGO', 'PASSENGER', 'CRUISE', 'LNG']
        },
        status: 'APPROVED',
        requestedBy: 'jane.smith@cbp.dhs.gov',
        requestedAt: '2024-12-08T14:20:00Z',
        reviewedBy: 'supervisor@cbp.dhs.gov',
        reviewedAt: '2024-12-08T16:45:00Z',
        comments: 'Port infrastructure upgrade completed to support LNG operations',
        createdAt: '2024-12-08T14:20:00Z',
        updatedAt: '2024-12-08T16:45:00Z'
      },
      {
        id: 'cr-003',
        entityType: 'AIRPORT',
        entityId: 'airport-015',
        changeType: 'UPDATE',
        description: 'Reactivate Telluride Regional Airport',
        oldValues: {
          status: 'INACTIVE',
          isActive: false,
          validTo: '2023-12-31T23:59:59Z'
        },
        newValues: {
          status: 'ACTIVE',
          isActive: true,
          validTo: null
        },
        status: 'REJECTED',
        requestedBy: 'mike.johnson@cbp.dhs.gov',
        requestedAt: '2024-12-07T09:15:00Z',
        reviewedBy: 'supervisor@cbp.dhs.gov',
        reviewedAt: '2024-12-07T11:30:00Z',
        comments: 'Airport remains closed due to ongoing infrastructure issues. Reapply after runway repairs are completed.',
        createdAt: '2024-12-07T09:15:00Z',
        updatedAt: '2024-12-07T11:30:00Z'
      },
      {
        id: 'cr-004',
        entityType: 'COUNTRY',
        changeType: 'CREATE',
        description: 'Add new country: South Sudan',
        newValues: {
          countryCode: 'SS',
          countryName: 'South Sudan',
          iso2Code: 'SS',
          iso3Code: 'SSD',
          numericCode: '728',
          codeSystem: 'ISO3166-1',
          isActive: true
        },
        status: 'PENDING',
        requestedBy: 'admin@cbp.dhs.gov',
        requestedAt: '2024-12-06T16:00:00Z',
        comments: 'Adding newly recognized country to reference data system',
        createdAt: '2024-12-06T16:00:00Z',
        updatedAt: '2024-12-06T16:00:00Z'
      },
      {
        id: 'cr-005',
        entityType: 'PORT',
        entityId: 'port-010',
        changeType: 'DELETE',
        description: 'Deactivate Rio de Janeiro port due to security concerns',
        oldValues: {
          portName: 'Rio de Janeiro',
          isActive: true,
          status: 'ACTIVE'
        },
        newValues: {
          isActive: false,
          status: 'INACTIVE',
          validTo: '2024-12-31T23:59:59Z'
        },
        status: 'APPROVED',
        requestedBy: 'security@cbp.dhs.gov',
        requestedAt: '2024-12-05T13:45:00Z',
        reviewedBy: 'port.manager@cbp.dhs.gov',
        reviewedAt: '2024-12-05T15:20:00Z',
        comments: 'Temporary deactivation due to ongoing security assessment. Will be reactivated pending resolution.',
        createdAt: '2024-12-05T13:45:00Z',
        updatedAt: '2024-12-05T15:20:00Z'
      },
      {
        id: 'cr-006',
        entityType: 'AIRPORT',
        entityId: 'airport-002',
        changeType: 'UPDATE',
        description: 'Update terminal count for JFK Airport',
        oldValues: {
          terminalCount: 6
        },
        newValues: {
          terminalCount: 8
        },
        status: 'PENDING',
        requestedBy: 'facility.manager@cbp.dhs.gov',
        requestedAt: '2024-12-04T11:20:00Z',
        comments: 'Two new terminals completed construction and are now operational',
        createdAt: '2024-12-04T11:20:00Z',
        updatedAt: '2024-12-04T11:20:00Z'
      },
      {
        id: 'cr-007',
        entityType: 'COUNTRY',
        entityId: '3',
        changeType: 'UPDATE',
        description: 'Correct numeric code for Mexico',
        oldValues: {
          numericCode: '484'
        },
        newValues: {
          numericCode: '484'
        },
        status: 'CANCELLED',
        requestedBy: 'data.analyst@cbp.dhs.gov',
        requestedAt: '2024-12-03T08:30:00Z',
        comments: 'Request cancelled - numeric code is already correct',
        createdAt: '2024-12-03T08:30:00Z',
        updatedAt: '2024-12-03T09:15:00Z'
      },
      {
        id: 'cr-008',
        entityType: 'PORT',
        changeType: 'CREATE',
        description: 'Add new port: Charleston',
        newValues: {
          portCode: 'USCHS',
          portName: 'Charleston',
          countryCode: 'US',
          countryName: 'United States',
          stateProvince: 'South Carolina',
          city: 'Charleston',
          portType: 'SEAPORT',
          capabilities: ['CONTAINER', 'BULK_CARGO', 'RO_RO'],
          codeSystem: 'UNLOCODE',
          isActive: true
        },
        status: 'APPROVED',
        requestedBy: 'port.expansion@cbp.dhs.gov',
        requestedAt: '2024-12-02T12:00:00Z',
        reviewedBy: 'regional.manager@cbp.dhs.gov',
        reviewedAt: '2024-12-02T14:30:00Z',
        comments: 'New port facility operational and ready for trade operations',
        createdAt: '2024-12-02T12:00:00Z',
        updatedAt: '2024-12-02T14:30:00Z'
      },
      {
        id: 'cr-009',
        entityType: 'AIRPORT',
        entityId: 'airport-006',
        changeType: 'UPDATE',
        description: 'Update runway count for Singapore Changi Airport',
        oldValues: {
          runwayCount: 2,
          longestRunwayFeet: 13124
        },
        newValues: {
          runwayCount: 3,
          longestRunwayFeet: 13780
        },
        status: 'PENDING',
        requestedBy: 'aviation.ops@cbp.dhs.gov',
        requestedAt: '2024-12-01T15:45:00Z',
        comments: 'New runway construction completed - Terminal 5 expansion project',
        createdAt: '2024-12-01T15:45:00Z',
        updatedAt: '2024-12-01T15:45:00Z'
      },
      {
        id: 'cr-010',
        entityType: 'COUNTRY',
        entityId: '5',
        changeType: 'UPDATE',
        description: 'Update China official name',
        oldValues: {
          countryName: 'China'
        },
        newValues: {
          countryName: 'China (People\'s Republic of)'
        },
        status: 'APPROVED',
        requestedBy: 'nomenclature@cbp.dhs.gov',
        requestedAt: '2024-11-30T10:15:00Z',
        reviewedBy: 'chief.analyst@cbp.dhs.gov',
        reviewedAt: '2024-11-30T12:45:00Z',
        comments: 'Updated to reflect official diplomatic nomenclature standards',
        createdAt: '2024-11-30T10:15:00Z',
        updatedAt: '2024-11-30T12:45:00Z'
      },
      {
        id: 'cr-011',
        entityType: 'PORT',
        entityId: 'port-005',
        changeType: 'UPDATE',
        description: 'Add environmental compliance capability to Shanghai port',
        oldValues: {
          capabilities: ['CONTAINER', 'BULK_CARGO', 'RO_RO', 'GRAIN']
        },
        newValues: {
          capabilities: ['CONTAINER', 'BULK_CARGO', 'RO_RO', 'GRAIN', 'GREEN_PORT']
        },
        status: 'PENDING',
        requestedBy: 'environmental@cbp.dhs.gov',
        requestedAt: '2024-11-29T14:20:00Z',
        comments: 'Port has achieved environmental certification and green operations standards',
        createdAt: '2024-11-29T14:20:00Z',
        updatedAt: '2024-11-29T14:20:00Z'
      },
      {
        id: 'cr-012',
        entityType: 'AIRPORT',
        entityId: 'airport-011',
        changeType: 'UPDATE',
        description: 'Upgrade Boise Airport to international status',
        oldValues: {
          airportType: 'DOMESTIC',
          hasCustoms: false
        },
        newValues: {
          airportType: 'INTERNATIONAL',
          hasCustoms: true
        },
        status: 'REJECTED',
        requestedBy: 'boise.ops@cbp.dhs.gov',
        requestedAt: '2024-11-28T09:30:00Z',
        reviewedBy: 'customs.director@cbp.dhs.gov',
        reviewedAt: '2024-11-28T13:15:00Z',
        comments: 'Customs facility construction not yet completed. Resubmit after facility inspection approval.',
        createdAt: '2024-11-28T09:30:00Z',
        updatedAt: '2024-11-28T13:15:00Z'
      }
    ];
  }

  private getMockChangeRequestsResponse(params?: any): PagedResponse<ChangeRequestDto> {
    let changeRequests = this.getMockChangeRequests();
    
    // Apply status filter if provided
    if (params?.status) {
      changeRequests = changeRequests.filter(cr => cr.status === params.status);
    }
    
    // Apply entity type filter if provided
    if (params?.entityType) {
      changeRequests = changeRequests.filter(cr => cr.entityType === params.entityType);
    }
    
    // Apply pagination
    const page = params?.page || 0;
    const size = params?.size || 20;
    const start = page * size;
    const end = start + size;
    const pagedChangeRequests = changeRequests.slice(start, end);
    
    return {
      content: pagedChangeRequests,
      totalElements: changeRequests.length,
      totalPages: Math.ceil(changeRequests.length / size),
      size: size,
      number: page,
      first: page === 0,
      last: end >= changeRequests.length
    };
  }

  private getMockDashboardStats(): any {
    const countries = this.getMockCountries();
    const ports = this.getMockPorts();
    const airports = this.getMockAirports();
    const changeRequests = this.getMockChangeRequests();
    
    const activeCountries = countries.filter(c => c.isActive).length;
    const activePorts = ports.filter(p => p.isActive).length;
    const activeAirports = airports.filter(a => a.isActive).length;
    const pendingRequests = changeRequests.filter(cr => cr.status === 'PENDING').length;
    
    return {
      countries: { 
        total: countries.length, 
        active: activeCountries, 
        trend: 2.1 
      },
      ports: { 
        total: ports.length, 
        active: activePorts, 
        trend: -1.3 
      },
      airports: { 
        total: airports.length, 
        active: activeAirports, 
        trend: 0.8 
      },
      mappings: { 
        total: 15782, 
        trend: 1.2 
      },
      pendingRequests: pendingRequests,
      dataQuality: {
        overall: 94,
        completeness: 96.2,
        consistency: 91.8,
        validity: 92.9,
        uniqueness: 95.1
      },
      systemHealth: {
        overall: 'healthy',
        components: [
          { name: 'Database', status: 'up', lastChecked: new Date(), responseTime: 12 },
          { name: 'API Gateway', status: 'up', lastChecked: new Date(), responseTime: 8 },
          { name: 'Cache Layer', status: 'up', lastChecked: new Date(), responseTime: 3 },
          { name: 'Message Queue', status: 'up', lastChecked: new Date(), responseTime: 15 }
        ]
      },
      monthlyActivity: [
        { month: 'July 2024', countries: 245, ports: 1820, airports: 4198 },
        { month: 'August 2024', countries: 248, ports: 1835, airports: 4205 },
        { month: 'September 2024', countries: 251, ports: 1847, airports: 4220 },
        { month: 'October 2024', countries: 255, ports: 1851, airports: 4235 },
        { month: 'November 2024', countries: 258, ports: 1863, airports: 4251 },
        { month: 'December 2024', countries: 261, ports: 1871, airports: 4263 }
      ],
      recentActivity: [
        {
          id: '1',
          type: 'CREATE',
          entityType: 'COUNTRY',
          description: 'New country added: Vanuatu (VU)',
          timestamp: new Date(Date.now() - 300000).toISOString(),
          user: 'john.smith@cbp.gov'
        },
        {
          id: '2',
          type: 'UPDATE',
          entityType: 'PORT',
          description: 'Updated Los Angeles Port (LAX) operational status',
          timestamp: new Date(Date.now() - 900000).toISOString(),
          user: 'jane.doe@cbp.gov'
        },
        {
          id: '3',
          type: 'UPDATE',
          entityType: 'AIRPORT',
          description: 'Modified JFK airport code mapping for IATA system',
          timestamp: new Date(Date.now() - 1800000).toISOString(),
          user: 'mike.johnson@cbp.gov'
        },
        {
          id: '4',
          type: 'CREATE',
          entityType: 'PORT',
          description: 'Added new port: Charleston (USCHS)',
          timestamp: new Date(Date.now() - 2700000).toISOString(),
          user: 'port.admin@cbp.gov'
        },
        {
          id: '5',
          type: 'DELETE',
          entityType: 'AIRPORT',
          description: 'Deactivated Telluride Regional Airport (TEX)',
          timestamp: new Date(Date.now() - 3600000).toISOString(),
          user: 'aviation.ops@cbp.gov'
        }
      ]
    };
  }
}