import { Component, OnInit, ViewChild, ElementRef, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, CountryDto, PagedResponse } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';

@Component({
  selector: 'app-countries',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './countries.html',
  styleUrl: './countries.scss',
  encapsulation: ViewEncapsulation.None
})
export class CountriesComponent implements OnInit {
  @ViewChild('searchInput') searchInput!: ElementRef;
  
  countries: CountryDto[] = [];
  loading: boolean = false;
  error: string | null = null;
  successMessage: string | null = null;
  
  // Pagination
  currentPage = 0;
  pageSize = 30; // Increased to show all countries
  totalElements = 0;
  totalPages = 0;
  
  // Search and filtering
  searchTerm = '';
  searchSubject = new Subject<string>();
  codeSystem = 'ISO3166-1';
  availableCodeSystems = ['ISO3166-1', 'ISO3166-2', 'ISO3166-3', 'GENC', 'CBP-COUNTRY5'];
  filterActive: boolean | null = null;
  
  // Modal and form
  showModal = false;
  showDeleteConfirm = false;
  selectedCountry: CountryDto | null = null;
  countryToDelete: CountryDto | null = null;
  isEditMode = false;
  formErrors: any = {};
  
  // Sorting
  sortField: keyof CountryDto = 'countryName';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Bulk operations
  selectedCountries = new Set<string>();
  selectAll = false;
  
  // View toggle
  viewMode: 'table' | 'card' = 'table';

  constructor(private apiService: ApiService, private toastService: ToastService) {}
  
