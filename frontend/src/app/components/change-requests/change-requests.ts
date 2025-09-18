import { Component, OnInit, ViewChild, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { ApiService, PagedResponse } from '../../services/api.service';
import { debounceTime, distinctUntilChanged, Subject, Observable, of } from 'rxjs';
import { ToastService } from '../../services/toast.service';
import { DiffViewerComponent } from '../diff-viewer/diff-viewer.component';
import { ApprovalHistoryComponent } from '../approval-history/approval-history.component';
import {
  ChangeRequestDto,
  ChangeRequestFilter,
  ChangeRequestBatchOperation,
  BatchOperationResponse,
  CHANGE_REQUEST_STATUSES,
  ENTITY_TYPES,
  CHANGE_TYPES,
  PRIORITY_LEVELS,
  getStatusConfig,
  getEntityTypeConfig,
  getPriorityConfig,
  isOverdue,
  formatDateRelative
} from '../../models/change-request.models';

@Component({
  selector: 'app-change-requests',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DiffViewerComponent, ApprovalHistoryComponent],
  templateUrl: './change-requests.html',
  styleUrl: './change-requests.scss'
})
export class ChangeRequestsComponent implements OnInit {
  @ViewChild('searchInput') searchInput!: ElementRef;

  changeRequests: ChangeRequestDto[] = [];
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;

  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  // Search and filtering
  searchTerm = '';
  searchSubject = new Subject<string>();
  filterForm: FormGroup;
  advancedFiltersVisible = false;

  // Available filter options
  availableStatuses = CHANGE_REQUEST_STATUSES;
  availableEntityTypes = ENTITY_TYPES;
  availableChangeTypes = CHANGE_TYPES;
  availablePriorities = PRIORITY_LEVELS;

  // Modal and details
  showModal = false;
  selectedRequest: ChangeRequestDto | null = null;
  showApprovalDialog = false;
  showBatchDialog = false;
  showDiffViewer = false;
  showHistoryViewer = false;
  modalTab: 'details' | 'diff' | 'history' = 'details';

  // Approval forms
  approvalNotes = '';
  rejectionNotes = '';
  batchComments = '';
  mode: 'approve' | 'reject' | 'batch-approve' | 'batch-reject' | null = null;
  notifyRequesters = true;
  urgentProcessing = false;

  // JSON accordion toggle in details modal
  showJson = false;

  // Sorting
  sortField: keyof ChangeRequestDto = 'createdAt';
  sortDirection: 'asc' | 'desc' = 'desc';

  // Bulk operations
  selectedRequests = new Set<string>();
  selectAll = false;
  batchProcessing = false;

  // View options
  viewMode: 'table' | 'cards' = 'table';
  showOverdueOnly = false;
  showPendingOnly = false;

  // Statistics
  stats = {
    total: 0,
    pending: 0,
    overdue: 0,
    approved: 0,
    rejected: 0
  };

  constructor(
    private apiService: ApiService,
    private toastService: ToastService,
    private fb: FormBuilder
  ) {
    this.filterForm = this.createFilterForm();
  }
  
  private createFilterForm(): FormGroup {
    return this.fb.group({
      status: [[]],
      entityType: [[]],
      changeType: [[]],
      priority: [[]],
      requestedBy: [''],
      reviewedBy: [''],
      dateFrom: [''],
      dateTo: [''],
      department: [''],
      tags: [[]]
    });
  }

