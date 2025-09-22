import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';

import { CountriesComponent } from './countries';
import { ApiService, CountryDto, PagedResponse, ChangeRequestDto } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';

describe('CountriesComponent', () => {
  let component: CountriesComponent;
  let fixture: ComponentFixture<CountriesComponent>;
  let apiService: jasmine.SpyObj<ApiService>;
  let toastService: jasmine.SpyObj<ToastService>;
  let router: Router;
  let httpTestingController: HttpTestingController;

  const mockCountries: CountryDto[] = [
    {
      id: '1',
      countryCode: 'US',
      countryName: 'United States',
      iso2Code: 'US',
      iso3Code: 'USA',
      numericCode: '840',
      codeSystem: 'ISO3166-1',
      isActive: true,
      validFrom: '2024-01-01',
      recordedAt: '2024-01-01T12:00:00Z',
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
      validFrom: '2024-01-01',
      recordedAt: '2024-01-01T12:00:00Z',
      recordedBy: 'system',
      version: 1
    },
    {
      id: '3',
      countryCode: 'GB',
      countryName: 'United Kingdom',
      iso2Code: 'GB',
      iso3Code: 'GBR',
      numericCode: '826',
      codeSystem: 'ISO3166-1',
      isActive: false,
      validFrom: '2024-01-01',
      validTo: '2024-06-01',
      recordedAt: '2024-01-01T12:00:00Z',
      recordedBy: 'system',
      version: 1
    }
  ];

  const mockPagedResponse: PagedResponse<CountryDto> = {
    content: mockCountries,
    totalElements: 3,
    totalPages: 1,
    number: 0,
    size: 20,
    first: true,
    last: true
  };

  beforeEach(async () => {
    const apiServiceSpy = jasmine.createSpyObj('ApiService', [
      'getCountries',
      'searchCountries',
      'createChangeRequest',
      'getAllCurrentCountries'
    ]);
    const toastServiceSpy = jasmine.createSpyObj('ToastService', [
      'showSuccess',
      'showError',
      'showWarning'
    ]);

    await TestBed.configureTestingModule({
      imports: [
        CountriesComponent,
        HttpClientTestingModule,
        RouterTestingModule,
        FormsModule,
        NoopAnimationsModule
      ],
      providers: [
        { provide: ApiService, useValue: apiServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CountriesComponent);
    component = fixture.componentInstance;
    apiService = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    router = TestBed.inject(Router);
    httpTestingController = TestBed.inject(HttpTestingController);

    // Setup default spy returns
    apiService.getCountries.and.returnValue(of(mockPagedResponse));
    apiService.searchCountries.and.returnValue(of(mockPagedResponse));
    apiService.getAllCurrentCountries.and.returnValue(of(mockCountries));
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with default values', () => {
      expect(component.countries).toEqual([]);
      expect(component.loading).toBeFalse();
      expect(component.error).toBeNull();
      expect(component.currentPage).toBe(0);
      expect(component.pageSize).toBe(20);
      expect(component.codeSystem).toBe('ISO3166-1');
      expect(component.viewMode).toBe('table');
    });

    it('should load countries on init', () => {
      fixture.detectChanges();
      expect(apiService.getCountries).toHaveBeenCalled();
      expect(component.countries).toEqual(mockCountries);
      expect(component.loading).toBeFalse();
    });

    it('should initialize search subscription', () => {
      spyOn(component['searchSubject'], 'pipe').and.returnValue(of('test'));
      component.ngOnInit();
      expect(component['searchSubject'].pipe).toHaveBeenCalled();
    });
  });

  describe('Data Loading', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should load countries successfully', () => {
      // Reset loading state
      component.loading = false;

      // Call loadCountries and immediately check loading state before observable completes
      component.loadCountries();

      // Check that the API was called correctly
      expect(apiService.getCountries).toHaveBeenCalledWith({
        page: 0,
        size: 20,
        systemCode: 'ISO3166-1'
      });

      // Check that countries were populated
      expect(component.countries).toEqual(mockCountries);
      expect(component.totalElements).toBe(3);
      expect(component.loading).toBeFalse(); // Should be false after completion
    });

    it('should handle API errors gracefully', () => {
      const errorResponse = { status: 500, message: 'Server Error' };
      apiService.getCountries.and.returnValue(throwError(() => errorResponse));

      component.loadCountries();

      expect(component.loading).toBeFalse();
      expect(component.error).toContain('Failed to connect to API');
      expect(toastService.showError).toHaveBeenCalledWith(
        'API Connection Failed',
        'Please check backend service.'
      );
    });

    it('should search countries when search term is provided', () => {
      component.searchTerm = 'United';
      component.loadCountries();

      expect(apiService.searchCountries).toHaveBeenCalledWith({
        name: 'United',
        page: 0,
        size: 20,
        systemCode: 'ISO3166-1'
      } as any);
    });

    it('should apply active filter when set', () => {
      component.filterActive = true;
      component.loadCountries();

      // Should filter client-side after API call
      expect(component.countries.every(c => c.isActive)).toBeTrue();
    });
  });

  describe('Search Functionality', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should handle search input with debouncing', fakeAsync(() => {
      const searchInput = fixture.nativeElement.querySelector('#search-countries');
      searchInput.value = 'test';
      searchInput.dispatchEvent(new Event('input'));

      tick(300); // Wait for debounce

      expect(component.searchTerm).toBe('test');
      expect(apiService.searchCountries).toHaveBeenCalled();
    }));

    it('should clear search', () => {
      component.searchTerm = 'test';
      component.clearSearch();

      expect(component.searchTerm).toBe('');
    });

    it('should reset current page when searching', fakeAsync(() => {
      component.currentPage = 2;
      component.onSearchInput({ target: { value: 'test' } } as any);

      tick(300);
      expect(component.currentPage).toBe(0);
    }));
  });

  describe('Pagination', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should go to specific page', () => {
      component.totalPages = 3;
      spyOn(component, 'loadCountries');

      component.goToPage(1);

      expect(component.currentPage).toBe(1);
      expect(component.loadCountries).toHaveBeenCalled();
    });

    it('should not go to invalid page', () => {
      component.totalPages = 3;
      component.currentPage = 0;
      spyOn(component, 'loadCountries');

      component.goToPage(-1);
      expect(component.currentPage).toBe(0);
      expect(component.loadCountries).not.toHaveBeenCalled();

      component.goToPage(5);
      expect(component.currentPage).toBe(0);
      expect(component.loadCountries).not.toHaveBeenCalled();
    });

    it('should change page size and reset to first page', () => {
      component.currentPage = 2;
      spyOn(component, 'loadCountries');

      component.onPageSizeChange(50);

      expect(component.pageSize).toBe(50);
      expect(component.currentPage).toBe(0);
      expect(component.loadCountries).toHaveBeenCalled();
    });

    it('should generate correct page numbers', () => {
      component.totalPages = 10;
      component.currentPage = 5;

      const pageNumbers = component.pageNumbers;

      expect(pageNumbers).toEqual([3, 4, 5, 6, 7]);
    });
  });

  describe('Sorting', () => {
    beforeEach(() => {
      fixture.detectChanges();
      component.countries = [...mockCountries];
    });

    it('should sort by field ascending by default', () => {
      // Use a different field since countryName is already the default sort field
      component.sortBy('countryCode');

      expect(component.sortField).toBe('countryCode');
      expect(component.sortDirection).toBe('asc');
      expect(component.countries[0].countryCode).toBe('CA');
    });

    it('should toggle sort direction when clicking same field', () => {
      component.sortField = 'countryName';
      component.sortDirection = 'asc';

      component.sortBy('countryName');

      expect(component.sortDirection).toBe('desc');
      expect(component.countries[0].countryName).toBe('United States');
    });

    it('should get correct sort icon', () => {
      component.sortField = 'countryName';
      component.sortDirection = 'asc';

      expect(component.getSortIcon('countryName')).toBe('expand_less');
      expect(component.getSortIcon('countryCode')).toBe('unfold_more');

      component.sortDirection = 'desc';
      expect(component.getSortIcon('countryName')).toBe('expand_more');
    });
  });

  describe('Modal Operations', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should open view modal for country', () => {
      const country = mockCountries[0];
      component.viewCountry(country);

      expect(component.selectedCountry).toEqual(country);
      expect(component.isEditMode).toBeFalse();
      expect(component.showModal).toBeTrue();
    });

    it('should open edit modal for country', () => {
      const country = mockCountries[0];
      component.editCountry(country);

      expect(component.selectedCountry).toEqual(country);
      expect(component.isEditMode).toBeTrue();
      expect(component.showModal).toBeTrue();
    });

    it('should open add country modal', () => {
      component.addCountry();

      expect(component.selectedCountry).toBeDefined();
      expect(component.selectedCountry?.countryCode).toBe('');
      expect(component.isEditMode).toBeTrue();
      expect(component.showModal).toBeTrue();
    });

    it('should close modal and reset state', () => {
      component.selectedCountry = mockCountries[0];
      component.showModal = true;
      component.isEditMode = true;

      component.closeModal();

      expect(component.selectedCountry).toBeNull();
      expect(component.showModal).toBeFalse();
      expect(component.isEditMode).toBeFalse();
      expect(component.formErrors).toEqual({});
    });
  });

  describe('Change Request Workflow', () => {
    beforeEach(() => {
      fixture.detectChanges();
      component.selectedCountry = { ...mockCountries[0] };
    });

    it('should validate form correctly', () => {
      // Valid form
      expect(component['validateForm']()).toBeTrue();
      expect(Object.keys(component.formErrors).length).toBe(0);

      // Invalid form - empty country name
      component.selectedCountry!.countryName = '';
      expect(component['validateForm']()).toBeFalse();
      expect(component.formErrors.countryName).toBeDefined();
    });

    it('should validate ISO codes correctly', () => {
      component.selectedCountry!.iso2Code = 'USA'; // Too long
      component.selectedCountry!.iso3Code = 'US'; // Too short

      expect(component['validateForm']()).toBeFalse();
      expect(component.formErrors.iso2Code).toBeDefined();
      expect(component.formErrors.iso3Code).toBeDefined();
    });

    it('should validate numeric code correctly', () => {
      component.selectedCountry!.numericCode = '12'; // Too short
      expect(component['validateForm']()).toBeFalse();
      expect(component.formErrors.numericCode).toBeDefined();

      component.selectedCountry!.numericCode = 'abc'; // Not numeric
      expect(component['validateForm']()).toBeFalse();
      expect(component.formErrors.numericCode).toBeDefined();
    });

    it('should open justification modal on save', () => {
      spyOn(component, 'validateForm' as any).and.returnValue(true);

      component.saveCountry();

      expect(component.pendingAction).toBe('update');
      expect(component.showJustificationModal).toBeTrue();
    });

    it('should submit change request with valid justification', () => {
      const mockChangeRequest: ChangeRequestDto = {
        id: 'cr-123',
        changeType: 'UPDATE',
        entityType: 'COUNTRY',
        description: 'Test update',
        requestedBy: 'current-user',
        status: 'PENDING',
        createdAt: '2024-01-01T12:00:00Z',
        priority: 'MEDIUM',
        requestedAt: '2024-01-01T12:00:00Z',
        updatedAt: '2024-01-01T12:00:00Z'
      };

      apiService.createChangeRequest.and.returnValue(of(mockChangeRequest));
      spyOn(component, 'closeJustificationModal');
      spyOn(component, 'closeModal');

      component.businessJustification = 'Valid business justification';
      component.pendingAction = 'update';
      component.submitChangeRequest();

      expect(apiService.createChangeRequest).toHaveBeenCalled();
      expect(toastService.showSuccess).toHaveBeenCalled();
      expect(component.closeJustificationModal).toHaveBeenCalled();
    });

    it('should handle change request creation errors', () => {
      const errorResponse = { error: { detail: 'Validation failed' } };
      apiService.createChangeRequest.and.returnValue(throwError(() => errorResponse));

      component.businessJustification = 'Valid justification';
      component.pendingAction = 'create';
      component.submitChangeRequest();

      expect(component.formErrors.general).toBe('Validation failed');
      expect(toastService.showError).toHaveBeenCalledWith(
        'Change Request Failed',
        'Validation failed'
      );
    });

    it('should validate business justification', () => {
      component.businessJustification = '';
      expect(component.validateJustification()).toBeFalse();
      expect(component.formErrors.justification).toBeDefined();

      component.businessJustification = 'short';
      expect(component.validateJustification()).toBeFalse();
      expect(component.formErrors.justification).toBeDefined();

      component.businessJustification = 'This is a valid business justification';
      expect(component.validateJustification()).toBeTrue();
      expect(component.formErrors.justification).toBeUndefined();
    });
  });

  describe('Delete Workflow', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should open delete confirmation modal', () => {
      const country = mockCountries[0];
      component.confirmDelete(country);

      expect(component.countryToDelete).toEqual(country);
      expect(component.showDeleteConfirm).toBeTrue();
    });

    it('should proceed to justification modal on delete confirmation', () => {
      component.countryToDelete = mockCountries[0];
      component.deleteCountry();

      expect(component.pendingAction).toBe('delete');
      expect(component.showDeleteConfirm).toBeFalse();
      expect(component.showJustificationModal).toBeTrue();
    });

    it('should cancel delete operation', () => {
      component.countryToDelete = mockCountries[0];
      component.showDeleteConfirm = true;

      component.cancelDelete();

      expect(component.countryToDelete).toBeNull();
      expect(component.showDeleteConfirm).toBeFalse();
    });
  });

  describe('Filtering and Code System', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should change code system and reload', () => {
      spyOn(component, 'loadCountries');
      component.codeSystem = 'GENC';

      component.onCodeSystemChange();

      expect(component.currentPage).toBe(0);
      expect(component.loadCountries).toHaveBeenCalled();
    });

    it('should filter by active status', () => {
      spyOn(component, 'loadCountries');

      component.onActiveFilterChange('active');
      expect(component.filterActive).toBeTrue();

      component.onActiveFilterChange('inactive');
      expect(component.filterActive).toBeFalse();

      component.onActiveFilterChange('all');
      expect(component.filterActive).toBeNull();

      expect(component.loadCountries).toHaveBeenCalledTimes(3);
    });

    it('should reset filters', () => {
      component.searchTerm = 'test';
      component.filterActive = true;
      component.codeSystem = 'GENC';
      component.currentPage = 2;
      spyOn(component, 'loadCountries');

      component.resetFilters();

      expect(component.searchTerm).toBe('');
      expect(component.filterActive).toBeNull();
      expect(component.codeSystem).toBe('ISO3166-1');
      expect(component.currentPage).toBe(0);
      expect(component.loadCountries).toHaveBeenCalled();
    });

    it('should check if has active filters', () => {
      expect(component.hasActiveFilters).toBeFalse();

      component.searchTerm = 'test';
      expect(component.hasActiveFilters).toBeTrue();

      component.searchTerm = '';
      component.filterActive = true;
      expect(component.hasActiveFilters).toBeTrue();

      component.filterActive = null;
      component.codeSystem = 'GENC';
      expect(component.hasActiveFilters).toBeTrue();
    });
  });

  describe('Export Functionality', () => {
    beforeEach(() => {
      fixture.detectChanges();
      component.countries = mockCountries;
    });

    it('should export selected countries', () => {
      component.selectedCountries.add('1');
      component.selectedCountries.add('2');
      spyOn(component, 'downloadCSV' as any);

      component.exportSelected();

      expect(component['downloadCSV']).toHaveBeenCalled();
    });

    it('should export all countries', () => {
      spyOn(component, 'downloadCSV' as any);

      component.exportAll();

      expect(apiService.getAllCurrentCountries).toHaveBeenCalled();
    });

    it('should convert countries to CSV format', () => {
      // Use explicit test data to avoid any sorting issues
      const testCountries = [
        {
          id: '1',
          countryCode: 'US',
          countryName: 'United States',
          iso2Code: 'US',
          iso3Code: 'USA',
          numericCode: '840',
          codeSystem: 'ISO3166-1',
          isActive: true,
          validFrom: '2024-01-01',
          recordedAt: '2024-01-01T12:00:00Z',
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
          validFrom: '2024-01-01',
          recordedAt: '2024-01-01T12:00:00Z',
          recordedBy: 'system',
          version: 1
        }
      ];

      const csv = component['convertToCSV'](testCountries);

      expect(csv).toContain('Country Code,Country Name');
      expect(csv).toContain('"US","United States"');
      expect(csv).toContain('"CA","Canada"');
      expect(csv).not.toContain('"GB","United Kingdom"');
    });
  });

  describe('Bulk Selection', () => {
    beforeEach(() => {
      fixture.detectChanges();
      component.countries = mockCountries;
    });

    it('should toggle select all', () => {
      component.selectAll = true;
      component.toggleSelectAll();

      expect(component.selectedCountries.size).toBe(3);
      expect(component.selectedCountries.has('1')).toBeTrue();

      component.selectAll = false;
      component.toggleSelectAll();

      expect(component.selectedCountries.size).toBe(0);
    });

    it('should toggle individual selection', () => {
      component.toggleSelection('1');
      expect(component.selectedCountries.has('1')).toBeTrue();

      component.toggleSelection('1');
      expect(component.selectedCountries.has('1')).toBeFalse();
    });

    it('should update select all state when all items selected', () => {
      component.toggleSelection('1');
      component.toggleSelection('2');
      component.toggleSelection('3');

      expect(component.selectAll).toBeTrue();
    });

    it('should check if item is selected', () => {
      component.selectedCountries.add('1');

      expect(component.isSelected('1')).toBeTrue();
      expect(component.isSelected('2')).toBeFalse();
    });
  });

  describe('View Mode', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should toggle between table and card view', () => {
      expect(component.isTableView()).toBeTrue();
      expect(component.isCardView()).toBeFalse();

      component.setViewMode('card');

      expect(component.isTableView()).toBeFalse();
      expect(component.isCardView()).toBeTrue();
    });
  });

  describe('Utility Functions', () => {
    it('should format dates correctly', () => {
      const dateString = '2024-01-15T12:30:00Z';
      const formatted = component.formatDate(dateString);
      expect(formatted).toMatch(/Jan \d{1,2}, 2024/);

      expect(component.formatDate(undefined)).toBe('N/A');
      expect(component.formatDate('')).toBe('N/A');
    });

    it('should track countries by ID', () => {
      const country = mockCountries[0];
      const trackResult = component.trackByCountryId(0, country);
      expect(trackResult).toBe(country.id);
    });

    it('should get minimum value', () => {
      expect(component.getMinValue(5, 10)).toBe(5);
      expect(component.getMinValue(15, 10)).toBe(10);
    });
  });

  describe('Justification Modal', () => {
    beforeEach(() => {
      fixture.detectChanges();
      component.selectedCountry = mockCountries[0];
    });

    it('should get correct modal title based on action', () => {
      component.pendingAction = 'create';
      expect(component.getJustificationModalTitle()).toBe('Justify Country Creation');

      component.pendingAction = 'update';
      expect(component.getJustificationModalTitle()).toBe('Justify Country Update');

      component.pendingAction = 'delete';
      expect(component.getJustificationModalTitle()).toBe('Justify Country Deactivation');
    });

    it('should get correct modal description based on action', () => {
      component.pendingAction = 'create';
      const description = component.getJustificationModalDescription();
      expect(description).toContain('creating the new country');

      component.pendingAction = 'update';
      const updateDescription = component.getJustificationModalDescription();
      expect(updateDescription).toContain('updating');

      component.pendingAction = 'delete';
      const deleteDescription = component.getJustificationModalDescription();
      expect(deleteDescription).toContain('deactivating');
    });

    it('should close justification modal and reset state', () => {
      component.showJustificationModal = true;
      component.businessJustification = 'test';
      component.pendingAction = 'create';
      component.formErrors.justification = 'error';

      component.closeJustificationModal();

      expect(component.showJustificationModal).toBeFalse();
      expect(component.businessJustification).toBe('');
      expect(component.pendingAction).toBeNull();
      expect(component.formErrors.justification).toBeUndefined();
    });
  });

  describe('Error Handling', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should display error messages in UI', () => {
      component.error = 'Test error message';
      fixture.detectChanges();

      const errorElement = fixture.nativeElement.querySelector('.cbp-alert--error');
      expect(errorElement).toBeTruthy();
      expect(errorElement.textContent).toContain('Test error message');
    });

    it('should display success messages in UI', () => {
      component.successMessage = 'Test success message';
      fixture.detectChanges();

      const successElement = fixture.nativeElement.querySelector('.cbp-alert--success');
      expect(successElement).toBeTruthy();
      expect(successElement.textContent).toContain('Test success message');
    });
  });
});