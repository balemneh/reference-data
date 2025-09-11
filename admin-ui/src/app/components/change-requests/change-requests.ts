import { Component, OnInit, ViewChild, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, ChangeRequestDto, PagedResponse } from '../../services/api.service';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { ToastService } from '../../services/toast.service';
import { simpleMockChangeRequests } from './simple-mock';

@Component({
  selector: 'app-change-requests',
  standalone: true,
  imports: [CommonModule, FormsModule],
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
  filterStatus: string | null = null;
  availableStatuses = ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'];
  filterEntityType: string | null = null;
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
  
  // Sorting
  sortField: keyof ChangeRequestDto = 'createdAt';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  // Bulk operations
  selectedRequests = new Set<string>();
  selectAll = false;

  constructor(private apiService: ApiService, private toastService: ToastService) {}
  
  private initializeSearchSubscription() {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      this.searchTerm = searchTerm;
      this.currentPage = 0;
      // Only reload if we have a search term or if it was cleared (not initial load)
      if (searchTerm !== '' || this.changeRequests.length === 0) {
        this.loadChangeRequests();
      }
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
    // Load mock data directly for demonstration
    this.loading = false;
    this.changeRequests = [...simpleMockChangeRequests] as ChangeRequestDto[];
    this.totalElements = this.changeRequests.length;
    this.totalPages = Math.ceil(this.totalElements / this.pageSize);
    
    console.log('Initial data loaded:', {
      data: this.changeRequests,
      count: this.changeRequests.length,
      totalElements: this.totalElements,
      totalPages: this.totalPages
    });
  }

  loadChangeRequests() {
    this.loading = true;
    this.error = null;
    this.selectedRequests.clear();
    this.selectAll = false;
    
    const params: any = {
      page: this.currentPage,
      size: this.pageSize
    };
    
    if (this.filterStatus) {
      params.status = this.filterStatus;
    }
    
    if (this.filterEntityType) {
      params.entityType = this.filterEntityType;
    }
    
    this.apiService.getChangeRequests(params).subscribe({
      next: (response: PagedResponse<ChangeRequestDto>) => {
        console.log('Change requests loaded:', response);
        console.log('Response content array:', response.content);
        console.log('Content length:', response.content?.length);
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
        
        // Apply client-side search if needed
        if (this.searchTerm && this.searchTerm.trim()) {
          this.applySearch();
          // Recompute pagination locally to avoid mismatch
          this.totalElements = this.changeRequests.length;
          this.totalPages = this.totalElements > 0 ? 1 : 0;
          this.currentPage = 0;
        }
        
        // Apply sorting
        this.sortChangeRequests();
      },
      error: (error) => {
        // Load mock data on error for demonstration
        this.loading = false;
        this.loadMockData();
        this.error = null; // Clear error for demo
        console.log('API not available, loading mock data for demonstration');
      }
    });
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

  // Close modals with Escape
  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.showApprovalDialog) this.closeApprovalDialog();
    if (this.showModal) this.closeModal();
  }

  private loadMockData() {
    // Mock data for UI demonstration when API is not available
    this.loading = false;
    this.changeRequests = simpleMockChangeRequests as ChangeRequestDto[];
    this.totalElements = this.changeRequests.length;
    this.totalPages = 1;
    
    console.log('Mock data loaded:', {
      data: this.changeRequests,
      count: this.changeRequests.length,
      totalElements: this.totalElements
    });
    
    // Apply sorting
    this.sortChangeRequests();
  }
}
