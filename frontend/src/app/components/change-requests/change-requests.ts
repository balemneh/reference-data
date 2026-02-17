import { Component, OnInit, OnDestroy, ViewChild, ElementRef, HostListener, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, ChangeRequestDto, PagedResponse } from '../../services/api.service';
import { debounceTime, distinctUntilChanged, Subject, Subscription } from 'rxjs';
import { ToastService } from '../../services/toast.service';
import { ChangeRequestService } from '../../services/change-request.service';
import { OperationHistoryService } from '../../services/operation-history.service';
import { PendingRequestsTrackerComponent } from '../pending-requests-tracker/pending-requests-tracker';
import { StewardListModalComponent } from '../data-ownership/steward-list-modal.component';
import { PermissionEditorModalComponent } from '../data-ownership/permission-editor-modal.component';
import { UserDto } from '../../services/user-management.service';
import { UserManagementService } from '../../services/user-management.service';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-change-requests',
  standalone: true,
  imports: [CommonModule, FormsModule, PendingRequestsTrackerComponent, StewardListModalComponent, PermissionEditorModalComponent],
  templateUrl: './change-requests.html',
  styleUrl: './change-requests.scss'
})
export class ChangeRequestsComponent implements OnInit, OnDestroy {
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
  filterStatus: string | undefined = undefined;
  availableStatuses = ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'];
  filterEntityType: string | undefined = undefined;
  availableEntityTypes = ['COUNTRY', 'PORT', 'AIRPORT'];
  
  // Modal and details
  showModal = false;
  selectedRequest: ChangeRequestDto | null = null;
  showApprovalDialog = false;
  approvalNotes = '';
  rejectionNotes = '';
  mode: 'approve' | 'reject' | null = null; // approval dialog mode

  // JSON accordion toggle in details modal
  showJson = false;
  parsedProposedChanges: any = null;
  parsedCurrentValues: any = null;
  
  // Sorting
  sortField: keyof ChangeRequestDto = 'createdAt';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  // Bulk operations
  selectedRequests = new Set<string>();
  selectAll = false;

  // Steward and permission modals
  isStewardModalOpen = false;
  isPermissionModalOpen = false;
  allDataStewards: UserDto[] = [];
  selectedStewardForOwnership: UserDto | null = null;

  comparisonFields: string[] = ['countryName', 'countryCode', 'iso2Code', 'iso3Code', 'numericCode'];

  getObjectKeys(obj: any): string[] {
    return Object.keys(obj);
  }

  isChanged(key: string): boolean {
    if (!this.parsedCurrentValues || !this.parsedProposedChanges) {
      return false;
    }
    return this.parsedCurrentValues[key] !== this.parsedProposedChanges[key];
  }

  // Computed properties
  get pendingRequests(): ChangeRequestDto[] {
    return this.changeRequests.filter(r => r.status === 'PENDING');
  }

  get hasSelectedItems(): boolean {
    return this.selectedRequests.size > 0;
  }

  private subscriptions: Subscription[] = [];

  constructor(
    private apiService: ApiService, 
    private toastService: ToastService,
    private changeRequestService: ChangeRequestService,
    private keycloakService: KeycloakService,
    private userManagementService: UserManagementService,
    private cdr: ChangeDetectorRef,
    private operationHistoryService: OperationHistoryService
  ) {
    console.log('User roles:', this.keycloakService.getUserRoles());
  }
  
