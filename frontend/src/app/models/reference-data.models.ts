// Generic Reference Data Models

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface BaseReferenceDataItem {
  id: string;
  isActive: boolean;
  validFrom: string;
  validTo?: string;
  recordedAt: string;
  recordedBy: string;
  version: number;
  createdAt?: string;
  updatedAt?: string;
}

// Generic Reference Data Type Definition
export interface ReferenceDataType {
  id: string;
  name: string;
  displayName: string;
  description: string;
  icon: string;
  category: string;
  isEnabled: boolean;
  permissions: {
    read: boolean;
    create: boolean;
    update: boolean;
    delete: boolean;
  };
  fields: ReferenceDataField[];
  systemCodes: string[];
}

export interface ReferenceDataField {
  name: string;
  displayName: string;
  type: 'string' | 'number' | 'boolean' | 'date' | 'select' | 'multiselect';
  required: boolean;
  maxLength?: number;
  pattern?: string;
  options?: { value: string; label: string }[];
  description?: string;
  searchable: boolean;
  sortable: boolean;
  showInList: boolean;
}

// Specific Reference Data Types

export interface CountryDto extends BaseReferenceDataItem {
  countryCode: string;
  countryName: string;
  iso2Code: string;
  iso3Code: string;
  numericCode: string;
  codeSystem: string;
}

export interface OrganizationDto extends BaseReferenceDataItem {
  organizationCode: string;
  organizationName: string;
  organizationType: string;
  parentOrganizationId?: string;
  address?: Address;
  contactInfo?: ContactInfo;
  codeSystem: string;
}

export interface LocationDto extends BaseReferenceDataItem {
  locationCode: string;
  locationName: string;
  locationType: 'PORT' | 'AIRPORT' | 'BORDER_CROSSING' | 'WAREHOUSE' | 'OTHER';
  countryCode: string;
  stateProvinceCode?: string;
  cityName?: string;
  coordinates?: Coordinates;
  timeZone?: string;
  codeSystem: string;
}

export interface ProductDto extends BaseReferenceDataItem {
  productCode: string;
  productName: string;
  category: string;
  subcategory?: string;
  harmonizedCode?: string;
  unitOfMeasure?: string;
  description?: string;
  codeSystem: string;
}

export interface AirportDto extends BaseReferenceDataItem {
  iataCode: string;
  icaoCode: string;
  airportName: string;
  city: string;
  countryCode: string;
  stateProvince?: string;
  airportType: 'INTERNATIONAL' | 'DOMESTIC' | 'REGIONAL' | 'MILITARY' | 'PRIVATE';
  hubSize: 'LARGE_HUB' | 'MEDIUM_HUB' | 'SMALL_HUB' | 'NON_HUB';
  coordinates?: Coordinates;
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
}

// Supporting Types

export interface Address {
  street1: string;
  street2?: string;
  city: string;
  stateProvince?: string;
  postalCode?: string;
  countryCode: string;
}

export interface ContactInfo {
  email?: string;
  phone?: string;
  fax?: string;
  website?: string;
}

export interface Coordinates {
  latitude: number;
  longitude: number;
}

// Change Request Types

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

// Translation Types

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

// Search and Filter Types

export interface SearchParams {
  query?: string;
  page?: number;
  size?: number;
  sort?: string;
  filters?: { [key: string]: any };
}

export interface FilterOption {
  field: string;
  operator: 'eq' | 'ne' | 'like' | 'in' | 'between' | 'gt' | 'lt' | 'gte' | 'lte';
  value: any;
}

// API Response Types

export interface ApiError {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  traceId: string;
  timestamp: string;
  errors?: { [key: string]: string[] };
}

// Dashboard Statistics

export interface DashboardStats {
  referenceDataTypes: Array<{
    type: string;
    total: number;
    active: number;
    lastUpdated: string;
  }>;
  changeRequests: {
    pending: number;
    approved: number;
    rejected: number;
  };
  systemHealth: {
    status: 'UP' | 'DOWN' | 'DEGRADED';
    components: { [key: string]: { status: string; details?: any } };
  };
  recentActivity: Array<{
    id: string;
    type: string;
    description: string;
    user: string;
    timestamp: string;
  }>;
}

// Export Configuration

export interface ExportRequest {
  dataTypes: string[];
  format: 'CSV' | 'JSON' | 'XML' | 'EXCEL';
  includeHistory: boolean;
  includeInactive: boolean;
  dateRange?: {
    start: string;
    end: string;
  };
  filters?: FilterOption[];
}

export interface ImportRequest {
  dataType: string;
  format: 'CSV' | 'JSON' | 'XML' | 'EXCEL';
  file: File;
  options: {
    skipHeader: boolean;
    validateOnly: boolean;
    updateExisting: boolean;
    batchSize: number;
  };
}

// User and Permission Types

export interface User {
  id: string;
  username: string;
  name: string;
  email: string;
  role: string;
  permissions: string[];
  lastLogin?: string;
  isActive: boolean;
}

export interface Permission {
  id: string;
  name: string;
  description: string;
  resource: string;
  action: string;
}

export interface Role {
  id: string;
  name: string;
  description: string;
  permissions: Permission[];
}