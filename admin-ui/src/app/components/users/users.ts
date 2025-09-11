import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../services/toast.service';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';

export interface UserDto {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  status: UserStatus;
  department: string;
  lastLogin?: string;
  createdAt: string;
  updatedAt: string;
  isActive: boolean;
  phoneNumber?: string;
  jobTitle?: string;
  manager?: string;
  twoFactorEnabled: boolean;
  failedLoginAttempts: number;
  passwordExpires?: string;
  permissions: string[];
  notes?: string;
}

export interface UserActivity {
  id: string;
  userId: string;
  action: string;
  timestamp: string;
  ipAddress: string;
  userAgent: string;
  resource?: string;
  details?: string;
}

export enum UserRole {
  ADMIN = 'ADMIN',
  EDITOR = 'EDITOR',
  VIEWER = 'VIEWER',
  ANALYST = 'ANALYST',
  AUDITOR = 'AUDITOR'
}

export enum UserStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUSPENDED = 'SUSPENDED',
  PENDING = 'PENDING',
  LOCKED = 'LOCKED'
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.html',
  styleUrl: './users.scss'
})
export class UsersComponent implements OnInit {
  @ViewChild('searchInput') searchInput!: ElementRef;
  
  users: UserDto[] = [];
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
  selectedRole: UserRole | 'ALL' = 'ALL';
  selectedStatus: UserStatus | 'ALL' = 'ALL';
  selectedDepartment: string | 'ALL' = 'ALL';
  dateFilter: 'ALL' | 'LAST_7_DAYS' | 'LAST_30_DAYS' | 'LAST_90_DAYS' = 'ALL';
  
  // Available options
  availableRoles = Object.values(UserRole);
  availableStatuses = Object.values(UserStatus);
  availableDepartments = [
    'IT', 'HR', 'Finance', 'Operations', 'Legal', 'Compliance',
    'Data Management', 'Analytics', 'Security', 'Administration'
  ];
  
  // Modal and form
  showModal = false;
  showDeleteConfirm = false;
  showActivityModal = false;
  showBulkModal = false;
  showPermissionModal = false;
  selectedUser: UserDto | null = null;
  userToDelete: UserDto | null = null;
  isEditMode = false;
  formErrors: any = {};
  
  // Activity
  userActivities: UserActivity[] = [];
  loadingActivity = false;
  
  // Sorting
  sortField: keyof UserDto = 'lastName';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Bulk operations
  selectedUsers = new Set<string>();
  selectAll = false;
  bulkOperation: 'activate' | 'deactivate' | 'suspend' | 'reset_password' | 'delete' | null = null;
  
  // View toggle
  viewMode: 'table' | 'card' = 'table';
  
  // Permission matrix
  allPermissions = [
    'countries.view', 'countries.edit', 'countries.create', 'countries.delete',
    'users.view', 'users.edit', 'users.create', 'users.delete',
    'changeRequests.view', 'changeRequests.approve', 'changeRequests.reject',
    'audit.view', 'system.admin', 'data.export', 'reports.generate'
  ];

  constructor(private toastService: ToastService) {}

  ngOnInit() {
    this.initializeSearchSubscription();
    this.loadUsers();
  }

