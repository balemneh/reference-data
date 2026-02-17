import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

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
    // No httpTestingController.verify() needed for isolated tests,
    // but good practice if you have pass-through requests.
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
      component.loading = true; // Set to true before calling
      component.loadCountries();
      expect(apiService.getCountries).toHaveBeenCalledWith({
        page: 0,
        size: 20,
        systemCode: 'ISO3166-1'
      });
      expect(component.countries).toEqual(mockCountries);
      expect(component.totalElements).toBe(3);
      expect(component.loading).toBeFalse();
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
        query: 'United',
        page: 0,
        size: 20,
        systemCode: 'ISO3166-1'
      });
    });

    it('should apply active filter when set', () => {
      component.filterActive = true;
      component.loadCountries();
      expect(component.countries.every(c => c.isActive)).toBeTrue();
    });
  });

  describe('Search Functionality', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should handle search input with debouncing', fakeAsync(() => {
      component.searchSubject.next('test');
      tick(300);
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
      component.searchSubject.next('test');
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
      expect(component['validateForm']()).toBeTrue();
      expect(Object.keys(component.formErrors).length).toBe(0);
      component.selectedCountry!.countryName = '';
      expect(component['validateForm']()).toBeFalse();
      expect(component.formErrors.countryName).toBeDefined();
    });

    it('should open justification modal on save', () => {
      spyOn(component as any, 'validateForm').and.returnValue(true);
      component.saveCountry();
      expect(component.pendingAction).toBe('update');
      expect(component.showJustificationModal).toBeTrue();
    });

    it('should submit change request with valid justification', () => {
      const mockChangeRequest: ChangeRequestDto = { id: 'cr-123' } as any;
      apiService.createChangeRequest.and.returnValue(of(mockChangeRequest));
      spyOn(component, 'closeJustificationModal');
      spyOn(component, 'closeModal');

      const justification = 'Valid business justification';
      component.businessJustification = justification;
      component.pendingAction = 'update';
      component.submitChangeRequest();

      const expectedPayload = {
        changeType: 'UPDATE' as 'UPDATE',
        entityType: 'COUNTRY' as 'COUNTRY',
        entityId: component.selectedCountry?.id,
        description: `update country: ${component.selectedCountry?.countryName}`,
        requestedBy: 'current-user',
        businessJustification: justification,
        proposedChanges: JSON.stringify(component.selectedCountry),
        currentValues: JSON.stringify(component.selectedCountry)
      };

      expect(apiService.createChangeRequest).toHaveBeenCalled();
      expect(toastService.showSuccess).toHaveBeenCalled();
      expect(component.closeJustificationModal).toHaveBeenCalled();
    });

    it('should handle change request creation errors', () => {
      const errorResponse = { error: { detail: 'Validation failed' } };
      apiService.createChangeRequest.and.returnValue(throwError(() => errorResponse));
      const justification = 'Valid justification';
      component.businessJustification = justification;
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

    it('should stringify proposedChanges when submitting a change request', () => {
      const mockChangeRequest: ChangeRequestDto = {
        id: 'cr-123',
        changeType: 'CREATE',
        entityType: 'COUNTRY',
        description: 'Test create',
        requestedBy: 'current-user',
        status: 'PENDING',
        createdAt: '2024-01-01T12:00:00Z',
        priority: 'MEDIUM',
        requestedAt: '2024-01-01T12:00:00Z',
        updatedAt: '2024-01-01T12:00:00Z'
      };
      apiService.createChangeRequest.and.returnValue(of(mockChangeRequest));

      const newCountry: CountryDto = {
        id: '',
        countryCode: 'XX',
        countryName: 'New Country',
        iso2Code: 'XX',
        iso3Code: 'XXX',
        numericCode: '123',
        codeSystem: 'ISO3166-1',
        isActive: true,
        validFrom: '2024-01-01',
        recordedAt: '2024-01-01T12:00:00Z',
        recordedBy: 'system',
        version: 1
      };
      component.selectedCountry = newCountry;
      component.businessJustification = 'This is a test justification';
      component.pendingAction = 'create';

      component.submitChangeRequest();

      expect(apiService.createChangeRequest).toHaveBeenCalled();
      
      const payload = apiService.createChangeRequest.calls.mostRecent().args[0];
      expect(typeof payload.proposedChanges).toBe('string');
      
      const parsedProposedChanges = JSON.parse(payload.proposedChanges);
      expect(parsedProposedChanges).toEqual(newCountry);
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

    it('should close justification modal and reset state', () => {
      component.showJustificationModal = true;
      component.pendingAction = 'create';
      component.formErrors.justification = 'error';
      component.closeJustificationModal();
      expect(component.showJustificationModal).toBeFalse();
      expect(component.pendingAction).toBeNull();
      expect(component.formErrors.justification).toBeUndefined();
    });
  });
});
