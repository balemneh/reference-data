import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, PortDto, PagedResponse } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';

@Component({
  selector: 'app-ports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ports.html',
  styleUrl: './ports.scss'
})
export class PortsComponent implements OnInit {
  @ViewChild('searchInput') searchInput!: ElementRef;
  
  ports: PortDto[] = [];
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
  codeSystem = 'UNLOCODE';
  availableCodeSystems = ['UNLOCODE', 'IATA', 'ICAO', 'CBP-PORT', 'SMDG'];
  filterActive: boolean | null = null;
  filterPortType: string | null = null;
  filterCountry: string | null = null;
  availablePortTypes = ['SEAPORT', 'INLAND', 'DRY_PORT', 'AIRPORT', 'RAIL_TERMINAL', 'ROAD_TERMINAL'];
  
  // Modal and form
  showModal = false;
  showDeleteConfirm = false;
  selectedPort: PortDto | null = null;
  portToDelete: PortDto | null = null;
  isEditMode = false;
  formErrors: any = {};
  
  // Sorting
  sortField: keyof PortDto = 'portName';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Bulk operations
  selectedPorts = new Set<string>();
  selectAll = false;
  
  // View toggle
  viewMode: 'table' | 'card' | 'map' = 'table';
  
  // Map view
  showMapView = false;

  constructor(private apiService: ApiService, private toastService: ToastService) {}
  