  private initializeSearchSubscription() {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      this.searchTerm = searchTerm;
      this.currentPage = 0;
      this.loadUsers();
    });
  }

  loadUsers() {
    this.loading = true;
    this.error = null;
    this.selectedUsers.clear();
    this.selectAll = false;
    
    // Simulate API call with mock data
    setTimeout(() => {
      try {
        let mockUsers = this.generateMockUsers();
        
        // Apply filters
        if (this.searchTerm) {
          const searchLower = this.searchTerm.toLowerCase();
          mockUsers = mockUsers.filter(user => 
            user.firstName.toLowerCase().includes(searchLower) ||
            user.lastName.toLowerCase().includes(searchLower) ||
            user.username.toLowerCase().includes(searchLower) ||
            user.email.toLowerCase().includes(searchLower) ||
            user.department.toLowerCase().includes(searchLower)
          );
        }
        
        if (this.selectedRole !== 'ALL') {
          mockUsers = mockUsers.filter(user => user.role === this.selectedRole);
        }
        
        if (this.selectedStatus !== 'ALL') {
          mockUsers = mockUsers.filter(user => user.status === this.selectedStatus);
        }
        
        if (this.selectedDepartment !== 'ALL') {
          mockUsers = mockUsers.filter(user => user.department === this.selectedDepartment);
        }
        
        // Apply date filter
        if (this.dateFilter !== 'ALL') {
          const now = new Date();
          const days = this.dateFilter === 'LAST_7_DAYS' ? 7 : 
                      this.dateFilter === 'LAST_30_DAYS' ? 30 : 90;
          const cutoffDate = new Date(now.getTime() - days * 24 * 60 * 60 * 1000);
          
          mockUsers = mockUsers.filter(user => {
            if (!user.lastLogin) return false;
            return new Date(user.lastLogin) >= cutoffDate;
          });
        }
        
        // Apply sorting
        this.sortUsers(mockUsers);
        
        // Apply pagination
        this.totalElements = mockUsers.length;
        this.totalPages = Math.ceil(this.totalElements / this.pageSize);
        const startIndex = this.currentPage * this.pageSize;
        const endIndex = startIndex + this.pageSize;
        
        this.users = mockUsers.slice(startIndex, endIndex);
        this.loading = false;
      } catch (error) {
        this.error = 'Failed to load users. Please try again.';
        this.loading = false;
        console.error('Error loading users:', error);
      }
    }, 800); // Simulate network delay
  }

  private generateMockUsers(): UserDto[] {
    const mockUsers: UserDto[] = [];
    const firstNames = ['John', 'Jane', 'Michael', 'Sarah', 'David', 'Emily', 'Robert', 'Lisa', 'Christopher', 'Jessica',
                       'Matthew', 'Ashley', 'Joshua', 'Amanda', 'Daniel', 'Melissa', 'James', 'Jennifer', 'Justin', 'Michelle'];
    const lastNames = ['Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis', 'Rodriguez', 'Martinez',
                      'Hernandez', 'Lopez', 'Gonzales', 'Wilson', 'Anderson', 'Thomas', 'Taylor', 'Moore', 'Jackson', 'Martin'];
    const departments = this.availableDepartments;
    const roles = this.availableRoles;
    const statuses = this.availableStatuses;
    
    for (let i = 1; i <= 147; i++) {
      const firstName = firstNames[Math.floor(Math.random() * firstNames.length)];
      const lastName = lastNames[Math.floor(Math.random() * lastNames.length)];
      const department = departments[Math.floor(Math.random() * departments.length)];
      const role = roles[Math.floor(Math.random() * roles.length)];
      const status = statuses[Math.floor(Math.random() * statuses.length)];
      
      const username = `${firstName.toLowerCase()}.${lastName.toLowerCase()}`;
      const email = `${username}@cbp.dhs.gov`;
      
      const createdDate = new Date(Date.now() - Math.random() * 365 * 24 * 60 * 60 * 1000);
      const lastLoginDate = status === UserStatus.ACTIVE ? 
        new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000) : null;
      
      const permissions = this.generatePermissionsForRole(role);
      
      mockUsers.push({
        id: `user-${i.toString().padStart(3, '0')}`,
        username,
        email,
        firstName,
        lastName,
        role,
        status,
        department,
        lastLogin: lastLoginDate?.toISOString(),
        createdAt: createdDate.toISOString(),
        updatedAt: new Date(createdDate.getTime() + Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(),
        isActive: status === UserStatus.ACTIVE,
        phoneNumber: `+1-${Math.floor(Math.random() * 900 + 100)}-${Math.floor(Math.random() * 900 + 100)}-${Math.floor(Math.random() * 9000 + 1000)}`,
        jobTitle: this.generateJobTitle(role, department),
        manager: i > 10 ? mockUsers[Math.floor(Math.random() * Math.min(10, mockUsers.length))]?.username : undefined,
        twoFactorEnabled: Math.random() > 0.3,
        failedLoginAttempts: status === UserStatus.LOCKED ? Math.floor(Math.random() * 5 + 3) : Math.floor(Math.random() * 3),
        passwordExpires: new Date(Date.now() + Math.random() * 90 * 24 * 60 * 60 * 1000).toISOString(),
        permissions,
        notes: Math.random() > 0.7 ? 'Special access granted by security team' : undefined
      });
    }
    
    return mockUsers;
  }

  private generatePermissionsForRole(role: UserRole): string[] {
    const basePermissions = ['countries.view'];
    
    switch (role) {
      case UserRole.ADMIN:
        return [...this.allPermissions];
      case UserRole.EDITOR:
        return [
          'countries.view', 'countries.edit', 'countries.create',
          'users.view', 'changeRequests.view', 'audit.view', 'data.export'
        ];
      case UserRole.ANALYST:
        return [
          'countries.view', 'audit.view', 'data.export', 'reports.generate'
        ];
      case UserRole.AUDITOR:
        return [
          'countries.view', 'users.view', 'changeRequests.view', 'audit.view', 'reports.generate'
        ];
      case UserRole.VIEWER:
      default:
        return ['countries.view', 'audit.view'];
    }
  }

  private generateJobTitle(role: UserRole, department: string): string {
    const titles = {
      [UserRole.ADMIN]: ['System Administrator', 'IT Director', 'Technical Lead'],
      [UserRole.EDITOR]: ['Data Manager', 'Senior Analyst', 'Operations Specialist'],
      [UserRole.ANALYST]: ['Business Analyst', 'Data Analyst', 'Research Specialist'],
      [UserRole.AUDITOR]: ['Compliance Officer', 'Internal Auditor', 'Quality Assurance Specialist'],
      [UserRole.VIEWER]: ['Staff Member', 'Associate', 'Coordinator']
    };
    
    const roleTitles = titles[role] || titles[UserRole.VIEWER];
    const baseTitle = roleTitles[Math.floor(Math.random() * roleTitles.length)];
    return `${baseTitle} - ${department}`;
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
  
  onFilterChange() {
    this.currentPage = 0;
    this.loadUsers();
  }
  
  sortBy(field: keyof UserDto) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.loadUsers();
  }
  
  private sortUsers(users: UserDto[]) {
    users.sort((a, b) => {
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
      this.loadUsers();
    }
  }

  viewUser(user: UserDto) {
    this.selectedUser = { ...user };
    this.isEditMode = false;
    this.formErrors = {};
    this.showModal = true;
  }
  
  editUser(user: UserDto) {
    this.selectedUser = { ...user };
    this.isEditMode = true;
    this.formErrors = {};
    this.showModal = true;
  }
  
  addUser() {
    this.selectedUser = {
      id: '',
      username: '',
      email: '',
      firstName: '',
      lastName: '',
      role: UserRole.VIEWER,
      status: UserStatus.PENDING,
      department: 'IT',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      isActive: true,
      twoFactorEnabled: false,
      failedLoginAttempts: 0,
      permissions: this.generatePermissionsForRole(UserRole.VIEWER)
    };
    this.isEditMode = true;
    this.formErrors = {};
    this.showModal = true;
  }
  
  confirmDelete(user: UserDto) {
    this.userToDelete = user;
    this.showDeleteConfirm = true;
  }
  
  deleteUser() {
    if (!this.userToDelete) return;
    
    // Simulate API call
    setTimeout(() => {
      this.successMessage = `User ${this.userToDelete?.firstName} ${this.userToDelete?.lastName} has been deactivated successfully`;
      this.toastService.showSuccess('User Deactivated', 'User has been successfully deactivated');
      this.showDeleteConfirm = false;
      this.userToDelete = null;
      this.loadUsers();
      
      setTimeout(() => {
        this.successMessage = null;
      }, 5000);
    }, 500);
  }
  
  cancelDelete() {
    this.showDeleteConfirm = false;
    this.userToDelete = null;
  }

  saveUser() {
    if (!this.selectedUser || !this.validateForm()) return;
    
    // Simulate API call
    setTimeout(() => {
      const isNew = !this.selectedUser?.id;
      this.successMessage = `User ${isNew ? 'created' : 'updated'} successfully`;
      this.toastService.showSuccess(
        isNew ? 'User Created' : 'User Updated', 
        `User has been ${isNew ? 'created' : 'updated'} successfully`
      );
      this.closeModal();
      this.loadUsers();
      
      setTimeout(() => {
        this.successMessage = null;
      }, 5000);
    }, 500);
  }
  
  private validateForm(): boolean {
    this.formErrors = {};
    
    if (!this.selectedUser) return false;
    
    if (!this.selectedUser.firstName?.trim()) {
      this.formErrors.firstName = 'First name is required';
    }
    
    if (!this.selectedUser.lastName?.trim()) {
      this.formErrors.lastName = 'Last name is required';
    }
    
    if (!this.selectedUser.username?.trim()) {
      this.formErrors.username = 'Username is required';
    }
    
    if (!this.selectedUser.email?.trim()) {
      this.formErrors.email = 'Email is required';
    } else if (!/^\S+@\S+\.\S+$/.test(this.selectedUser.email)) {
      this.formErrors.email = 'Please enter a valid email address';
    }
    
    return Object.keys(this.formErrors).length === 0;
  }

  closeModal() {
    this.showModal = false;
    this.selectedUser = null;
    this.isEditMode = false;
    this.formErrors = {};
  }
  
  // Bulk operations
  toggleSelectAll() {
    if (this.selectAll) {
      this.users.forEach(u => this.selectedUsers.add(u.id));
    } else {
      this.selectedUsers.clear();
    }
  }
  
  toggleSelection(userId: string) {
    if (this.selectedUsers.has(userId)) {
      this.selectedUsers.delete(userId);
    } else {
      this.selectedUsers.add(userId);
    }
    
    this.selectAll = this.selectedUsers.size === this.users.length;
  }
  
  isSelected(userId: string): boolean {
    return this.selectedUsers.has(userId);
  }
  
  openBulkModal(operation: typeof this.bulkOperation) {
    this.bulkOperation = operation;
    this.showBulkModal = true;
  }
  
  executeBulkOperation() {
    const selectedCount = this.selectedUsers.size;
    const operationName = this.bulkOperation?.replace('_', ' ') || '';
    
    // Simulate API call
    setTimeout(() => {
      this.successMessage = `Bulk ${operationName} completed for ${selectedCount} users`;
      this.toastService.showSuccess('Bulk Operation Complete', `${selectedCount} users ${operationName}d successfully`);
      this.showBulkModal = false;
      this.bulkOperation = null;
      this.selectedUsers.clear();
      this.selectAll = false;
      this.loadUsers();
      
      setTimeout(() => {
        this.successMessage = null;
      }, 5000);
    }, 1000);
  }
  
  cancelBulkOperation() {
    this.showBulkModal = false;
    this.bulkOperation = null;
  }
  
  exportSelected() {
    const selectedData = this.users.filter(u => this.selectedUsers.has(u.id));
    const csv = this.convertToCSV(selectedData);
    this.downloadCSV(csv, `users_export_${new Date().getTime()}.csv`);
  }
  
  exportAll() {
    // In production, this would call a backend endpoint for bulk export
    const allUsers = this.generateMockUsers();
    const csv = this.convertToCSV(allUsers);
    this.downloadCSV(csv, `all_users_export_${new Date().getTime()}.csv`);
  }
  
  private convertToCSV(data: UserDto[]): string {
    if (!data.length) return '';
    
    const headers = [
      'Username', 'Email', 'First Name', 'Last Name', 'Role', 'Status', 
      'Department', 'Job Title', 'Phone', 'Last Login', 'Created', '2FA Enabled'
    ];
    const rows = data.map(u => [
      u.username,
      u.email,
      u.firstName,
      u.lastName,
      u.role,
      u.status,
      u.department,
      u.jobTitle || '',
      u.phoneNumber || '',
      u.lastLogin ? new Date(u.lastLogin).toLocaleDateString() : 'Never',
      new Date(u.createdAt).toLocaleDateString(),
      u.twoFactorEnabled ? 'Yes' : 'No'
    ]);
    
    return [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${(cell || '').toString().replace(/"/g, '""')}"`).join(','))
    ].join('\n');
  }
  
  private downloadCSV(csv: string, filename: string) {
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    if (link.download !== undefined) {
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', filename);
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  }

  // Activity tracking
  viewActivity(user: UserDto) {
    this.selectedUser = user;
    this.loadingActivity = true;
    this.showActivityModal = true;
    
    // Simulate loading activity data
    setTimeout(() => {
      this.userActivities = this.generateMockActivity(user.id);
      this.loadingActivity = false;
    }, 800);
  }
  
  private generateMockActivity(userId: string): UserActivity[] {
    const actions = [
      'Login', 'Logout', 'View Countries', 'Edit Country', 'Create Change Request',
      'Approve Change Request', 'Export Data', 'Password Change', 'Profile Update'
    ];
    
    const activities: UserActivity[] = [];
    for (let i = 0; i < 25; i++) {
      const action = actions[Math.floor(Math.random() * actions.length)];
      const timestamp = new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000);
      
      activities.push({
        id: `activity-${i}`,
        userId,
        action,
        timestamp: timestamp.toISOString(),
        ipAddress: `192.168.1.${Math.floor(Math.random() * 254 + 1)}`,
        userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        resource: action.includes('Country') ? 'Countries' : action.includes('Change') ? 'Change Requests' : undefined,
        details: `${action} performed successfully`
      });
    }
    
    return activities.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
  }
  
  closeActivityModal() {
    this.showActivityModal = false;
    this.selectedUser = null;
    this.userActivities = [];
  }
  
  // Permission management
  viewPermissions(user: UserDto) {
    this.selectedUser = user;
    this.showPermissionModal = true;
  }
  
  closePermissionModal() {
    this.showPermissionModal = false;
    this.selectedUser = null;
  }
  
  togglePermission(permission: string) {
    if (!this.selectedUser) return;
    
    const index = this.selectedUser.permissions.indexOf(permission);
    if (index > -1) {
      this.selectedUser.permissions.splice(index, 1);
    } else {
      this.selectedUser.permissions.push(permission);
    }
  }
  
  hasPermission(permission: string): boolean {
    return this.selectedUser?.permissions.includes(permission) || false;
  }
  
  savePermissions() {
    // Simulate API call
    setTimeout(() => {
      this.successMessage = `Permissions updated for ${this.selectedUser?.firstName} ${this.selectedUser?.lastName}`;
      this.toastService.showSuccess('Permissions Updated', 'User permissions have been updated successfully');
      this.closePermissionModal();
      this.loadUsers();
      
      setTimeout(() => {
        this.successMessage = null;
      }, 5000);
    }, 500);
  }
  
  // Password reset
  resetPassword(user: UserDto) {
    // Simulate API call
    setTimeout(() => {
      this.successMessage = `Password reset email sent to ${user.email}`;
      this.toastService.showSuccess('Password Reset', `Password reset email sent to ${user.email}`);
      
      setTimeout(() => {
        this.successMessage = null;
      }, 5000);
    }, 500);
  }
  
  // Utility methods
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
    return this.selectedUsers.size > 0;
  }
  
  resetFilters() {
    this.searchTerm = '';
    this.selectedRole = 'ALL';
    this.selectedStatus = 'ALL';
    this.selectedDepartment = 'ALL';
    this.dateFilter = 'ALL';
    this.currentPage = 0;
    if (this.searchInput) {
      this.searchInput.nativeElement.value = '';
    }
    this.loadUsers();
  }

  isTableView(): boolean {
    return this.viewMode === 'table';
  }

  isCardView(): boolean {
    return this.viewMode === 'card';
  }

  setViewMode(mode: 'table' | 'card') {
    this.viewMode = mode;
  }

  trackByUserId(index: number, user: UserDto): string {
    return user.id;
  }

  trackByActivityId(index: number, activity: UserActivity): number {
    return index;
  }

  getMinValue(a: number, b: number): number {
    return Math.min(a, b);
  }

  formatDate(date: string | undefined): string {
    if (!date) return 'Never';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
  
  formatDateTime(date: string | undefined): string {
    if (!date) return 'Never';
    return new Date(date).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
  
  getSortIcon(field: keyof UserDto): string {
    if (this.sortField !== field) return 'unfold_more';
    return this.sortDirection === 'asc' ? 'expand_less' : 'expand_more';
  }
  
  get hasActiveFilters(): boolean {
    return this.searchTerm.length > 0 || 
           this.selectedRole !== 'ALL' || 
           this.selectedStatus !== 'ALL' ||
           this.selectedDepartment !== 'ALL' ||
           this.dateFilter !== 'ALL';
  }
  
  get selectedItems(): string[] {
    return Array.from(this.selectedUsers);
  }
  
  getStatusClass(status: UserStatus): string {
    return `cbp-status-badge--${status.toLowerCase()}`;
  }
  
  getStatusText(status: UserStatus): string {
    switch (status) {
      case UserStatus.ACTIVE: return 'Active';
      case UserStatus.INACTIVE: return 'Inactive';
      case UserStatus.SUSPENDED: return 'Suspended';
      case UserStatus.PENDING: return 'Pending';
      case UserStatus.LOCKED: return 'Locked';
      default: return status;
    }
  }
  
  getRoleDisplayName(role: UserRole): string {
    switch (role) {
      case UserRole.ADMIN: return 'Administrator';
      case UserRole.EDITOR: return 'Editor';
      case UserRole.VIEWER: return 'Viewer';
      case UserRole.ANALYST: return 'Analyst';
      case UserRole.AUDITOR: return 'Auditor';
      default: return role;
    }
  }
  
  getFullName(user: UserDto): string {
    return `${user.firstName} ${user.lastName}`;
  }
  
  onRoleChange() {
    if (this.selectedUser) {
      this.selectedUser.permissions = this.generatePermissionsForRole(this.selectedUser.role);
    }
  }
}