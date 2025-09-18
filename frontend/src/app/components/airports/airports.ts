import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, AirportDto, PagedResponse } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';

@Component({
  selector: 'app-airports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './airports.html',
  styleUrl: './airports.scss'
})
export class AirportsComponent implements OnInit {
  @ViewChild('searchInput') searchInput!: ElementRef;
  
  airports: AirportDto[] = [];
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
  codeSystem = 'IATA';
  availableCodeSystems = ['IATA', 'ICAO', 'FAA-LID', 'GENC'];
  filterActive: boolean | null = null;
  filterAirportType: string | null = null;
  filterHubSize: string | null = null;
  filterCountry: string | null = null;
  filterCustoms: boolean | null = null;
  availableAirportTypes = ['INTERNATIONAL', 'DOMESTIC', 'REGIONAL', 'MILITARY', 'PRIVATE'];
  availableHubSizes = ['LARGE_HUB', 'MEDIUM_HUB', 'SMALL_HUB', 'NON_HUB'];
  availableStatuses = ['ACTIVE', 'INACTIVE', 'SEASONAL', 'CLOSED'];
  
  // Modal and form
  showModal = false;
  showDeleteConfirm = false;
  selectedAirport: AirportDto | null = null;
  airportToDelete: AirportDto | null = null;
  isEditMode = false;
  formErrors: any = {};
  
  // Sorting
  sortField: keyof AirportDto = 'airportName';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Bulk operations
  selectedAirports = new Set<string>();
  selectAll = false;
  
  // View toggle
  viewMode: 'table' | 'card' | 'map' = 'table';

  constructor(private apiService: ApiService, private toastService: ToastService) {}
  
