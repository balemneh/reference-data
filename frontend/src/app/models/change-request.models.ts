// Enhanced Change Request Models for Issue #7

export interface ChangeRequestDto {
  id: string;
  entityType: 'COUNTRY' | 'PORT' | 'AIRPORT' | 'CARRIER';
  entityId?: string;
  changeType: 'CREATE' | 'UPDATE' | 'DELETE';
  description: string;
  currentValues?: any;
  proposedChanges?: any;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'APPLIED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  requestedBy: string;
  requestedAt: string;
  reviewedBy?: string;
  reviewedAt?: string;
  comments?: string;
  createdAt: string;
  updatedAt: string;
  approvalHistory?: ApprovalHistoryEntry[];
  tags?: string[];
  requesterDepartment?: string;
  businessJustification?: string;
  estimatedImpact?: 'LOW' | 'MEDIUM' | 'HIGH';
  targetImplementationDate?: string;
  relatedRequests?: string[];
}

export interface ApprovalHistoryEntry {
  id: string;
  changeRequestId: string;
  action: 'SUBMITTED' | 'REVIEWED' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'APPLIED';
  performedBy: string;
  performedAt: string;
  comments?: string;
  previousStatus?: string;
  newStatus?: string;
}

export interface ChangeRequestFilter {
  status?: string[];
  entityType?: string[];
  priority?: string[];
  changeType?: string[];
  requestedBy?: string;
  reviewedBy?: string;
  dateFrom?: string;
  dateTo?: string;
  searchTerm?: string;
  tags?: string[];
  department?: string;
}

export interface ChangeRequestBatchOperation {
  action: 'APPROVE' | 'REJECT' | 'CANCEL';
  requestIds: string[];
  comments?: string;
  notifyRequesters?: boolean;
}

export interface ChangeRequestExport {
  format: 'CSV' | 'EXCEL' | 'PDF';
  includeHistory?: boolean;
  includeComments?: boolean;
  dateRange?: {
    from: string;
    to: string;
  };
  filters?: ChangeRequestFilter;
}

export interface DiffViewConfig {
  showLineNumbers: boolean;
  highlightChanges: boolean;
  collapseUnchanged: boolean;
  viewMode: 'SIDE_BY_SIDE' | 'UNIFIED';
}

export interface ChangeRequestStats {
  total: number;
  pending: number;
  approved: number;
  rejected: number;
  cancelled: number;
  applied: number;
  byEntityType: {
    [key: string]: number;
  };
  byPriority: {
    [key: string]: number;
  };
  byChangeType: {
    [key: string]: number;
  };
  avgProcessingTime: number; // in hours
  oldestPendingDate?: string;
}

export interface ChangeRequestNotification {
  id: string;
  changeRequestId: string;
  type: 'APPROVAL_REQUIRED' | 'APPROVED' | 'REJECTED' | 'APPLIED' | 'OVERDUE';
  message: string;
  recipients: string[];
  sentAt: string;
  readBy?: string[];
}

// Utility interfaces for UI components
export interface SortConfig {
  field: keyof ChangeRequestDto;
  direction: 'asc' | 'desc';
}

