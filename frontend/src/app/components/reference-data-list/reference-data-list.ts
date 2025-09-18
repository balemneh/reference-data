import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ReferenceDataService } from '../../services/reference-data.service';
import { 
  ReferenceDataType, 
  BaseReferenceDataItem, 
  PagedResponse,
  SearchParams 
} from '../../models/reference-data.models';

@Component({
  selector: 'app-reference-data-list',
  templateUrl: './reference-data-list.html',
  styleUrls: ['./reference-data-list.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule]
})
export class ReferenceDataListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  dataType: string = '';
  referenceDataType?: ReferenceDataType;
  items: BaseReferenceDataItem[] = [];
  loading = false;
  error: string | null = null;

  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  pageSizes = [10, 20, 50, 100];

  // Search and filters
  searchForm = new FormGroup({
    query: new FormControl(''),
    active: new FormControl('all'),
    codeSystem: new FormControl('')
  });

  sortOptions = [
    { value: 'createdAt,desc', label: 'Newest First' },
    { value: 'createdAt,asc', label: 'Oldest First' },
    { value: 'updatedAt,desc', label: 'Recently Updated' }
  ];

  currentSort = 'createdAt,desc';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private referenceDataService: ReferenceDataService
  ) {}

  ngOnInit() {
    // Get data type from route
    this.route.params
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.dataType = params['type'];
        this.initializeComponent();
      });

    // Listen to search form changes
    this.searchForm.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => {
        this.currentPage = 0;
        this.loadItems();
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeComponent() {
    // Get reference data type configuration
    this.referenceDataService.getReferenceDataTypes()
      .pipe(takeUntil(this.destroy$))
      .subscribe(types => {
        this.referenceDataType = types.find(type => type.id === this.dataType);
        if (this.referenceDataType) {
          this.setupSortOptions();
          this.loadItems();
        } else {
          this.error = `Reference data type '${this.dataType}' not found`;
        }
      });
  }

  private setupSortOptions() {
    if (!this.referenceDataType) return;

    // Add dynamic sort options based on available fields
    const additionalSorts: { value: string; label: string }[] = [];
    
    this.referenceDataType.fields.forEach(field => {
      if (field.sortable) {
        additionalSorts.push(
          { value: `${field.name},asc`, label: `${field.displayName} (A-Z)` },
          { value: `${field.name},desc`, label: `${field.displayName} (Z-A)` }
        );
      }
    });

    this.sortOptions = [...this.sortOptions, ...additionalSorts];
  }

  loadItems() {
    if (!this.referenceDataType) return;

    this.loading = true;
    this.error = null;

    const searchParams: SearchParams = {
      page: this.currentPage,
      size: this.pageSize,
      sort: this.currentSort
    };

    // Add search query
    const formValue = this.searchForm.value;
    if (formValue.query?.trim()) {
      searchParams.query = formValue.query.trim();
    }

    // Add filters
    const filters: { [key: string]: any } = {};
    if (formValue.active && formValue.active !== 'all') {
      filters['active'] = formValue.active === 'true';
    }
    if (formValue.codeSystem?.trim()) {
      filters['codeSystem'] = formValue.codeSystem.trim();
    }

    if (Object.keys(filters).length > 0) {
      searchParams.filters = filters;
    }

    this.referenceDataService.getItems(this.dataType, searchParams)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: PagedResponse<BaseReferenceDataItem>) => {
          this.items = response.content;
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
          this.loading = false;
        },
        error: (error) => {
          this.error = 'Failed to load data. Please try again.';
          this.loading = false;
          console.error('Error loading items:', error);
        }
      });
  }

  onPageChange(page: number) {
    this.currentPage = page;
    this.loadItems();
  }

  onPageSizeChange(size: number) {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadItems();
  }

  onSortChange(sort: string) {
    this.currentSort = sort;
    this.currentPage = 0;
    this.loadItems();
  }

  clearSearch() {
    this.searchForm.reset({
      query: '',
      active: 'all',
      codeSystem: ''
    });
  }

  viewItem(item: BaseReferenceDataItem) {
    this.router.navigate(['/reference-data', this.dataType, item.id]);
  }

  editItem(item: BaseReferenceDataItem) {
    this.router.navigate(['/reference-data', this.dataType, item.id, 'edit']);
  }

  addNewItem() {
    this.router.navigate(['/reference-data', this.dataType, 'new']);
  }

  getFieldValue(item: any, fieldName: string): any {
    return item[fieldName] || '-';
  }

  formatFieldValue(value: any, fieldType: string): string {
    if (value === null || value === undefined || value === '') {
      return '-';
    }

    switch (fieldType) {
      case 'boolean':
        return value ? 'Yes' : 'No';
      case 'date':
        return new Date(value).toLocaleDateString();
      default:
        return value.toString();
    }
  }

  getDisplayFields(): any[] {
    if (!this.referenceDataType) return [];
    return this.referenceDataType.fields.filter(field => field.showInList);
  }

  getPaginationInfo(): string {
    const start = this.currentPage * this.pageSize + 1;
    const end = Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
    return `${start}-${end} of ${this.totalElements}`;
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPagesToShow = 5;
    const halfMax = Math.floor(maxPagesToShow / 2);
    
    let startPage = Math.max(0, this.currentPage - halfMax);
    let endPage = Math.min(this.totalPages - 1, startPage + maxPagesToShow - 1);
    
    // Adjust start page if we're near the end
    if (endPage - startPage < maxPagesToShow - 1) {
      startPage = Math.max(0, endPage - maxPagesToShow + 1);
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    
    return pages;
  }

  canUserPerformAction(action: string): boolean {
    if (!this.referenceDataType) return false;
    
    switch (action) {
      case 'create':
        return this.referenceDataType.permissions.create;
      case 'update':
        return this.referenceDataType.permissions.update;
      case 'delete':
        return this.referenceDataType.permissions.delete;
      default:
        return this.referenceDataType.permissions.read;
    }
  }

  getSystemCodes(): string[] {
    return this.referenceDataType ? this.referenceDataType.systemCodes : [];
  }
}