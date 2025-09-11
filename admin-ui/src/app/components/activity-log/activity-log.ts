import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';

interface ActivityLogEntry {
  id: string;
  timestamp: string;
  action: string;
  entityType: string;
  entityId: string;
  description: string;
  ipAddress: string;
  userAgent: string;
  sessionId: string;
  result: 'SUCCESS' | 'FAILURE' | 'WARNING';
  details?: any;
  userId?: string;
  userName?: string;
}

interface ActivityLogResponse {
  content: ActivityLogEntry[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

interface ActivityFilter {
  dateFrom: string;
  dateTo: string;
  action: string;
  entityType: string;
  result: string;
  user?: string;
}

@Component({
  selector: 'app-activity-log',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './activity-log.html',
  styleUrl: './activity-log.scss'
})
export class ActivityLogComponent implements OnInit {
  activities: ActivityLogEntry[] = [];
  loading = false;
  error: string | null = null;
  
  // Expose Math object for template usage
  Math = Math;
  
  // Pagination
  currentPage = 0;
  pageSize = 25;
  totalElements = 0;
  totalPages = 0;

  // Filters
  filter: ActivityFilter = {
    dateFrom: this.getDefaultDateFrom(),
    dateTo: this.getDefaultDateTo(),
    action: '',
    entityType: '',
    result: '',
    user: ''
  };

  showFilters = false;
  expandedEntry: string | null = null;

  // Filter options - will be populated from API metadata
  actionOptions = [
    { value: '', label: 'All Actions' }
  ];

  entityTypeOptions = [
    { value: '', label: 'All Entity Types' }
  ];

  resultOptions = [
    { value: '', label: 'All Results' },
    { value: 'SUCCESS', label: 'Success' },
    { value: 'FAILURE', label: 'Failure' },
    { value: 'WARNING', label: 'Warning' }
  ];

  constructor(
    private apiService: ApiService,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadFilterOptions();
    this.loadActivityLog();
  }

  loadActivityLog() {
    this.loading = true;
    this.error = null;

    // Build query parameters for the dedicated API method
    const params: any = {
      page: this.currentPage,
      size: this.pageSize
    };

    // Add filters if specified
    if (this.filter.dateFrom) {
      params.startDate = this.filter.dateFrom;
    }
    if (this.filter.dateTo) {
      params.endDate = this.filter.dateTo;
    }
    if (this.filter.action) {
      params.action = this.filter.action;
    }
    if (this.filter.entityType) {
      params.entity = this.filter.entityType;
    }
    if (this.filter.result) {
      params.result = this.filter.result;
    }
    if (this.filter.user) {
      params.user = this.filter.user;
    }

    this.apiService.getActivityLog(params).subscribe({
      next: (response) => {
        this.activities = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.currentPage = response.number;
        this.loading = false;
      },
      error: (error) => {
        console.error('Failed to load activity log:', error);
        this.error = 'Failed to load activity log. Please try again later.';
        this.loading = false;
        this.toastService.showError('Failed to load activity log');
      }
    });
  }

  loadFilterOptions() {
    // Set up commonly used filter options directly since we can't currently fetch them from API
    this.actionOptions = [
      { value: '', label: 'All Actions' },
      { value: 'LOGIN', label: 'Login' },
      { value: 'LOGOUT', label: 'Logout' },
      { value: 'CREATE', label: 'Create' },
      { value: 'UPDATE', label: 'Update' },
      { value: 'DELETE', label: 'Delete' },
      { value: 'VIEW', label: 'View' },
      { value: 'EXPORT', label: 'Export' },
      { value: 'IMPORT', label: 'Import' },
      { value: 'APPROVE', label: 'Approve' },
      { value: 'REJECT', label: 'Reject' }
    ];

    this.entityTypeOptions = [
      { value: '', label: 'All Entity Types' },
      { value: 'COUNTRY', label: 'Country' },
      { value: 'PORT', label: 'Port' },
      { value: 'AIRPORT', label: 'Airport' },
      { value: 'CODE_MAPPING', label: 'Code Mapping' },
      { value: 'CHANGE_REQUEST', label: 'Change Request' },
      { value: 'USER', label: 'User' },
      { value: 'SETTINGS', label: 'Settings' }
    ];
  }

  formatActionLabel(action: string): string {
    return action.charAt(0).toUpperCase() + action.slice(1).toLowerCase().replace('_', ' ');
  }

  formatEntityTypeLabel(entityType: string): string {
    return entityType.charAt(0).toUpperCase() + entityType.slice(1).toLowerCase().replace('_', ' ');
  }

  generateDescription(action: string, entityType: string): string {
    const descriptions = {
      'LOGIN': 'User logged into the system',
      'LOGOUT': 'User logged out of the system',
      'VIEW': `Viewed ${entityType.toLowerCase().replace('_', ' ')} details`,
      'CREATE': `Created new ${entityType.toLowerCase().replace('_', ' ')}`,
      'UPDATE': `Updated ${entityType.toLowerCase().replace('_', ' ')} information`,
      'DELETE': `Deleted ${entityType.toLowerCase().replace('_', ' ')}`,
      'EXPORT': `Exported ${entityType.toLowerCase().replace('_', ' ')} data`,
      'IMPORT': `Imported ${entityType.toLowerCase().replace('_', ' ')} data`,
      'APPROVE': `Approved ${entityType.toLowerCase().replace('_', ' ')}`,
      'REJECT': `Rejected ${entityType.toLowerCase().replace('_', ' ')}`
    };
    return descriptions[action as keyof typeof descriptions] || `Performed ${action.toLowerCase().replace('_', ' ')} on ${entityType.toLowerCase().replace('_', ' ')}`;
  }

  applyFilters() {
    // Validate date range
    if (this.filter.dateFrom && this.filter.dateTo) {
      const fromDate = new Date(this.filter.dateFrom);
      const toDate = new Date(this.filter.dateTo);
      
      if (fromDate > toDate) {
        this.toastService.showError('Start date cannot be after end date');
        return;
      }
    }

    this.currentPage = 0;
    this.loadActivityLog();
    this.showFilters = false;
  }

  clearFilters() {
    this.filter = {
      dateFrom: this.getDefaultDateFrom(),
      dateTo: this.getDefaultDateTo(),
      action: '',
      entityType: '',
      result: '',
      user: ''
    };
    this.applyFilters();
  }

  getDefaultDateFrom(): string {
    const date = new Date();
    date.setDate(date.getDate() - 30); // Last 30 days
    return date.toISOString().split('T')[0];
  }

  getDefaultDateTo(): string {
    return new Date().toISOString().split('T')[0];
  }

  toggleFilters() {
    this.showFilters = !this.showFilters;
  }

  toggleDetails(entryId: string) {
    this.expandedEntry = this.expandedEntry === entryId ? null : entryId;
  }

  formatTimestamp(timestamp: string): string {
    const date = new Date(timestamp);
    return date.toLocaleString();
  }

  getResultIcon(result: string): string {
    switch (result) {
      case 'SUCCESS': return 'check_circle';
      case 'FAILURE': return 'error';
      case 'WARNING': return 'warning';
      default: return 'info';
    }
  }

  getResultClass(result: string): string {
    switch (result) {
      case 'SUCCESS': return 'cbp-result--success';
      case 'FAILURE': return 'cbp-result--failure';
      case 'WARNING': return 'cbp-result--warning';
      default: return 'cbp-result--info';
    }
  }

  getActionIcon(action: string): string {
    switch (action) {
      case 'LOGIN': return 'login';
      case 'LOGOUT': return 'logout';
      case 'CREATE': return 'add_circle';
      case 'UPDATE': return 'edit';
      case 'DELETE': return 'delete';
      case 'VIEW': return 'visibility';
      case 'EXPORT': return 'download';
      case 'IMPORT': return 'upload';
      case 'APPROVE': return 'check_circle';
      case 'REJECT': return 'cancel';
      default: return 'assignment';
    }
  }

  exportActivity() {
    // For now, export as CSV using the current activity data
    this.loading = true;
    
    try {
      const csvContent = this.generateCSV();
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const link = document.createElement('a');
      
      if (link.download !== undefined) {
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', `activity-log-${new Date().toISOString().split('T')[0]}.csv`);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      }
      
      this.loading = false;
      this.toastService.showSuccess('Activity log exported successfully');
    } catch (error) {
      console.error('Failed to export activity log:', error);
      this.loading = false;
      this.toastService.showError('Failed to export activity log');
    }
  }

  private generateCSV(): string {
    const headers = ['Timestamp', 'Action', 'Entity Type', 'Entity ID', 'Description', 'Result', 'User', 'IP Address'];
    const rows = this.activities.map(activity => [
      this.formatTimestamp(activity.timestamp),
      activity.action,
      activity.entityType,
      activity.entityId,
      activity.description,
      activity.result,
      activity.userName || activity.userId || 'Unknown',
      activity.ipAddress
    ]);
    
    const csvRows = [headers, ...rows];
    return csvRows.map(row => 
      row.map(field => `"${String(field || '').replace(/"/g, '""')}"`).join(',')
    ).join('\n');
  }

  goToPreviousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadActivityLog();
    }
  }

  goToNextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadActivityLog();
    }
  }

  goToPage(page: number) {
    this.currentPage = page;
    this.loadActivityLog();
  }

  get paginatedActivities(): ActivityLogEntry[] {
    // Activities are already paginated by the API
    return this.activities;
  }

  get pageNumbers(): number[] {
    const pages = [];
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages, start + 5);
    
    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    
    return pages;
  }

  trackActivity(index: number, activity: ActivityLogEntry): string {
    return activity.id;
  }

  retryLoad() {
    this.error = null;
    this.loadActivityLog();
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}