  private initializeSearchSubscription() {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      this.searchTerm = searchTerm;
      this.currentPage = 0;
      this.loadPorts();
    });
  }

  ngOnInit() {
    this.initializeSearchSubscription();
    // Load mock data immediately for demonstration
    this.loadMockData();
  }

  loadPorts() {
    this.loading = true;
    this.error = null;
    this.selectedPorts.clear();
    this.selectAll = false;
    
    const params = {
      page: this.currentPage,
      size: this.pageSize,
      systemCode: this.codeSystem
    };
    
    const loadObservable = this.searchTerm
      ? this.apiService.searchPorts({ name: this.searchTerm, ...params })
      : this.apiService.getPorts(params);
    
    loadObservable.subscribe({
      next: (response: PagedResponse<PortDto>) => {
        this.ports = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.totalPages = response.totalPages || 0;
        this.loading = false;
        
        // Apply client-side filters
        this.applyClientFilters();
        
        // Apply sorting
        this.sortPorts();
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

  private applyClientFilters() {
    let filteredPorts = [...this.ports];
    
    if (this.filterActive !== null) {
      filteredPorts = filteredPorts.filter(p => p.isActive === this.filterActive);
    }
    
    if (this.filterPortType) {
      filteredPorts = filteredPorts.filter(p => p.portType === this.filterPortType);
    }
    
    if (this.filterCountry) {
      filteredPorts = filteredPorts.filter(p => 
        p.countryCode?.toLowerCase().includes(this.filterCountry!.toLowerCase()) ||
        p.countryName?.toLowerCase().includes(this.filterCountry!.toLowerCase())
      );
    }
    
    this.ports = filteredPorts;
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
    this.loadPorts();
  }
  
  onActiveFilterChange(value: string) {
    this.filterActive = value === 'all' ? null : value === 'active';
    this.applyClientFilters();
  }
  
  onPortTypeFilterChange(value: string) {
    this.filterPortType = value === 'all' ? null : value;
    this.applyClientFilters();
  }
  
  onCountryFilterChange(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.filterCountry = value || null;
    this.applyClientFilters();
  }
  
  sortBy(field: keyof PortDto) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.sortPorts();
  }
  
  private sortPorts() {
    this.ports.sort((a, b) => {
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
      this.loadPorts();
    }
  }

  viewPort(port: PortDto) {
    this.selectedPort = { ...port };
    this.isEditMode = false;
    this.formErrors = {};
    this.showModal = true;
  }
  
  editPort(port: PortDto) {
    this.selectedPort = { ...port };
    this.isEditMode = true;
    this.formErrors = {};
    this.showModal = true;
  }
  
  addPort() {
    // Open modal with empty port form
    this.selectedPort = {
      id: '',
      portCode: '',
      portName: '',
      countryCode: '',
      countryName: '',
      stateProvince: '',
      city: '',
      portType: 'SEAPORT',
      latitude: 0,
      longitude: 0,
      codeSystem: this.codeSystem,
      isActive: true,
      capabilities: [],
      customsOffice: '',
      timeZone: '',
      validFrom: new Date().toISOString(),
      recordedAt: new Date().toISOString(),
      recordedBy: 'system',
      version: 1
    };
    this.isEditMode = true;
    this.formErrors = {};
    this.showModal = true;
  }
  
  confirmDelete(port: PortDto) {
    this.portToDelete = port;
    this.showDeleteConfirm = true;
  }
  
  deletePort() {
    if (!this.portToDelete) return;
    
    // In a bitemporal system, we don't actually delete - we mark as inactive
    const updatedPort = {
      ...this.portToDelete,
      isActive: false,
      validTo: new Date().toISOString()
    };
    
    this.savePortData(updatedPort, 'Port deactivated successfully');
    this.showDeleteConfirm = false;
    this.portToDelete = null;
  }
  
  cancelDelete() {
    this.showDeleteConfirm = false;
    this.portToDelete = null;
  }

  savePort() {
    if (!this.selectedPort || !this.validateForm()) return;
    
    this.savePortData(this.selectedPort, 'Port saved successfully');
  }
  
  private savePortData(port: PortDto, successMsg: string) {
    // Create a change request for the port modification
    const changeRequest = {
      changeType: (port.id ? 'UPDATE' : 'CREATE') as 'UPDATE' | 'CREATE',
      entityType: 'PORT',
      entityId: port.id || undefined,
      description: `${port.id ? 'Update' : 'Create'} port: ${port.portName}`,
      requestedBy: 'current-user', // Would come from auth service
      newValues: port
    };
    
    this.apiService.createChangeRequest(changeRequest).subscribe({
      next: (response: any) => {
        this.successMessage = successMsg + ` (Request ID: ${response.id})`;
        this.toastService.showSuccess('Change request submitted', `Request ID: ${response.id}`);
        this.closeModal();
        this.loadPorts();
        
        // Clear success message after 5 seconds
        setTimeout(() => {
          this.successMessage = null;
        }, 5000);
      },
      error: (error: any) => {
        this.formErrors.general = error.error?.detail || 'Failed to save port. Please try again.';
        this.toastService.showError('Failed to save port', this.formErrors.general);
        console.error('Error saving port:', error);
      }
    });
  }
  
  private validateForm(): boolean {
    this.formErrors = {};
    
    if (!this.selectedPort) return false;
    
    if (!this.selectedPort.portName?.trim()) {
      this.formErrors.portName = 'Port name is required';
    }
    
    if (!this.selectedPort.portCode?.trim()) {
      this.formErrors.portCode = 'Port code is required';
    }
    
    if (!this.selectedPort.countryCode?.trim()) {
      this.formErrors.countryCode = 'Country code is required';
    }
    
    if (!this.selectedPort.city?.trim()) {
      this.formErrors.city = 'City is required';
    }
    
    if (this.selectedPort.latitude && (this.selectedPort.latitude < -90 || this.selectedPort.latitude > 90)) {
      this.formErrors.latitude = 'Latitude must be between -90 and 90';
    }
    
    if (this.selectedPort.longitude && (this.selectedPort.longitude < -180 || this.selectedPort.longitude > 180)) {
      this.formErrors.longitude = 'Longitude must be between -180 and 180';
    }
    
    return Object.keys(this.formErrors).length === 0;
  }

  closeModal() {
    this.showModal = false;
    this.selectedPort = null;
    this.isEditMode = false;
    this.formErrors = {};
  }
  
  // Bulk operations
  toggleSelectAll() {
    if (this.selectAll) {
      this.ports.forEach(p => this.selectedPorts.add(p.id));
    } else {
      this.selectedPorts.clear();
    }
  }
  
  toggleSelection(portId: string) {
    if (this.selectedPorts.has(portId)) {
      this.selectedPorts.delete(portId);
    } else {
      this.selectedPorts.add(portId);
    }
    
    this.selectAll = this.selectedPorts.size === this.ports.length;
  }
  
  isSelected(portId: string): boolean {
    return this.selectedPorts.has(portId);
  }
  
  exportSelected() {
    const selectedData = this.ports.filter(p => this.selectedPorts.has(p.id));
    const csv = this.convertToCSV(selectedData);
    this.downloadCSV(csv, `ports_export_${new Date().getTime()}.csv`);
  }
  
  exportAll() {
    // In production, this would call a backend endpoint for bulk export
    this.apiService.getAllCurrentPorts().subscribe({
      next: (ports) => {
        const csv = this.convertToCSV(ports);
        this.downloadCSV(csv, `all_ports_export_${new Date().getTime()}.csv`);
      },
      error: (error) => {
        this.error = 'Failed to export ports';
        console.error('Export error:', error);
      }
    });
  }
  
  private convertToCSV(data: PortDto[]): string {
    if (!data.length) return '';
    
    const headers = ['Port Code', 'Port Name', 'Country', 'City', 'State/Province', 'Type', 
                    'Latitude', 'Longitude', 'Capabilities', 'Active', 'Valid From', 'Valid To'];
    const rows = data.map(p => [
      p.portCode,
      p.portName,
      p.countryName || p.countryCode,
      p.city,
      p.stateProvince,
      p.portType,
      p.latitude?.toString() || '',
      p.longitude?.toString() || '',
      p.capabilities?.join('; ') || '',
      p.isActive ? 'Yes' : 'No',
      new Date(p.validFrom).toLocaleDateString(),
      p.validTo ? new Date(p.validTo).toLocaleDateString() : ''
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

  // Capability management
  addCapability() {
    if (!this.selectedPort?.capabilities) {
      this.selectedPort!.capabilities = [];
    }
  }

  removeCapability(index: number) {
    if (this.selectedPort?.capabilities) {
      this.selectedPort.capabilities.splice(index, 1);
    }
  }

  onCapabilityChange(index: number, event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    if (this.selectedPort?.capabilities && this.selectedPort.capabilities[index] !== undefined) {
      this.selectedPort.capabilities[index] = value;
    }
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
    return this.selectedPorts.size > 0;
  }
  
  resetFilters() {
    this.searchTerm = '';
    this.filterActive = null;
    this.filterPortType = null;
    this.filterCountry = null;
    this.codeSystem = 'UNLOCODE';
    this.currentPage = 0;
    if (this.searchInput) {
      this.searchInput.nativeElement.value = '';
    }
    this.loadPorts();
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
    if (mode === 'map') {
      this.showMapView = true;
    }
  }

  trackByPortId(index: number, port: PortDto): string {
    return port.id;
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
  
  getSortIcon(field: keyof PortDto): string {
    if (this.sortField !== field) return 'unfold_more';
    return this.sortDirection === 'asc' ? 'expand_less' : 'expand_more';
  }
  
  get hasActiveFilters(): boolean {
    return this.searchTerm.length > 0 || 
           this.filterActive !== null || 
           this.filterPortType !== null ||
           this.filterCountry !== null ||
           this.codeSystem !== 'UNLOCODE';
  }
  
  get selectedItems(): string[] {
    return Array.from(this.selectedPorts);
  }

  getPortTypeDisplayName(type: string): string {
    const typeMap: { [key: string]: string } = {
      'SEAPORT': 'Seaport',
      'INLAND': 'Inland Port',
      'DRY_PORT': 'Dry Port',
      'AIRPORT': 'Airport',
      'RAIL_TERMINAL': 'Rail Terminal',
      'ROAD_TERMINAL': 'Road Terminal'
    };
    return typeMap[type] || type;
  }

  formatCoordinates(lat: number | undefined, lon: number | undefined): string {
    if (!lat || !lon) return 'N/A';
    const latDir = lat >= 0 ? 'N' : 'S';
    const lonDir = lon >= 0 ? 'E' : 'W';
    return `${Math.abs(lat).toFixed(4)}°${latDir}, ${Math.abs(lon).toFixed(4)}°${lonDir}`;
  }

  getCapabilityIcon(capability: string): string {
    const icons: { [key: string]: string } = {
      'CONTAINER': 'inventory_2',
      'BULK_CARGO': 'local_shipping',
      'PASSENGER': 'groups',
      'RO_RO': 'directions_car',
      'CRUISE': 'directions_boat',
      'FISHING': 'phishing',
      'NAVAL': 'security',
      'PETROLEUM': 'local_gas_station',
      'LNG': 'propane_tank',
      'GRAIN': 'agriculture'
    };
    return icons[capability] || 'location_on';
  }

  private loadMockData() {
    // Mock data for UI demonstration when API is not available
    this.loading = false;
    this.ports = [
      {
        id: 'port-001',
        portCode: 'USNYC',
        portName: 'New York',
        countryCode: 'US',
        countryName: 'United States',
        stateProvince: 'New York',
        city: 'New York',
        portType: 'SEAPORT',
        latitude: 40.7128,
        longitude: -74.0060,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'PASSENGER', 'CRUISE'],
        customsOffice: 'Port of New York/Newark Area',
        timeZone: 'America/New_York',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-002',
        portCode: 'USLAX',
        portName: 'Los Angeles',
        countryCode: 'US',
        countryName: 'United States',
        stateProvince: 'California',
        city: 'Los Angeles',
        portType: 'SEAPORT',
        latitude: 33.7405,
        longitude: -118.2668,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'RO_RO', 'PETROLEUM'],
        customsOffice: 'Port of Los Angeles',
        timeZone: 'America/Los_Angeles',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-003',
        portCode: 'GBSOU',
        portName: 'Southampton',
        countryCode: 'GB',
        countryName: 'United Kingdom',
        stateProvince: 'Hampshire',
        city: 'Southampton',
        portType: 'SEAPORT',
        latitude: 50.9097,
        longitude: -1.4044,
        capabilities: ['CONTAINER', 'PASSENGER', 'CRUISE', 'RO_RO'],
        customsOffice: 'Southampton Port',
        timeZone: 'Europe/London',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-004',
        portCode: 'DEHAM',
        portName: 'Hamburg',
        countryCode: 'DE',
        countryName: 'Germany',
        stateProvince: 'Hamburg',
        city: 'Hamburg',
        portType: 'SEAPORT',
        latitude: 53.5511,
        longitude: 9.9937,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'RO_RO'],
        customsOffice: 'Hamburg Port Authority',
        timeZone: 'Europe/Berlin',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-005',
        portCode: 'CNSHG',
        portName: 'Shanghai',
        countryCode: 'CN',
        countryName: 'China',
        stateProvince: 'Shanghai',
        city: 'Shanghai',
        portType: 'SEAPORT',
        latitude: 31.2304,
        longitude: 121.4737,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'RO_RO', 'GRAIN'],
        customsOffice: 'Shanghai Customs',
        timeZone: 'Asia/Shanghai',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-006',
        portCode: 'BRRIO',
        portName: 'Rio de Janeiro',
        countryCode: 'BR',
        countryName: 'Brazil',
        stateProvince: 'Rio de Janeiro',
        city: 'Rio de Janeiro',
        portType: 'SEAPORT',
        latitude: -22.9068,
        longitude: -43.1729,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'PASSENGER', 'CRUISE'],
        customsOffice: 'Porto do Rio de Janeiro',
        timeZone: 'America/Sao_Paulo',
        codeSystem: 'UNLOCODE',
        isActive: false,
        validFrom: new Date().toISOString(),
        validTo: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-007',
        portCode: 'USLGB',
        portName: 'Long Beach',
        countryCode: 'US',
        countryName: 'United States',
        stateProvince: 'California',
        city: 'Long Beach',
        portType: 'SEAPORT',
        latitude: 33.7669,
        longitude: -118.1883,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'RO_RO'],
        customsOffice: 'Port of Long Beach',
        timeZone: 'America/Los_Angeles',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-008',
        portCode: 'USHOU',
        portName: 'Houston',
        countryCode: 'US',
        countryName: 'United States',
        stateProvince: 'Texas',
        city: 'Houston',
        portType: 'SEAPORT',
        latitude: 29.7604,
        longitude: -95.3698,
        capabilities: ['CONTAINER', 'PETROLEUM', 'BULK_CARGO', 'CHEMICALS'],
        customsOffice: 'Port of Houston',
        timeZone: 'America/Chicago',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-009',
        portCode: 'USBAL',
        portName: 'Baltimore',
        countryCode: 'US',
        countryName: 'United States',
        stateProvince: 'Maryland',
        city: 'Baltimore',
        portType: 'SEAPORT',
        latitude: 39.2904,
        longitude: -76.6122,
        capabilities: ['CONTAINER', 'RO_RO', 'BULK_CARGO', 'CRUISE'],
        customsOffice: 'Port of Baltimore',
        timeZone: 'America/New_York',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-010',
        portCode: 'USSEA',
        portName: 'Seattle',
        countryCode: 'US',
        countryName: 'United States',
        stateProvince: 'Washington',
        city: 'Seattle',
        portType: 'SEAPORT',
        latitude: 47.6062,
        longitude: -122.3321,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'CRUISE', 'PASSENGER'],
        customsOffice: 'Port of Seattle',
        timeZone: 'America/Los_Angeles',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-011',
        portCode: 'USMIA',
        portName: 'Miami',
        countryCode: 'US',
        countryName: 'United States',
        stateProvince: 'Florida',
        city: 'Miami',
        portType: 'SEAPORT',
        latitude: 25.7617,
        longitude: -80.1918,
        capabilities: ['CONTAINER', 'CRUISE', 'PASSENGER', 'RO_RO'],
        customsOffice: 'Port of Miami',
        timeZone: 'America/New_York',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-012',
        portCode: 'USBOS',
        portName: 'Boston',
        countryCode: 'US',
        countryName: 'United States',
        stateProvince: 'Massachusetts',
        city: 'Boston',
        portType: 'SEAPORT',
        latitude: 42.3601,
        longitude: -71.0589,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'CRUISE', 'LNG'],
        customsOffice: 'Port of Boston',
        timeZone: 'America/New_York',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-013',
        portCode: 'CAHAL',
        portName: 'Halifax',
        countryCode: 'CA',
        countryName: 'Canada',
        stateProvince: 'Nova Scotia',
        city: 'Halifax',
        portType: 'SEAPORT',
        latitude: 44.6488,
        longitude: -63.5752,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'CRUISE', 'RO_RO'],
        customsOffice: 'Port of Halifax',
        timeZone: 'America/Halifax',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-014',
        portCode: 'CAVAN',
        portName: 'Vancouver',
        countryCode: 'CA',
        countryName: 'Canada',
        stateProvince: 'British Columbia',
        city: 'Vancouver',
        portType: 'SEAPORT',
        latitude: 49.2827,
        longitude: -123.1207,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'CRUISE', 'PETROLEUM'],
        customsOffice: 'Port of Vancouver',
        timeZone: 'America/Vancouver',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-015',
        portCode: 'MXVER',
        portName: 'Veracruz',
        countryCode: 'MX',
        countryName: 'Mexico',
        stateProvince: 'Veracruz',
        city: 'Veracruz',
        portType: 'SEAPORT',
        latitude: 19.1738,
        longitude: -96.1342,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'PETROLEUM', 'RO_RO'],
        customsOffice: 'Puerto de Veracruz',
        timeZone: 'America/Mexico_City',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-016',
        portCode: 'MXMZT',
        portName: 'Manzanillo',
        countryCode: 'MX',
        countryName: 'Mexico',
        stateProvince: 'Colima',
        city: 'Manzanillo',
        portType: 'SEAPORT',
        latitude: 19.0540,
        longitude: -104.3188,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'CRUISE'],
        customsOffice: 'Puerto de Manzanillo',
        timeZone: 'America/Mexico_City',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-017',
        portCode: 'NLRTM',
        portName: 'Rotterdam',
        countryCode: 'NL',
        countryName: 'Netherlands',
        stateProvince: 'Zuid-Holland',
        city: 'Rotterdam',
        portType: 'SEAPORT',
        latitude: 51.9244,
        longitude: 4.4777,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'PETROLEUM', 'CHEMICALS', 'LNG'],
        customsOffice: 'Port of Rotterdam',
        timeZone: 'Europe/Amsterdam',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-018',
        portCode: 'DEHAM',
        portName: 'Hamburg',
        countryCode: 'DE',
        countryName: 'Germany',
        stateProvince: 'Hamburg',
        city: 'Hamburg',
        portType: 'SEAPORT',
        latitude: 53.5511,
        longitude: 9.9937,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'RO_RO', 'CRUISE'],
        customsOffice: 'Port of Hamburg',
        timeZone: 'Europe/Berlin',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-019',
        portCode: 'BEANR',
        portName: 'Antwerp',
        countryCode: 'BE',
        countryName: 'Belgium',
        stateProvince: 'Antwerp',
        city: 'Antwerp',
        portType: 'SEAPORT',
        latitude: 51.2194,
        longitude: 4.4025,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'CHEMICALS', 'PETROLEUM'],
        customsOffice: 'Port of Antwerp',
        timeZone: 'Europe/Brussels',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      },
      {
        id: 'port-020',
        portCode: 'FRLEH',
        portName: 'Le Havre',
        countryCode: 'FR',
        countryName: 'France',
        stateProvince: 'Normandy',
        city: 'Le Havre',
        portType: 'SEAPORT',
        latitude: 49.4944,
        longitude: 0.1079,
        capabilities: ['CONTAINER', 'BULK_CARGO', 'RO_RO', 'CRUISE'],
        customsOffice: 'Port of Le Havre',
        timeZone: 'Europe/Paris',
        codeSystem: 'UNLOCODE',
        isActive: true,
        validFrom: new Date().toISOString(),
        recordedAt: new Date().toISOString(),
        recordedBy: 'system',
        version: 1
      }
    ];
    this.totalElements = 20;
    this.totalPages = 1;
    this.loading = false;
    
    // Apply client-side filters
    this.applyClientFilters();
    
    // Apply sorting
    this.sortPorts();
  }
}