export interface PaginationConfig {
  currentPage: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

export interface TableColumn {
  key: keyof ChangeRequestDto;
  label: string;
  sortable: boolean;
  width?: string;
  align?: 'left' | 'center' | 'right';
  formatter?: (value: any) => string;
}

export interface FilterOption {
  value: string;
  label: string;
  count?: number;
  icon?: string;
}

// Form validation interfaces
export interface ApprovalFormData {
  action: 'APPROVE' | 'REJECT';
  comments: string;
  notifyRequester: boolean;
  urgentProcessing: boolean;
}

export interface BatchApprovalFormData {
  action: 'APPROVE' | 'REJECT' | 'CANCEL';
  comments: string;
  notifyRequesters: boolean;
  selectedIds: string[];
  confirmOverride: boolean; // for high-priority items
}

// API response interfaces
export interface ChangeRequestResponse {
  changeRequest: ChangeRequestDto;
  validationErrors?: string[];
  warnings?: string[];
  relatedImpacts?: string[];
}

export interface BatchOperationResponse {
  processedCount: number;
  failedCount: number;
  errors: {
    requestId: string;
    error: string;
  }[];
  warnings: string[];
}

// Diff computation interfaces
export interface ValueDiff {
  field: string;
  oldValue: any;
  newValue: any;
  type: 'ADDED' | 'REMOVED' | 'MODIFIED';
  path: string[];
}

export interface ChangeRequestDiff {
  summary: {
    fieldsChanged: number;
    fieldsAdded: number;
    fieldsRemoved: number;
  };
  diffs: ValueDiff[];
  metadata: {
    computedAt: string;
    algorithm: string;
  };
}

// Constants and enums
export const CHANGE_REQUEST_STATUSES = [
  { value: 'PENDING', label: 'Pending Review', color: 'yellow', icon: 'schedule' },
  { value: 'APPROVED', label: 'Approved', color: 'green', icon: 'check_circle' },
  { value: 'REJECTED', label: 'Rejected', color: 'red', icon: 'cancel' },
  { value: 'CANCELLED', label: 'Cancelled', color: 'gray', icon: 'remove_circle' },
  { value: 'APPLIED', label: 'Applied', color: 'blue', icon: 'published_with_changes' }
] as const;

export const ENTITY_TYPES = [
  { value: 'COUNTRY', label: 'Country', icon: 'public' },
  { value: 'PORT', label: 'Port', icon: 'sailing' },
  { value: 'AIRPORT', label: 'Airport', icon: 'flight' },
  { value: 'CARRIER', label: 'Carrier', icon: 'local_shipping' }
] as const;

export const CHANGE_TYPES = [
  { value: 'CREATE', label: 'Create', icon: 'add_circle', color: 'green' },
  { value: 'UPDATE', label: 'Update', icon: 'edit', color: 'blue' },
  { value: 'DELETE', label: 'Delete', icon: 'delete', color: 'red' }
] as const;

export const PRIORITY_LEVELS = [
  { value: 'LOW', label: 'Low', color: 'gray', order: 1 },
  { value: 'MEDIUM', label: 'Medium', color: 'yellow', order: 2 },
  { value: 'HIGH', label: 'High', color: 'orange', order: 3 },
  { value: 'URGENT', label: 'Urgent', color: 'red', order: 4 }
] as const;

// Utility type guards
export function isValidChangeRequestStatus(status: string): status is ChangeRequestDto['status'] {
  return ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'APPLIED'].includes(status);
}

export function isValidEntityType(entityType: string): entityType is ChangeRequestDto['entityType'] {
  return ['COUNTRY', 'PORT', 'AIRPORT', 'CARRIER'].includes(entityType);
}

export function isValidPriority(priority: string): priority is ChangeRequestDto['priority'] {
  return ['LOW', 'MEDIUM', 'HIGH', 'URGENT'].includes(priority);
}

// Helper functions
export function getStatusConfig(status: ChangeRequestDto['status']) {
  return CHANGE_REQUEST_STATUSES.find(s => s.value === status);
}

export function getEntityTypeConfig(entityType: ChangeRequestDto['entityType']) {
  return ENTITY_TYPES.find(e => e.value === entityType);
}

export function getPriorityConfig(priority: ChangeRequestDto['priority']) {
  return PRIORITY_LEVELS.find(p => p.value === priority);
}

export function getPriorityOrder(priority: ChangeRequestDto['priority']): number {
  return getPriorityConfig(priority)?.order || 0;
}

export function formatDateRelative(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffHours / 24);

  if (diffHours < 1) return 'Just now';
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;
  if (diffDays < 30) return `${Math.floor(diffDays / 7)}w ago`;
  return date.toLocaleDateString();
}

export function isOverdue(requestedAt: string, priority: ChangeRequestDto['priority']): boolean {
  const created = new Date(requestedAt);
  const now = new Date();
  const ageHours = (now.getTime() - created.getTime()) / (1000 * 60 * 60);

  // SLA thresholds based on priority
  const slaHours = {
    URGENT: 4,
    HIGH: 24,
    MEDIUM: 72,
    LOW: 168
  };

  return ageHours > slaHours[priority];
}