  private initializeSearchSubscription() {
    const searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      this.searchTerm = searchTerm;
      this.currentPage = 0;
      if (searchTerm !== '' || this.changeRequests.length === 0) {
        this.loadChangeRequests();
      }
    });
    this.subscriptions.push(searchSubscription);
  }

  ngOnInit() {
    this.initializeSearchSubscription();
    this.loadInitialData();
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private loadInitialData() {
    this.loadChangeRequests();
  }

  loadChangeRequests() {
    this.loading = true;
    this.error = null;
    this.selectedRequests.clear();
    this.selectAll = false;
    
    const params: any = {
      page: this.currentPage,
      size: this.pageSize,
      sortBy: this.sortField,
      sortDirection: this.sortDirection
    };
    
    if (this.filterStatus) {
      params.status = this.filterStatus;
    }
    
    if (this.filterEntityType) {
      params.entityType = this.filterEntityType;
    }
    
    this.apiService.getChangeRequests(params).subscribe({
      next: (response: PagedResponse<ChangeRequestDto>) => {
        this.changeRequests = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.totalPages = response.totalPages || 0;
        this.loading = false;
        console.log('Component state after loading:', {
          changeRequests: this.changeRequests,
          changeRequestsLength: this.changeRequests.length,
          loading: this.loading,
          totalElements: this.totalElements
        });

        this.changeRequestService.updatePendingRequestsCount(this.pendingRequests.length);
        
        // Apply client-side search if needed
        if (this.searchTerm && this.searchTerm.trim()) {
          this.applySearch();
          this.totalElements = this.changeRequests.length;
          this.totalPages = this.totalElements > 0 ? 1 : 0;
          this.currentPage = 0;
        }
        
        this.sortChangeRequests();
        this.cdr.detectChanges(); // Force UI update
      },
      error: (error: any) => {
        console.error('Failed to load change requests from API:', error);
        this.loading = false;
        this.error = `Failed to connect to API (${error.status || 'Network Error'}). Please check if the backend service is running.`;
        this.cdr.detectChanges(); // Force UI update on error
      }
    });
  }

  isAdmin(): boolean {
    return this.keycloakService.getUserRoles().includes('ADMIN');
  }

  openStewardModal(): void {
    this.userManagementService.getStewards().subscribe(
      (stewards: UserDto[]) => {
        this.allDataStewards = stewards;
        this.isStewardModalOpen = true;
      },
      (error: any) => {
        this.toastService.showError('Error', 'Failed to load data stewards.');
        console.error('Failed to load data stewards:', error);
      }
    );
  }

  closeStewardModal(): void {
    this.isStewardModalOpen = false;
  }

  openPermissionModal(steward: UserDto): void {
    this.selectedStewardForOwnership = steward;
    this.isPermissionModalOpen = true;
    this.isStewardModalOpen = false; // Close the steward list modal
  }

  onPermissionModalClose(needsRefresh: boolean): void {
    this.isPermissionModalOpen = false;
    this.selectedStewardForOwnership = null;
    if (needsRefresh) {
      // Optionally, you might want to refresh the steward list if permissions change
    }
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
    this.filterStatus = value === 'all' ? undefined : value;
    this.currentPage = 0;
    this.loadChangeRequests();
  }
  
  onEntityTypeFilterChange(value: string) {
    this.filterEntityType = value === 'all' ? undefined : value;
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
    
    try {
      if (typeof this.selectedRequest.proposedChanges === 'string') {
        this.parsedProposedChanges = JSON.parse(this.selectedRequest.proposedChanges);
        this.selectedRequest.proposedChanges = this.parsedProposedChanges; // Update selectedRequest
      } else {
        this.parsedProposedChanges = this.selectedRequest.proposedChanges;
      }
    } catch (e) {
      console.error('Error parsing proposedChanges:', e);
      this.parsedProposedChanges = {};
      this.selectedRequest.proposedChanges = {}; // Update selectedRequest
    }

    try {
      if (typeof this.selectedRequest.currentValues === 'string') {
        this.parsedCurrentValues = JSON.parse(this.selectedRequest.currentValues);
        this.selectedRequest.currentValues = this.parsedCurrentValues; // Update selectedRequest
      } else {
        this.parsedCurrentValues = this.selectedRequest.currentValues;
      }
    } catch (e) {
      console.error('Error parsing currentValues:', e);
      this.parsedCurrentValues = {};
      this.selectedRequest.currentValues = {}; // Update selectedRequest
    }
    
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
    
    this.apiService.approveChangeRequest(this.selectedRequest.id, this.approvalNotes).subscribe({
      next: () => {
        this.successMessage = 'Change request approved successfully';
        this.closeApprovalDialog();
        this.loadChangeRequests();
        this.operationHistoryService.removeOperationByChangeRequestId(this.selectedRequest!.id);
        
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
    
    this.apiService.rejectChangeRequest(this.selectedRequest.id, this.rejectionNotes).subscribe({
      next: () => {
        this.successMessage = 'Change request rejected';
        this.closeApprovalDialog();
        this.loadChangeRequests();
        this.operationHistoryService.removeOperationByChangeRequestId(this.selectedRequest!.id);
        
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
    
    try {
      if (typeof this.selectedRequest.proposedChanges === 'string') {
        this.parsedProposedChanges = JSON.parse(this.selectedRequest.proposedChanges);
        this.selectedRequest.proposedChanges = this.parsedProposedChanges; // Update selectedRequest
      } else {
        this.parsedProposedChanges = this.selectedRequest.proposedChanges;
      }
    } catch (e) {
      console.error('Error parsing proposedChanges:', e);
      this.parsedProposedChanges = {};
      this.selectedRequest.proposedChanges = {}; // Update selectedRequest
    }

    try {
      if (typeof this.selectedRequest.currentValues === 'string') {
        this.parsedCurrentValues = JSON.parse(this.selectedRequest.currentValues);
        this.selectedRequest.currentValues = this.parsedCurrentValues; // Update selectedRequest
      } else {
        this.parsedCurrentValues = this.selectedRequest.currentValues;
      }
    } catch (e) {
      console.error('Error parsing currentValues:', e);
      this.parsedCurrentValues = {};
      this.selectedRequest.currentValues = {}; // Update selectedRequest
    }
    
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
    
    if (end - start < maxVisible) {
      start = Math.max(0, end - maxVisible + 1);
    }
    
    for (let i = start; i < end; i++) {
      pages.push(i + 1);
    }
    
    return pages;
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

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.showApprovalDialog) this.closeApprovalDialog();
    if (this.showModal) this.closeModal();
  }

}