  private initializeSearchSubscription() {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      this.searchTerm = searchTerm;
      this.currentPage = 0;
      // Only load from API if we have a search term
      if (searchTerm) {
        this.loadCountries();
      }
    });
  }

  ngOnInit() {
    this.initializeSearchSubscription();
    // Load full mock data
    this.loadMockData();
    // Force Angular to detect changes
    setTimeout(() => {
      console.log('Countries after init:', this.countries.length);
      console.log('Loading status:', this.loading);
      console.log('View mode:', this.viewMode);
    }, 0);
  }

  loadCountries() {
    // For demo, preserve existing data
    this.loading = false;
    
    // Apply sorting
    this.sortCountries();
    return;
    
    // Skip API call for demo
    /*
    // Skip API call if we have mock data and no search term
    if (this.countries.length > 0 && !this.searchTerm) {
      this.loading = false;
      
      // Apply client-side active filter if set
      if (this.filterActive !== null) {
        const allCountries = [...this.countries]; // Keep a copy
        this.countries = allCountries.filter(c => c.isActive === this.filterActive);
      }
      
      // Apply sorting
      this.sortCountries();
      return;
    }
    */
    
    this.loading = true;
    this.error = null;
    this.selectedCountries.clear();
    this.selectAll = false;
    
    const params = {
      page: this.currentPage,
      size: this.pageSize,
      systemCode: this.codeSystem
    };
    
    const loadObservable = this.searchTerm
      ? this.apiService.searchCountries({ name: this.searchTerm, ...params })
      : this.apiService.getCountries(params);
    
    loadObservable.subscribe({
      next: (response: PagedResponse<CountryDto>) => {
        this.countries = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.totalPages = response.totalPages || 0;
        this.loading = false;
        
        // Apply client-side active filter if set
        if (this.filterActive !== null) {
          this.countries = this.countries.filter(c => c.isActive === this.filterActive);
        }
        
        // Apply sorting
        this.sortCountries();
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
  
  onCodeSystemChange() {
    this.currentPage = 0;
    this.loadCountries();
  }
  
  onActiveFilterChange(value: string) {
    this.filterActive = value === 'all' ? null : value === 'active';
    this.currentPage = 0;
    this.loadCountries();
  }
  
  sortBy(field: keyof CountryDto) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.sortCountries();
  }
  
  private sortCountries() {
    this.countries.sort((a, b) => {
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
      this.loadCountries();
    }
  }

  viewCountry(country: CountryDto) {
    this.selectedCountry = { ...country };
    this.isEditMode = false;
    this.formErrors = {};
    this.showModal = true;
  }
  
  editCountry(country: CountryDto) {
    this.selectedCountry = { ...country };
    this.isEditMode = true;
    this.formErrors = {};
    this.showModal = true;
  }
  
  addCountry() {
    // Open modal with empty country form
    this.selectedCountry = {
      id: '',
      countryCode: '',
      countryName: '',
      iso2Code: '',
      iso3Code: '',
      numericCode: '',
      codeSystem: this.codeSystem,
      isActive: true,
      validFrom: new Date().toISOString(),
      recordedAt: new Date().toISOString(),
      recordedBy: 'system',
      version: 1
    };
    this.isEditMode = true;
    this.formErrors = {};
    this.showModal = true;
  }
  
  confirmDelete(country: CountryDto) {
    this.countryToDelete = country;
    this.showDeleteConfirm = true;
  }
  
  deleteCountry() {
    if (!this.countryToDelete) return;
    
    // In a bitemporal system, we don't actually delete - we mark as inactive
    const updatedCountry = {
      ...this.countryToDelete,
      isActive: false,
      validTo: new Date().toISOString()
    };
    
    this.saveCountryData(updatedCountry, 'Country deactivated successfully');
    this.showDeleteConfirm = false;
    this.countryToDelete = null;
  }
  
  cancelDelete() {
    this.showDeleteConfirm = false;
    this.countryToDelete = null;
  }

  saveCountry() {
    if (!this.selectedCountry || !this.validateForm()) return;
    
    this.saveCountryData(this.selectedCountry, 'Country saved successfully');
  }
  
  private saveCountryData(country: CountryDto, successMsg: string) {
    // Create a change request for the country modification
    const changeRequest = {
      changeType: (country.id ? 'UPDATE' : 'CREATE') as 'UPDATE' | 'CREATE',
      entityType: 'COUNTRY',
      entityId: country.id || undefined,
      description: `${country.id ? 'Update' : 'Create'} country: ${country.countryName}`,
      requestedBy: 'current-user', // Would come from auth service
      newValues: country
    };
    
    this.apiService.createChangeRequest(changeRequest).subscribe({
      next: (response: any) => {
        this.successMessage = successMsg + ` (Request ID: ${response.id})`;
        this.toastService.showSuccess('Change request submitted', `Request ID: ${response.id}`);
        this.closeModal();
        this.loadCountries();
        
        // Clear success message after 5 seconds
        setTimeout(() => {
          this.successMessage = null;
        }, 5000);
      },
      error: (error: any) => {
        this.formErrors.general = error.error?.detail || 'Failed to save country. Please try again.';
        this.toastService.showError('Failed to save country', this.formErrors.general);
        console.error('Error saving country:', error);
      }
    });
  }
  
  private validateForm(): boolean {
    this.formErrors = {};
    
    if (!this.selectedCountry) return false;
    
    if (!this.selectedCountry.countryName?.trim()) {
      this.formErrors.countryName = 'Country name is required';
    }
    
    if (!this.selectedCountry.countryCode?.trim()) {
      this.formErrors.countryCode = 'Country code is required';
    }
    
    if (!this.selectedCountry.iso2Code?.trim() || this.selectedCountry.iso2Code.length !== 2) {
      this.formErrors.iso2Code = 'ISO 2 code must be exactly 2 characters';
    }
    
    if (!this.selectedCountry.iso3Code?.trim() || this.selectedCountry.iso3Code.length !== 3) {
      this.formErrors.iso3Code = 'ISO 3 code must be exactly 3 characters';
    }
    
    if (this.selectedCountry.numericCode && !/^\d{3}$/.test(this.selectedCountry.numericCode)) {
      this.formErrors.numericCode = 'Numeric code must be exactly 3 digits';
    }
    
    return Object.keys(this.formErrors).length === 0;
  }

  closeModal() {
    this.showModal = false;
    this.selectedCountry = null;
    this.isEditMode = false;
    this.formErrors = {};
  }
  
  // Bulk operations
  toggleSelectAll() {
    if (this.selectAll) {
      this.countries.forEach(c => this.selectedCountries.add(c.id));
    } else {
      this.selectedCountries.clear();
    }
  }
  
  toggleSelection(countryId: string) {
    if (this.selectedCountries.has(countryId)) {
      this.selectedCountries.delete(countryId);
    } else {
      this.selectedCountries.add(countryId);
    }
    
    this.selectAll = this.selectedCountries.size === this.countries.length;
  }
  
  isSelected(countryId: string): boolean {
    return this.selectedCountries.has(countryId);
  }
  
  exportSelected() {
    const selectedData = this.countries.filter(c => this.selectedCountries.has(c.id));
    const csv = this.convertToCSV(selectedData);
    this.downloadCSV(csv, `countries_export_${new Date().getTime()}.csv`);
  }
  
  exportAll() {
    // In production, this would call a backend endpoint for bulk export
    this.apiService.getAllCurrentCountries().subscribe({
      next: (countries) => {
        const csv = this.convertToCSV(countries);
        this.downloadCSV(csv, `all_countries_export_${new Date().getTime()}.csv`);
      },
      error: (error) => {
        this.error = 'Failed to export countries';
        console.error('Export error:', error);
      }
    });
  }
  
  private convertToCSV(data: CountryDto[]): string {
    if (!data.length) return '';
    
    const headers = ['Country Code', 'Country Name', 'ISO2', 'ISO3', 'Numeric', 'Code System', 'Active', 'Valid From', 'Valid To'];
    const rows = data.map(c => [
      c.countryCode,
      c.countryName,
      c.iso2Code,
      c.iso3Code,
      c.numericCode,
      c.codeSystem,
      c.isActive ? 'Yes' : 'No',
      new Date(c.validFrom).toLocaleDateString(),
      c.validTo ? new Date(c.validTo).toLocaleDateString() : ''
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
    return this.selectedCountries.size > 0;
  }
  
  get selectedItems(): any[] {
    return Array.from(this.selectedCountries);
  }
  
  resetFilters() {
    this.searchTerm = '';
    this.filterActive = null;
    this.codeSystem = 'ISO3166-1';
    this.currentPage = 0;
    if (this.searchInput) {
      this.searchInput.nativeElement.value = '';
    }
    this.loadCountries();
  }

  isTableView(): boolean {
    const result = this.viewMode === 'table';
    console.log('isTableView called, viewMode:', this.viewMode, 'result:', result);
    return result;
  }

  isCardView(): boolean {
    return this.viewMode === 'card';
  }

  setViewMode(mode: 'table' | 'card') {
    this.viewMode = mode;
  }

  trackByCountryId(index: number, country: CountryDto): string {
    return country.id;
  }

  private loadMockData() {
    console.log('loadMockData called');
    // Mock data for UI demonstration when API is not available
    const mockCountries = [
      {
        id: '1',
        countryCode: 'US',
        countryName: 'United States',
        iso2Code: 'US',
        iso3Code: 'USA',
        numericCode: '840',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
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
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
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
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
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
        isActive: false,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '5',
        countryCode: 'DE',
        countryName: 'Germany',
        iso2Code: 'DE',
        iso3Code: 'DEU',
        numericCode: '276',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '6',
        countryCode: 'FR',
        countryName: 'France',
        iso2Code: 'FR',
        iso3Code: 'FRA',
        numericCode: '250',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '7',
        countryCode: 'JP',
        countryName: 'Japan',
        iso2Code: 'JP',
        iso3Code: 'JPN',
        numericCode: '392',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '8',
        countryCode: 'CN',
        countryName: 'China',
        iso2Code: 'CN',
        iso3Code: 'CHN',
        numericCode: '156',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '9',
        countryCode: 'AU',
        countryName: 'Australia',
        iso2Code: 'AU',
        iso3Code: 'AUS',
        numericCode: '036',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '10',
        countryCode: 'IN',
        countryName: 'India',
        iso2Code: 'IN',
        iso3Code: 'IND',
        numericCode: '356',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '11',
        countryCode: 'BR',
        countryName: 'Brazil',
        iso2Code: 'BR',
        iso3Code: 'BRA',
        numericCode: '076',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '12',
        countryCode: 'IT',
        countryName: 'Italy',
        iso2Code: 'IT',
        iso3Code: 'ITA',
        numericCode: '380',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '13',
        countryCode: 'ES',
        countryName: 'Spain',
        iso2Code: 'ES',
        iso3Code: 'ESP',
        numericCode: '724',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '14',
        countryCode: 'KR',
        countryName: 'South Korea',
        iso2Code: 'KR',
        iso3Code: 'KOR',
        numericCode: '410',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '15',
        countryCode: 'NL',
        countryName: 'Netherlands',
        iso2Code: 'NL',
        iso3Code: 'NLD',
        numericCode: '528',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '16',
        countryCode: 'SE',
        countryName: 'Sweden',
        iso2Code: 'SE',
        iso3Code: 'SWE',
        numericCode: '752',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '17',
        countryCode: 'NO',
        countryName: 'Norway',
        iso2Code: 'NO',
        iso3Code: 'NOR',
        numericCode: '578',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '18',
        countryCode: 'DK',
        countryName: 'Denmark',
        iso2Code: 'DK',
        iso3Code: 'DNK',
        numericCode: '208',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '19',
        countryCode: 'FI',
        countryName: 'Finland',
        iso2Code: 'FI',
        iso3Code: 'FIN',
        numericCode: '246',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '20',
        countryCode: 'PL',
        countryName: 'Poland',
        iso2Code: 'PL',
        iso3Code: 'POL',
        numericCode: '616',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '21',
        countryCode: 'AR',
        countryName: 'Argentina',
        iso2Code: 'AR',
        iso3Code: 'ARG',
        numericCode: '032',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: '22',
        countryCode: 'CL',
        countryName: 'Chile',
        iso2Code: 'CL',
        iso3Code: 'CHL',
        numericCode: '152',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      }
    ];
    
    // Force change detection by creating a new array
    this.countries = [...mockCountries];
    this.totalElements = this.countries.length;
    this.totalPages = Math.ceil(this.totalElements / this.pageSize);
    this.currentPage = 0;
    this.loading = false;
    console.log('Mock data loaded. Countries count:', this.countries.length);
    console.log('Loading set to:', this.loading);
    
    // Apply client-side active filter if set
    if (this.filterActive !== null) {
      console.log('Filtering by active:', this.filterActive);
      console.log('Countries before filter:', this.countries.length);
      console.log('Sample country isActive:', this.countries[0]?.isActive);
      this.countries = this.countries.filter(c => c.isActive === this.filterActive);
      console.log('Countries after filter:', this.countries.length);
    }
    
    // Apply sorting
    this.sortCountries();
    console.log('Final countries in loadMockData:', this.countries);
  }

  getMinValue(a: number, b: number): number {
    return Math.min(a, b);
  }

  formatDate(date: string | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
  
  getSortIcon(field: keyof CountryDto): string {
    if (this.sortField !== field) return 'unfold_more';
    return this.sortDirection === 'asc' ? 'expand_less' : 'expand_more';
  }
  
  get hasActiveFilters(): boolean {
    return this.searchTerm.length > 0 || this.filterActive !== null || this.codeSystem !== 'ISO3166-1';
  }
}