  private initializeSearchSubscription() {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      this.searchTerm = searchTerm;
      this.currentPage = 0;
      this.loadChangeRequests();
    });

    // Subscribe to filter form changes
    this.filterForm.valueChanges.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadChangeRequests();
    });
  }

  ngOnInit() {
    console.log('===== ChangeRequestsComponent ngOnInit =====');
    console.log('Component initialized');
    this.initializeSearchSubscription();
    
    // Load initial data
    this.loadInitialData();
  }

  private loadInitialData() {
    // Load data from API
    this.loadChangeRequests();
  }

  loadChangeRequests() {
    this.loading = true;
    this.error = null;
    this.selectedRequests.clear();
    this.selectAll = false;

    // Build enhanced filter parameters
    const filter = this.buildFilterFromForm();

    // Use enhanced filtering if advanced filters are applied
    const useAdvancedFiltering = this.hasAdvancedFilters();

    if (useAdvancedFiltering) {
      this.apiService.getChangeRequestsFiltered(filter).subscribe({
        next: (response) => this.handleChangeRequestsResponse(response),
        error: (error) => this.handleChangeRequestsError(error)
      });
    } else {
      // Use simple parameters for basic filtering
      const params = this.buildBasicParams();
      this.apiService.getChangeRequests(params).subscribe({
        next: (response) => this.handleChangeRequestsResponse(response),
        error: (error) => this.handleChangeRequestsError(error)
      });
    }
  }

  private buildFilterFromForm(): ChangeRequestFilter {
    const formValue = this.filterForm.value;
    const filter: ChangeRequestFilter = {};

    if (formValue.status?.length) filter.status = formValue.status;
    if (formValue.entityType?.length) filter.entityType = formValue.entityType;
    if (formValue.changeType?.length) filter.changeType = formValue.changeType;
    if (formValue.priority?.length) filter.priority = formValue.priority;
    if (formValue.requestedBy) filter.requestedBy = formValue.requestedBy;
    if (formValue.reviewedBy) filter.reviewedBy = formValue.reviewedBy;
    if (formValue.dateFrom) filter.dateFrom = formValue.dateFrom;
    if (formValue.dateTo) filter.dateTo = formValue.dateTo;
    if (formValue.department) filter.department = formValue.department;
    if (formValue.tags?.length) filter.tags = formValue.tags;
    if (this.searchTerm) filter.searchTerm = this.searchTerm;

    return filter;
  }

  private buildBasicParams(): any {
    const params: any = {
      page: this.currentPage,
      size: this.pageSize,
      sort: `${this.sortField},${this.sortDirection}`
    };

    const formValue = this.filterForm.value;

    if (formValue.status?.length === 1) params.status = formValue.status[0];
    if (formValue.entityType?.length === 1) params.entityType = formValue.entityType[0];
    if (formValue.priority?.length === 1) params.priority = formValue.priority[0];
    if (formValue.changeType?.length === 1) params.changeType = formValue.changeType[0];
    if (formValue.requestedBy) params.requestedBy = formValue.requestedBy;
    if (formValue.dateFrom) params.dateFrom = formValue.dateFrom;
    if (formValue.dateTo) params.dateTo = formValue.dateTo;

    return params;
  }

  private hasAdvancedFilters(): boolean {
    const formValue = this.filterForm.value;
    return (
      (formValue.status?.length > 1) ||
      (formValue.entityType?.length > 1) ||
      (formValue.changeType?.length > 1) ||
      (formValue.priority?.length > 1) ||
      formValue.reviewedBy ||
      formValue.department ||
      (formValue.tags?.length > 0) ||
      this.searchTerm
    );
  }

  private handleChangeRequestsResponse(response: PagedResponse<ChangeRequestDto>) {
    console.log('Change requests loaded:', response);
    this.changeRequests = response.content || [];
    this.totalElements = response.totalElements || 0;
    this.totalPages = response.totalPages || 0;
    this.loading = false;

    // Apply additional client-side filters
    this.applyClientSideFilters();

    // Apply sorting if not done server-side
    this.sortChangeRequests();

    // Update statistics
    this.updateStatistics();
  }

  private handleChangeRequestsError(error: any) {
    console.error('Failed to load change requests from API:', error);
    this.loading = false;
    this.error = `Failed to connect to API (${error.status || 'Network Error'}). Please check if the backend service is running.`;
  }

  private applyClientSideFilters() {
    let filtered = [...this.changeRequests];

    // Apply overdue filter
    if (this.showOverdueOnly) {
      filtered = filtered.filter(request =>
        request.priority && isOverdue(request.requestedAt, request.priority)
      );
    }

    // Apply pending only filter
    if (this.showPendingOnly) {
      filtered = filtered.filter(request => request.status === 'PENDING');
    }

    this.changeRequests = filtered;
    this.totalElements = filtered.length;
    this.totalPages = Math.ceil(this.totalElements / this.pageSize);
  }

  private updateStatistics() {
    this.stats = {
      total: this.changeRequests.length,
      pending: this.changeRequests.filter(r => r.status === 'PENDING').length,
      overdue: this.changeRequests.filter(r =>
        r.priority && isOverdue(r.requestedAt, r.priority)
      ).length,
      approved: this.changeRequests.filter(r => r.status === 'APPROVED').length,
      rejected: this.changeRequests.filter(r => r.status === 'REJECTED').length
    };
  }

  private applySearch() {
    if (!this.searchTerm.trim()) return;
    
    const searchLower = this.searchTerm.toLowerCase();
    this.changeRequests = this.changeRequests.filter(request => 
      request.description.toLowerCase().includes(searchLower) ||
      request.requestedBy.toLowerCase().includes(searchLower) ||
      request.id.toLowerCase().includes(searchLower)
    );
  }

  onSearchInput(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }
  
  clearSearch() {
    this.searchTerm = '';
    this.searchSubject.next('');
    if (this.searchInput) {
      this.searchInput.nativeElement.value = '';
    }
  }
  
  onStatusFilterChange(value: string) {
    this.filterStatus = value === 'all' ? null : value;
    this.currentPage = 0;
    this.loadChangeRequests();
  }
  
  onEntityTypeFilterChange(value: string) {
    this.filterEntityType = value === 'all' ? null : value;
    this.currentPage = 0;
    this.loadChangeRequests();
  }
  
  sortBy(field: keyof ChangeRequestDto) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.sortChangeRequests();
  }
  
  private sortChangeRequests() {
    this.changeRequests.sort((a, b) => {
      const aVal = a[this.sortField];
      const bVal = b[this.sortField];
      
      if (aVal === null || aVal === undefined) return 1;
      if (bVal === null || bVal === undefined) return -1;
      
      const comparison = aVal < bVal ? -1 : aVal > bVal ? 1 : 0;
      return this.sortDirection === 'asc' ? comparison : -comparison;
    });
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadChangeRequests();
    }
  }

  viewRequest(request: ChangeRequestDto) {
    this.selectedRequest = { ...request };
    this.showModal = true;
  }
  
  approveRequest(request: ChangeRequestDto) {
    this.selectedRequest = request;
    this.showApprovalDialog = true;
    this.mode = 'approve';
    this.approvalNotes = '';
    this.rejectionNotes = '';
  }
  
  rejectRequest(request: ChangeRequestDto) {
    this.selectedRequest = request;
    this.showApprovalDialog = true;
    this.mode = 'reject';
    this.rejectionNotes = '';
    this.approvalNotes = '';
  }
  
  confirmApproval() {
    if (!this.selectedRequest) return;
    
    const taskVariables = {
      approved: true,
    };
    
    // Use the approve endpoint
    this.apiService.approveChangeRequest(this.selectedRequest.id, this.approvalNotes).subscribe({
      next: () => {
        this.successMessage = 'Change request approved successfully';
        this.closeApprovalDialog();
        this.loadChangeRequests();
        
        setTimeout(() => {
          this.successMessage = null;
        }, 5000);
      },
      error: (error: any) => {
        this.error = error.error?.detail || 'Failed to approve request';
        console.error('Error approving request:', error);
      }
    });
  }
  
  confirmRejection() {
    if (!this.selectedRequest) return;
    
    const taskVariables = {
      approved: false,
    };
    
    // Use the reject endpoint
    this.apiService.rejectChangeRequest(this.selectedRequest.id, this.rejectionNotes).subscribe({
      next: () => {
        this.successMessage = 'Change request rejected';
        this.closeApprovalDialog();
        this.loadChangeRequests();
        
        setTimeout(() => {
          this.successMessage = null;
        }, 5000);
      },
      error: (error: any) => {
        this.error = error.error?.detail || 'Failed to reject request';
        console.error('Error rejecting request:', error);
      }
    });
  }
  
  closeModal() {
    this.showModal = false;
    this.selectedRequest = null;
    this.showJson = false;
  }

  sort(field: keyof ChangeRequestDto) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.sortChangeRequests();
  }

  viewDetails(request: ChangeRequestDto) {
    this.selectedRequest = request;
    this.showModal = true;
  }

  previousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadChangeRequests();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadChangeRequests();
    }
  }

  getPageNumbers(): number[] {
    const pages = [];
    const maxPages = Math.min(this.totalPages, 5);
    let start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages, start + maxPages);
    
    if (end - start < maxPages) {
      start = Math.max(0, end - maxPages);
    }
    
    for (let i = start; i < end; i++) {
      pages.push(i + 1);
    }
    
    return pages;
  }

  closeApprovalDialog() {
    this.showApprovalDialog = false;
    this.selectedRequest = null;
    this.approvalNotes = '';
    this.rejectionNotes = '';
    this.mode = null;
  }
  
  // Bulk operations
  toggleSelectAll() {
    if (this.selectAll) {
      this.changeRequests.forEach(r => this.selectedRequests.add(r.id));
    } else {
      this.selectedRequests.clear();
    }
  }
  
  toggleSelection(requestId: string | undefined) {
    if (!requestId) return;
    if (this.selectedRequests.has(requestId)) {
      this.selectedRequests.delete(requestId);
    } else {
      this.selectedRequests.add(requestId);
    }
    
    this.selectAll = this.selectedRequests.size === this.changeRequests.length;
  }
  
  isSelected(requestId: string | undefined): boolean {
    return requestId ? this.selectedRequests.has(requestId) : false;
  }
  
  bulkApprove() {
    const selectedData = this.changeRequests.filter(r => this.selectedRequests.has(r.id));
    if (selectedData.length === 0) return;
    this.toastService.showInfo('Bulk action', `${selectedData.length} items will be approved (mock).`);
  }
  
  bulkReject() {
    const selectedData = this.changeRequests.filter(r => this.selectedRequests.has(r.id));
    if (selectedData.length === 0) return;
    this.toastService.showInfo('Bulk action', `${selectedData.length} items will be rejected (mock).`);
  }
  
  exportSelected() {
    const selectedData = this.changeRequests.filter(r => this.selectedRequests.has(r.id));
    const csv = this.convertToCSV(selectedData);
    this.downloadCSV(csv, `change_requests_export_${new Date().getTime()}.csv`);
  }
  
  private convertToCSV(data: ChangeRequestDto[]): string {
    if (!data.length) return '';
    
    const headers = ['ID', 'Change Type', 'Entity Type', 'Description', 'Requested By', 'Status', 'Created At', 'Updated At'];
    const rows = data.map(r => [
      r.id,
      r.changeType,
      r.entityType,
      r.description,
      r.requestedBy,
      r.status,
      new Date(r.createdAt).toLocaleDateString(),
      new Date(r.updatedAt).toLocaleDateString()
    ]);
    
    return [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell || ''}"`).join(','))
    ].join('\n');
  }
  
  private downloadCSV(csv: string, filename: string) {
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  get pageNumbers(): number[] {
    const pages = [];
    const maxVisible = 5;
    const halfVisible = Math.floor(maxVisible / 2);
    
    let start = Math.max(0, this.currentPage - halfVisible);
    let end = Math.min(this.totalPages - 1, start + maxVisible - 1);
    
    if (end - start < maxVisible - 1) {
      start = Math.max(0, end - maxVisible + 1);
    }
    
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    
    return pages;
  }
  
  get hasSelectedItems(): boolean {
    return this.selectedRequests.size > 0;
  }
  
  get pendingRequests(): ChangeRequestDto[] {
    return this.changeRequests.filter(r => r.status === 'PENDING');
  }
  
  formatDate(date: string | undefined): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
  
  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'usa-tag--yellow';
      case 'APPROVED': return 'usa-tag--green';
      case 'REJECTED': return 'usa-tag--red';
      case 'CANCELLED': return 'usa-tag--gray';
      default: return 'usa-tag--gray';
    }
  }
  
  getEntityTypeIcon(entityType: string | undefined): string {
    // Return USWDS sprite id
    if (!entityType) return 'assignment';
    switch (entityType) {
      case 'COUNTRY': return 'public';
      case 'PORT': return 'sailing';
      case 'AIRPORT': return 'flight';
      default: return 'assignment';
    }
  }
  
  getSortIcon(field: keyof ChangeRequestDto): string {
    if (this.sortField !== field) return 'unfold_more';
    return this.sortDirection === 'asc' ? 'expand_less' : 'expand_more';
  }
  
  canApprove(request: ChangeRequestDto | null | undefined): boolean {
    return request?.status === 'PENDING';
  }
  
  canReject(request: ChangeRequestDto | null | undefined): boolean {
    return request?.status === 'PENDING';
  }
  
  getMinValue(a: number, b: number): number {
    return Math.min(a, b);
  }

  // Enhanced batch operations
  performBatchOperation(action: 'approve' | 'reject' | 'cancel') {
    if (this.selectedRequests.size === 0) {
      this.toastService.showWarning('No Selection', 'Please select at least one change request.');
      return;
    }

    this.mode = action === 'approve' ? 'batch-approve' : action === 'reject' ? 'batch-reject' : null;
    this.showBatchDialog = true;
    this.batchComments = '';
    this.notifyRequesters = true;
  }

  confirmBatchOperation() {
    if (!this.mode || this.selectedRequests.size === 0) return;

    this.batchProcessing = true;
    const operation: ChangeRequestBatchOperation = {
      action: this.mode === 'batch-approve' ? 'APPROVE' : this.mode === 'batch-reject' ? 'REJECT' : 'CANCEL',
      requestIds: Array.from(this.selectedRequests),
      comments: this.batchComments,
      notifyRequesters: this.notifyRequesters
    };

    const apiCall = this.mode === 'batch-approve'
      ? this.apiService.batchApproveChangeRequests(operation)
      : this.mode === 'batch-reject'
      ? this.apiService.batchRejectChangeRequests(operation)
      : this.apiService.batchCancelChangeRequests(operation);

    apiCall.subscribe({
      next: (response: BatchOperationResponse) => {
        this.handleBatchOperationResponse(response);
        this.closeBatchDialog();
        this.loadChangeRequests();
      },
      error: (error) => {
        this.error = `Batch operation failed: ${error.error?.detail || error.message}`;
        this.batchProcessing = false;
      }
    });
  }

  private handleBatchOperationResponse(response: BatchOperationResponse) {
    this.batchProcessing = false;
    const { processedCount, failedCount, errors } = response;

    if (failedCount === 0) {
      this.toastService.showSuccess(
        'Batch Operation Complete',
        `Successfully processed ${processedCount} requests.`
      );
    } else {
      this.toastService.showWarning(
        'Partial Success',
        `Processed ${processedCount} requests. ${failedCount} failed.`
      );
      console.warn('Batch operation errors:', errors);
    }
  }

  // Enhanced modal operations
  viewDiff(request: ChangeRequestDto) {
    this.selectedRequest = request;
    this.modalTab = 'diff';
    this.showModal = true;
    this.showDiffViewer = true;
    this.showHistoryViewer = false;
  }

  viewHistory(request: ChangeRequestDto) {
    this.selectedRequest = request;
    this.modalTab = 'history';
    this.showModal = true;
    this.showDiffViewer = false;
    this.showHistoryViewer = true;
  }

  switchModalTab(tab: 'details' | 'diff' | 'history') {
    this.modalTab = tab;
    this.showDiffViewer = tab === 'diff';
    this.showHistoryViewer = tab === 'history';
  }

  // Advanced filtering
  toggleAdvancedFilters() {
    this.advancedFiltersVisible = !this.advancedFiltersVisible;
  }

  clearAllFilters() {
    this.filterForm.reset();
    this.searchTerm = '';
    this.showOverdueOnly = false;
    this.showPendingOnly = false;
    if (this.searchInput) {
      this.searchInput.nativeElement.value = '';
    }
    this.currentPage = 0;
    this.loadChangeRequests();
  }

  // Enhanced dialog management
  closeBatchDialog() {
    this.showBatchDialog = false;
    this.batchComments = '';
    this.mode = null;
    this.batchProcessing = false;
  }

  // Helper methods for templates
  isOverdue(request: ChangeRequestDto): boolean {
    return request.priority ? isOverdue(request.requestedAt, request.priority) : false;
  }

  getRelativeTime(dateString: string): string {
    return formatDateRelative(dateString);
  }

  getPriorityColor(priority: ChangeRequestDto['priority']): string {
    const config = getPriorityConfig(priority);
    return config?.color || 'gray';
  }

  hasActiveFilters(): boolean {
    const formValue = this.filterForm.value;
    return (
      formValue.status?.length > 0 ||
      formValue.entityType?.length > 0 ||
      formValue.changeType?.length > 0 ||
      formValue.priority?.length > 0 ||
      formValue.requestedBy ||
      formValue.reviewedBy ||
      formValue.dateFrom ||
      formValue.dateTo ||
      formValue.department ||
      formValue.tags?.length > 0 ||
      this.searchTerm ||
      this.showOverdueOnly ||
      this.showPendingOnly
    );
  }

  getFilterCount(): number {
    let count = 0;
    const formValue = this.filterForm.value;

    if (formValue.status?.length) count++;
    if (formValue.entityType?.length) count++;
    if (formValue.changeType?.length) count++;
    if (formValue.priority?.length) count++;
    if (formValue.requestedBy) count++;
    if (formValue.reviewedBy) count++;
    if (formValue.dateFrom) count++;
    if (formValue.dateTo) count++;
    if (formValue.department) count++;
    if (formValue.tags?.length) count++;
    if (this.searchTerm) count++;
    if (this.showOverdueOnly) count++;
    if (this.showPendingOnly) count++;

    return count;
  }

  trackByRequestId(index: number, request: ChangeRequestDto): string {
    return request.id;
  }

  switchViewMode(mode: 'table' | 'cards') {
    this.viewMode = mode;
  }

  // Close modals with Escape
  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.showBatchDialog) this.closeBatchDialog();
    if (this.showApprovalDialog) this.closeApprovalDialog();
    if (this.showModal) this.closeModal();
  }

}