  private initializeSearchSubscription() {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      this.searchTerm = searchTerm;
      this.currentPage = 0;
      this.loadAirports();
    });
  }

  ngOnInit() {
    this.initializeSearchSubscription();
    // Try to load from API first
    this.loadAirports();
  }

  loadAirports() {
    this.loading = true;
    this.error = null;
    this.selectedAirports.clear();
    this.selectAll = false;
    
    const params = {
      page: this.currentPage,
      size: this.pageSize,
      codeSystem: this.codeSystem
    };
    
    const loadObservable = this.searchTerm
      ? this.apiService.searchAirports({ name: this.searchTerm, ...params })
      : this.apiService.getAirports(params);
    
    loadObservable.subscribe({
      next: (response: PagedResponse<AirportDto>) => {
        this.airports = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.totalPages = response.totalPages || 0;
        this.loading = false;
        
        // Apply client-side filters
        this.applyClientSideFilters();
        
        // Apply sorting
        this.sortAirports();
      },
      error: (error) => {
        console.error('Failed to load airports from API:', error);
        this.loading = false;
        this.error = `Failed to connect to API (${error.status || 'Network Error'}). Please check if the backend service is running at ${this.apiService['baseUrl']}.`;
        // Load mock data as fallback
        
        
      }
    });
  }

  private applyClientSideFilters() {
    let filteredAirports = [...this.airports];
    
    if (this.filterActive !== null) {
      filteredAirports = filteredAirports.filter(a => a.isActive === this.filterActive);
    }
    
    if (this.filterAirportType) {
      filteredAirports = filteredAirports.filter(a => a.airportType === this.filterAirportType);
    }
    
    if (this.filterHubSize) {
      filteredAirports = filteredAirports.filter(a => a.hubSize === this.filterHubSize);
    }
    
    if (this.filterCountry) {
      filteredAirports = filteredAirports.filter(a => a.countryCode === this.filterCountry);
    }
    
    if (this.filterCustoms !== null) {
      filteredAirports = filteredAirports.filter(a => a.hasCustoms === this.filterCustoms);
    }
    
    this.airports = filteredAirports;
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
    this.loadAirports();
  }
  
  onActiveFilterChange(value: string) {
    this.filterActive = value === 'all' ? null : value === 'active';
    this.currentPage = 0;
    this.loadAirports();
  }
  
  onAirportTypeFilterChange(value: string) {
    this.filterAirportType = value === 'all' ? null : value;
    this.currentPage = 0;
    this.loadAirports();
  }
  
  onHubSizeFilterChange(value: string) {
    this.filterHubSize = value === 'all' ? null : value;
    this.currentPage = 0;
    this.loadAirports();
  }
  
  onCustomsFilterChange(value: string) {
    this.filterCustoms = value === 'all' ? null : value === 'yes';
    this.currentPage = 0;
    this.loadAirports();
  }
  
  sortBy(field: keyof AirportDto) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.sortAirports();
  }
  
  private sortAirports() {
    this.airports.sort((a, b) => {
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
      this.loadAirports();
    }
  }

  viewAirport(airport: AirportDto) {
    this.selectedAirport = { ...airport };
    this.isEditMode = false;
    this.formErrors = {};
    this.showModal = true;
  }
  
  editAirport(airport: AirportDto) {
    this.selectedAirport = { ...airport };
    this.isEditMode = true;
    this.formErrors = {};
    this.showModal = true;
  }
  
  addAirport() {
    // Open modal with empty airport form
    this.selectedAirport = {
      id: '',
      iataCode: '',
      icaoCode: '',
      airportName: '',
      city: '',
      countryCode: '',
      stateProvince: '',
      airportType: 'DOMESTIC',
      hubSize: 'NON_HUB',
      hasCustoms: false,
      status: 'ACTIVE',
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
  
  confirmDelete(airport: AirportDto) {
    this.airportToDelete = airport;
    this.showDeleteConfirm = true;
  }
  
  deleteAirport() {
    if (!this.airportToDelete) return;
    
    // In a bitemporal system, we don't actually delete - we mark as inactive
    const updatedAirport = {
      ...this.airportToDelete,
      isActive: false,
      validTo: new Date().toISOString()
    };
    
    this.saveAirportData(updatedAirport, 'Airport deactivated successfully');
    this.showDeleteConfirm = false;
    this.airportToDelete = null;
  }
  
  cancelDelete() {
    this.showDeleteConfirm = false;
    this.airportToDelete = null;
  }

  saveAirport() {
    if (!this.selectedAirport || !this.validateForm()) return;
    
    this.saveAirportData(this.selectedAirport, 'Airport saved successfully');
  }
  
  private saveAirportData(airport: AirportDto, successMsg: string) {
    // Create a change request for the airport modification
    const changeRequest = {
      changeType: (airport.id ? 'UPDATE' : 'CREATE') as 'UPDATE' | 'CREATE',
      entityType: 'AIRPORT',
      entityId: airport.id || undefined,
      description: `${airport.id ? 'Update' : 'Create'} airport: ${airport.airportName}`,
      requestedBy: 'current-user', // Would come from auth service
      newValues: airport
    };
    
    this.apiService.createChangeRequest(changeRequest).subscribe({
      next: (response: any) => {
        this.successMessage = successMsg + ` (Request ID: ${response.id})`;
        this.toastService.showSuccess('Change request submitted', `Request ID: ${response.id}`);
        this.closeModal();
        this.loadAirports();
        
        // Clear success message after 5 seconds
        setTimeout(() => {
          this.successMessage = null;
        }, 5000);
      },
      error: (error: any) => {
        this.formErrors.general = error.error?.detail || 'Failed to save airport. Please try again.';
        this.toastService.showError('Failed to save airport', this.formErrors.general);
        console.error('Error saving airport:', error);
      }
    });
  }
  
  private validateForm(): boolean {
    this.formErrors = {};
    
    if (!this.selectedAirport) return false;
    
    if (!this.selectedAirport.airportName?.trim()) {
      this.formErrors.airportName = 'Airport name is required';
    }
    
    if (!this.selectedAirport.iataCode?.trim() || this.selectedAirport.iataCode.length !== 3) {
      this.formErrors.iataCode = 'IATA code must be exactly 3 characters';
    }
    
    if (!this.selectedAirport.icaoCode?.trim() || this.selectedAirport.icaoCode.length !== 4) {
      this.formErrors.icaoCode = 'ICAO code must be exactly 4 characters';
    }
    
    if (!this.selectedAirport.city?.trim()) {
      this.formErrors.city = 'City is required';
    }
    
    if (!this.selectedAirport.countryCode?.trim()) {
      this.formErrors.countryCode = 'Country code is required';
    }
    
    if (this.selectedAirport.elevationFeet !== undefined && this.selectedAirport.elevationFeet < -1000) {
      this.formErrors.elevationFeet = 'Elevation must be greater than -1000 feet';
    }
    
    if (this.selectedAirport.runwayCount !== undefined && this.selectedAirport.runwayCount < 0) {
      this.formErrors.runwayCount = 'Runway count cannot be negative';
    }
    
    if (this.selectedAirport.longestRunwayFeet !== undefined && this.selectedAirport.longestRunwayFeet < 0) {
      this.formErrors.longestRunwayFeet = 'Runway length cannot be negative';
    }
    
    if (this.selectedAirport.terminalCount !== undefined && this.selectedAirport.terminalCount < 0) {
      this.formErrors.terminalCount = 'Terminal count cannot be negative';
    }
    
    return Object.keys(this.formErrors).length === 0;
  }

  closeModal() {
    this.showModal = false;
    this.selectedAirport = null;
    this.isEditMode = false;
    this.formErrors = {};
  }
  
  // Bulk operations
  toggleSelectAll() {
    if (this.selectAll) {
      this.airports.forEach(a => this.selectedAirports.add(a.id));
    } else {
      this.selectedAirports.clear();
    }
  }
  
  toggleSelection(airportId: string) {
    if (this.selectedAirports.has(airportId)) {
      this.selectedAirports.delete(airportId);
    } else {
      this.selectedAirports.add(airportId);
    }
    
    this.selectAll = this.selectedAirports.size === this.airports.length;
  }
  
  isSelected(airportId: string): boolean {
    return this.selectedAirports.has(airportId);
  }
  
  exportSelected() {
    const selectedData = this.airports.filter(a => this.selectedAirports.has(a.id));
    const csv = this.convertToCSV(selectedData);
    this.downloadCSV(csv, `airports_export_${new Date().getTime()}.csv`);
  }
  
  exportAll() {
    // In production, this would call a backend endpoint for bulk export
    this.apiService.getAllCurrentAirports().subscribe({
      next: (airports) => {
        const csv = this.convertToCSV(airports);
        this.downloadCSV(csv, `all_airports_export_${new Date().getTime()}.csv`);
      },
      error: (error) => {
        this.error = 'Failed to export airports';
        console.error('Export error:', error);
      }
    });
  }
  
  private convertToCSV(data: AirportDto[]): string {
    if (!data.length) return '';
    
    const headers = [
      'IATA Code', 'ICAO Code', 'Airport Name', 'City', 'Country', 'State/Province',
      'Airport Type', 'Hub Size', 'Status', 'Has Customs', 'Elevation (ft)', 'Runway Count',
      'Longest Runway (ft)', 'Terminal Count', 'Time Zone', 'Active', 'Valid From', 'Valid To'
    ];
    const rows = data.map(a => [
      a.iataCode,
      a.icaoCode,
      a.airportName,
      a.city,
      a.countryCode,
      a.stateProvince || '',
      a.airportType,
      a.hubSize,
      a.status,
      a.hasCustoms ? 'Yes' : 'No',
      a.elevationFeet || '',
      a.runwayCount || '',
      a.longestRunwayFeet || '',
      a.terminalCount || '',
      a.timeZone || '',
      a.isActive ? 'Yes' : 'No',
      new Date(a.validFrom).toLocaleDateString(),
      a.validTo ? new Date(a.validTo).toLocaleDateString() : ''
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
    return this.selectedAirports.size > 0;
  }
  
  resetFilters() {
    this.searchTerm = '';
    this.filterActive = null;
    this.filterAirportType = null;
    this.filterHubSize = null;
    this.filterCountry = null;
    this.filterCustoms = null;
    this.codeSystem = 'IATA';
    this.currentPage = 0;
    if (this.searchInput) {
      this.searchInput.nativeElement.value = '';
    }
    this.loadAirports();
  }

  isTableView(): boolean {
    return this.viewMode === 'table';
  }

  isCardView(): boolean {
    return this.viewMode === 'card';
  }

  isMapView(): boolean {
    return this.viewMode === 'map';
  }

  setViewMode(mode: 'table' | 'card' | 'map') {
    this.viewMode = mode;
  }

  trackByAirportId(index: number, airport: AirportDto): string {
    return airport.id;
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
  
  getSortIcon(field: keyof AirportDto): string {
    if (this.sortField !== field) return 'unfold_more';
    return this.sortDirection === 'asc' ? 'expand_less' : 'expand_more';
  }
  
  get hasActiveFilters(): boolean {
    return this.searchTerm.length > 0 || 
           this.filterActive !== null || 
           this.codeSystem !== 'IATA' || 
           this.filterAirportType !== null ||
           this.filterHubSize !== null ||
           this.filterCountry !== null ||
           this.filterCustoms !== null;
  }
  
  get selectedItems(): string[] {
    return Array.from(this.selectedAirports);
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'cbp-status-badge--active';
      case 'SEASONAL': return 'cbp-status-badge--seasonal';
      case 'INACTIVE': return 'cbp-status-badge--inactive';
      case 'CLOSED': return 'cbp-status-badge--closed';
      default: return 'cbp-status-badge--inactive';
    }
  }

  getHubSizeDisplay(hubSize: string): string {
    switch (hubSize) {
      case 'LARGE_HUB': return 'Large Hub';
      case 'MEDIUM_HUB': return 'Medium Hub';
      case 'SMALL_HUB': return 'Small Hub';
      case 'NON_HUB': return 'Non-Hub';
      default: return hubSize;
    }
  }

  getAirportTypeDisplay(type: string): string {
    switch (type) {
      case 'INTERNATIONAL': return 'International';
      case 'DOMESTIC': return 'Domestic';
      case 'REGIONAL': return 'Regional';
      case 'MILITARY': return 'Military';
      case 'PRIVATE': return 'Private';
      default: return type;
    }
  }

//   private loadMockData() {
//     // Mock data for UI demonstration when API is not available
//     this.loading = false;
//     this.airports = [
//       {
//         id: 'airport-001',
//         iataCode: 'LAX',
//         icaoCode: 'KLAX',
//         airportName: 'Los Angeles International Airport',
//         city: 'Los Angeles',
//         countryCode: 'US',
//         stateProvince: 'California',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 33.9425, longitude: -118.4081 },
//         timeZone: 'America/Los_Angeles',
//         elevationFeet: 125,
//         runwayCount: 4,
//         longestRunwayFeet: 12091,
//         terminalCount: 9,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.flylax.com',
//         phone: '+1-855-463-5252',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-002',
//         iataCode: 'JFK',
//         icaoCode: 'KJFK',
//         airportName: 'John F. Kennedy International Airport',
//         city: 'New York',
//         countryCode: 'US',
//         stateProvince: 'New York',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 40.6413, longitude: -73.7781 },
//         timeZone: 'America/New_York',
//         elevationFeet: 13,
//         runwayCount: 4,
//         longestRunwayFeet: 14511,
//         terminalCount: 6,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.jfkairport.com',
//         phone: '+1-718-244-4444',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-003',
//         iataCode: 'LHR',
//         icaoCode: 'EGLL',
//         airportName: 'London Heathrow Airport',
//         city: 'London',
//         countryCode: 'GB',
//         stateProvince: 'England',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 51.4700, longitude: -0.4543 },
//         timeZone: 'Europe/London',
//         elevationFeet: 83,
//         runwayCount: 2,
//         longestRunwayFeet: 12802,
//         terminalCount: 5,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.heathrow.com',
//         phone: '+44-844-335-1801',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-004',
//         iataCode: 'DXB',
//         icaoCode: 'OMDB',
//         airportName: 'Dubai International Airport',
//         city: 'Dubai',
//         countryCode: 'AE',
//         stateProvince: 'Dubai',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 25.2532, longitude: 55.3657 },
//         timeZone: 'Asia/Dubai',
//         elevationFeet: 62,
//         runwayCount: 2,
//         longestRunwayFeet: 13124,
//         terminalCount: 3,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.dubaiairports.ae',
//         phone: '+971-4-224-5555',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-005',
//         iataCode: 'BOI',
//         icaoCode: 'KBOI',
//         airportName: 'Boise Airport',
//         city: 'Boise',
//         countryCode: 'US',
//         stateProvince: 'Idaho',
//         airportType: 'DOMESTIC',
//         hubSize: 'SMALL_HUB',
//         coordinates: { latitude: 43.5644, longitude: -116.2228 },
//         timeZone: 'America/Boise',
//         elevationFeet: 2871,
//         runwayCount: 3,
//         longestRunwayFeet: 10000,
//         terminalCount: 1,
//         hasCustoms: false,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.iflyboise.com',
//         phone: '+1-208-383-3110',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-006',
//         iataCode: 'TEX',
//         icaoCode: 'KTEX',
//         airportName: 'Telluride Regional Airport',
//         city: 'Telluride',
//         countryCode: 'US',
//         stateProvince: 'Colorado',
//         airportType: 'REGIONAL',
//         hubSize: 'NON_HUB',
//         coordinates: { latitude: 37.9538, longitude: -107.9085 },
//         timeZone: 'America/Denver',
//         elevationFeet: 9078,
//         runwayCount: 1,
//         longestRunwayFeet: 7111,
//         terminalCount: 1,
//         hasCustoms: false,
//         status: 'INACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.tellurideairport.com',
//         phone: '+1-970-728-5313',
//         isActive: false,
//         validFrom: new Date().toISOString(),
//         validTo: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-007',
//         iataCode: 'ORD',
//         icaoCode: 'KORD',
//         airportName: "O'Hare International Airport",
//         city: 'Chicago',
//         countryCode: 'US',
//         stateProvince: 'Illinois',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 41.9742, longitude: -87.9073 },
//         timeZone: 'America/Chicago',
//         elevationFeet: 680,
//         runwayCount: 7,
//         longestRunwayFeet: 13000,
//         terminalCount: 4,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.flychicago.com/ohare',
//         phone: '+1-800-832-6352',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-008',
//         iataCode: 'DFW',
//         icaoCode: 'KDFW',
//         airportName: 'Dallas/Fort Worth International Airport',
//         city: 'Dallas',
//         countryCode: 'US',
//         stateProvince: 'Texas',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 32.8975, longitude: -97.0407 },
//         timeZone: 'America/Chicago',
//         elevationFeet: 607,
//         runwayCount: 7,
//         longestRunwayFeet: 13401,
//         terminalCount: 5,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.dfwairport.com',
//         phone: '+1-972-973-3112',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-009',
//         iataCode: 'ATL',
//         icaoCode: 'KATL',
//         airportName: 'Hartsfield-Jackson Atlanta International Airport',
//         city: 'Atlanta',
//         countryCode: 'US',
//         stateProvince: 'Georgia',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 33.6407, longitude: -84.4277 },
//         timeZone: 'America/New_York',
//         elevationFeet: 1026,
//         runwayCount: 5,
//         longestRunwayFeet: 12390,
//         terminalCount: 2,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.atl.com',
//         phone: '+1-800-897-1910',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-010',
//         iataCode: 'SFO',
//         icaoCode: 'KSFO',
//         airportName: 'San Francisco International Airport',
//         city: 'San Francisco',
//         countryCode: 'US',
//         stateProvince: 'California',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 37.6213, longitude: -122.3790 },
//         timeZone: 'America/Los_Angeles',
//         elevationFeet: 13,
//         runwayCount: 4,
//         longestRunwayFeet: 11870,
//         terminalCount: 4,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.flysfo.com',
//         phone: '+1-650-821-8211',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-011',
//         iataCode: 'SEA',
//         icaoCode: 'KSEA',
//         airportName: 'Seattle-Tacoma International Airport',
//         city: 'Seattle',
//         countryCode: 'US',
//         stateProvince: 'Washington',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 47.4502, longitude: -122.3088 },
//         timeZone: 'America/Los_Angeles',
//         elevationFeet: 433,
//         runwayCount: 3,
//         longestRunwayFeet: 11901,
//         terminalCount: 1,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.portseattle.org/sea-tac',
//         phone: '+1-206-787-5388',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-012',
//         iataCode: 'MIA',
//         icaoCode: 'KMIA',
//         airportName: 'Miami International Airport',
//         city: 'Miami',
//         countryCode: 'US',
//         stateProvince: 'Florida',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 25.7959, longitude: -80.2870 },
//         timeZone: 'America/New_York',
//         elevationFeet: 8,
//         runwayCount: 4,
//         longestRunwayFeet: 13016,
//         terminalCount: 3,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.miami-airport.com',
//         phone: '+1-305-876-7000',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-013',
//         iataCode: 'BOS',
//         icaoCode: 'KBOS',
//         airportName: 'Logan International Airport',
//         city: 'Boston',
//         countryCode: 'US',
//         stateProvince: 'Massachusetts',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 42.3656, longitude: -71.0096 },
//         timeZone: 'America/New_York',
//         elevationFeet: 20,
//         runwayCount: 6,
//         longestRunwayFeet: 10083,
//         terminalCount: 4,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.massport.com/logan-airport',
//         phone: '+1-800-235-6426',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-014',
//         iataCode: 'YYZ',
//         icaoCode: 'CYYZ',
//         airportName: 'Toronto Pearson International Airport',
//         city: 'Toronto',
//         countryCode: 'CA',
//         stateProvince: 'Ontario',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 43.6777, longitude: -79.6248 },
//         timeZone: 'America/Toronto',
//         elevationFeet: 569,
//         runwayCount: 5,
//         longestRunwayFeet: 11120,
//         terminalCount: 2,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.torontopearson.com',
//         phone: '+1-416-247-7678',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-015',
//         iataCode: 'YVR',
//         icaoCode: 'CYVR',
//         airportName: 'Vancouver International Airport',
//         city: 'Vancouver',
//         countryCode: 'CA',
//         stateProvince: 'British Columbia',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 49.1947, longitude: -123.1838 },
//         timeZone: 'America/Vancouver',
//         elevationFeet: 14,
//         runwayCount: 3,
//         longestRunwayFeet: 11500,
//         terminalCount: 2,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.yvr.ca',
//         phone: '+1-604-207-7077',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-016',
//         iataCode: 'MEX',
//         icaoCode: 'MMMX',
//         airportName: 'Mexico City International Airport',
//         city: 'Mexico City',
//         countryCode: 'MX',
//         stateProvince: 'Mexico City',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 19.4363, longitude: -99.0721 },
//         timeZone: 'America/Mexico_City',
//         elevationFeet: 7316,
//         runwayCount: 2,
//         longestRunwayFeet: 12795,
//         terminalCount: 2,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.aicm.com.mx',
//         phone: '+52-55-2482-2400',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-017',
//         iataCode: 'CDG',
//         icaoCode: 'LFPG',
//         airportName: 'Charles de Gaulle Airport',
//         city: 'Paris',
//         countryCode: 'FR',
//         stateProvince: 'Île-de-France',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 49.0097, longitude: 2.5479 },
//         timeZone: 'Europe/Paris',
//         elevationFeet: 392,
//         runwayCount: 4,
//         longestRunwayFeet: 13829,
//         terminalCount: 3,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.parisaeroport.fr',
//         phone: '+33-1-70-36-39-50',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-018',
//         iataCode: 'FRA',
//         icaoCode: 'EDDF',
//         airportName: 'Frankfurt Airport',
//         city: 'Frankfurt',
//         countryCode: 'DE',
//         stateProvince: 'Hessen',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 50.0379, longitude: 8.5622 },
//         timeZone: 'Europe/Berlin',
//         elevationFeet: 364,
//         runwayCount: 4,
//         longestRunwayFeet: 13123,
//         terminalCount: 2,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.frankfurt-airport.com',
//         phone: '+49-180-6-372-4636',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-019',
//         iataCode: 'AMS',
//         icaoCode: 'EHAM',
//         airportName: 'Amsterdam Airport Schiphol',
//         city: 'Amsterdam',
//         countryCode: 'NL',
//         stateProvince: 'North Holland',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 52.3105, longitude: 4.7683 },
//         timeZone: 'Europe/Amsterdam',
//         elevationFeet: -11,
//         runwayCount: 6,
//         longestRunwayFeet: 12467,
//         terminalCount: 1,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.schiphol.nl',
//         phone: '+31-20-794-0800',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-020',
//         iataCode: 'NRT',
//         icaoCode: 'RJAA',
//         airportName: 'Narita International Airport',
//         city: 'Tokyo',
//         countryCode: 'JP',
//         stateProvince: 'Chiba',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: 35.7720, longitude: 140.3929 },
//         timeZone: 'Asia/Tokyo',
//         elevationFeet: 135,
//         runwayCount: 2,
//         longestRunwayFeet: 13123,
//         terminalCount: 3,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.narita-airport.jp',
//         phone: '+81-476-34-8000',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       },
//       {
//         id: 'airport-021',
//         iataCode: 'SYD',
//         icaoCode: 'YSSY',
//         airportName: 'Sydney Kingsford Smith Airport',
//         city: 'Sydney',
//         countryCode: 'AU',
//         stateProvince: 'New South Wales',
//         airportType: 'INTERNATIONAL',
//         hubSize: 'LARGE_HUB',
//         coordinates: { latitude: -33.9461, longitude: 151.1772 },
//         timeZone: 'Australia/Sydney',
//         elevationFeet: 21,
//         runwayCount: 3,
//         longestRunwayFeet: 12999,
//         terminalCount: 3,
//         hasCustoms: true,
//         status: 'ACTIVE',
//         codeSystem: 'IATA',
//         website: 'https://www.sydneyairport.com.au',
//         phone: '+61-2-9667-9111',
//         isActive: true,
//         validFrom: new Date().toISOString(),
//         recordedAt: new Date().toISOString(),
//         recordedBy: 'system',
//         version: 1
//       }
//     ];
//     this.totalElements = 21;
//     this.totalPages = Math.ceil(this.totalElements / this.pageSize);
//     this.loading = false;
//     
//     // Apply client-side filters
//     this.applyClientSideFilters();
//     
//     // Apply sorting
//     this.sortAirports();
//   }
}